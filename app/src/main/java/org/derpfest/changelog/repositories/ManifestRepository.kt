/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.repositories

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.derpfest.changelog.R
import org.derpfest.changelog.datasources.ManifestDataSource
import org.derpfest.changelog.ext.readJson
import org.derpfest.changelog.ext.writeJson
import org.derpfest.changelog.models.ChangelogError
import org.derpfest.changelog.models.Manifest
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Provides the manifest of the ROM, which decides which repositories the changelog covers.
 */
class ManifestRepository(context: Context) {
    private val manifestDataSource = ManifestDataSource(
        manifestUrl = context.getString(R.string.manifest_url),
        manifestRawUrl = context.getString(R.string.manifest_raw_url),
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val cacheFile = AtomicFile(File(context.cacheDir, CACHE_FILE_NAME))

    private val mutex = Mutex()

    /**
     * Get the manifest. The cached copy is used if it's recent enough, or as a fallback when the
     * manifest can't be fetched.
     *
     * @throws ChangelogError If the manifest couldn't be fetched and nothing is cached
     */
    suspend fun getManifest(): Manifest = mutex.withLock {
        withContext(Dispatchers.IO) {
            val cachedManifest = cacheFile.readJson<Manifest>(json)
            val cacheAge = System.currentTimeMillis() - cacheFile.baseFile.lastModified()

            if (cachedManifest != null && cacheAge in 0 until MAX_CACHE_AGE) {
                return@withContext cachedManifest
            }

            try {
                manifestDataSource.getManifest().also {
                    cacheFile.writeJson(json, it)
                }
            } catch (e: ChangelogError) {
                cachedManifest ?: throw e
            }
        }
    }

    companion object {
        private const val CACHE_FILE_NAME = "manifest.json"

        private val MAX_CACHE_AGE = TimeUnit.DAYS.toMillis(1)
    }
}
