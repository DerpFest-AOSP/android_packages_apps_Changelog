/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.datasources

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.derpfest.changelog.ext.fetch
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.Manifest
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.StringReader
import java.net.URI

/**
 * Fetches a repo manifest and resolves its projects, following its `<include>`s.
 *
 * @param manifestUrl The URL of the manifest repository, relative remote URLs are resolved
 *   against it like repo does
 * @param manifestRawUrl The URL of the raw main manifest file
 */
class ManifestDataSource(
    private val manifestUrl: String,
    private val manifestRawUrl: String,
) {
    /**
     * Fetch and resolve the manifest.
     *
     * @throws ChangelogError If the manifest couldn't be fetched or parsed
     */
    suspend fun getManifest() = withContext(Dispatchers.IO) {
        val manifestUri = URI(manifestRawUrl)

        val elements = buildList {
            parseManifest(manifestUri, manifestUri.resolve("."), mutableSetOf())
        }

        resolve(elements)
    }

    private fun MutableList<Element>.parseManifest(
        uri: URI,
        rootUri: URI,
        visitedUris: MutableSet<URI>,
    ) {
        if (!visitedUris.add(uri)) {
            return
        }

        val parser = Xml.newPullParser().apply {
            setInput(StringReader(uri.toURL().fetch()))
        }

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 2) {
                    continue
                }

                when (parser.name) {
                    "remote" -> add(
                        Element.Remote(
                            name = parser.requireAttribute("name"),
                            fetch = parser.requireAttribute("fetch"),
                            review = parser.attribute("review"),
                            revision = parser.attribute("revision"),
                        )
                    )

                    "default" -> add(
                        Element.Default(
                            remote = parser.attribute("remote"),
                            revision = parser.attribute("revision"),
                        )
                    )

                    "project" -> {
                        val name = parser.requireAttribute("name")

                        add(
                            Element.Project(
                                name = name,
                                path = parser.attribute("path") ?: name,
                                remote = parser.attribute("remote"),
                                revision = parser.attribute("revision"),
                            )
                        )
                    }

                    "extend-project" -> add(
                        Element.ExtendProject(
                            name = parser.requireAttribute("name"),
                            path = parser.attribute("path"),
                            remote = parser.attribute("remote"),
                            revision = parser.attribute("revision"),
                        )
                    )

                    "remove-project" -> add(
                        Element.RemoveProject(
                            name = parser.attribute("name"),
                            path = parser.attribute("path"),
                        )
                    )

                    // Includes are relative to the root of the manifest repository
                    "include" -> parseManifest(
                        rootUri.resolve(parser.requireAttribute("name")),
                        rootUri,
                        visitedUris,
                    )
                }
            }
        } catch (e: XmlPullParserException) {
            throw ChangelogError.InvalidResponse
        } catch (e: IOException) {
            throw ChangelogError.InvalidResponse
        }
    }

    private fun resolve(elements: List<Element>): Manifest {
        val remotes = elements.filterIsInstance<Element.Remote>().associateBy { it.name }
        val default = elements.filterIsInstance<Element.Default>().lastOrNull()

        val projects = mutableListOf<Element.Project>()
        elements.forEach { element ->
            when (element) {
                is Element.Project -> projects.add(element)

                is Element.ExtendProject -> projects.replaceAll {
                    when (it.matches(element.name, element.path)) {
                        true -> it.copy(
                            remote = element.remote ?: it.remote,
                            revision = element.revision ?: it.revision,
                        )

                        false -> it
                    }
                }

                is Element.RemoveProject -> projects.removeAll {
                    it.matches(element.name, element.path)
                }

                else -> {}
            }
        }

        return Manifest(
            projects = projects.mapNotNull { project ->
                val remote = remotes[project.remote ?: default?.remote] ?: return@mapNotNull null
                val revision = project.revision
                    ?: remote.revision
                    ?: default?.revision
                    ?: return@mapNotNull null
                val fetchUrl = URI(manifestUrl.trimEnd('/')).resolve(remote.fetch).toString()

                Manifest.Project(
                    path = project.path,
                    name = project.name,
                    url = "${fetchUrl.trimEnd('/')}/${project.name}",
                    review = remote.review,
                    revision = revision,
                )
            }
        )
    }

    private sealed interface Element {
        data class Remote(
            val name: String,
            val fetch: String,
            val review: String?,
            val revision: String?,
        ) : Element

        data class Default(
            val remote: String?,
            val revision: String?,
        ) : Element

        data class Project(
            val name: String,
            val path: String,
            val remote: String?,
            val revision: String?,
        ) : Element {
            fun matches(name: String?, path: String?) = when {
                name == null && path == null -> false
                else -> (name == null || this.name == name) && (path == null || this.path == path)
            }
        }

        data class ExtendProject(
            val name: String,
            val path: String?,
            val remote: String?,
            val revision: String?,
        ) : Element

        data class RemoveProject(
            val name: String?,
            val path: String?,
        ) : Element
    }

    companion object {
        private fun XmlPullParser.attribute(name: String): String? = getAttributeValue(null, name)

        private fun XmlPullParser.requireAttribute(name: String) =
            attribute(name) ?: throw ChangelogError.InvalidResponse
    }
}
