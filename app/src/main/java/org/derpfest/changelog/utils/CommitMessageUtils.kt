/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.utils

object CommitMessageUtils {
    /**
     * Trailers that only matter for the review process.
     */
    private val trailerRegex = Regex("^(Change-Id|Signed-off-by):.*$", RegexOption.IGNORE_CASE)

    private val listItemRegex = Regex("""^([-*+•]|\d+[.)])\s""")

    /**
     * Lines like "Bug: 123456" or "Test: atest".
     */
    private val tagRegex = Regex("""^[\w-]+: \S""")

    private val urlRegex = Regex("""https?://\S+""")

    /**
     * Characters that end a sentence rather than a link.
     */
    private const val URL_TRAILING_CHARACTERS = ".,;:!?)]}>'\""

    /**
     * Get the body of a commit message, without its subject and the Change-Id and Signed-off-by
     * trailers.
     *
     * @param message The commit message
     */
    fun getBody(message: String) = message
        .lines()
        .drop(1)
        .filterNot { trailerRegex.matches(it.trim()) }
        .joinToString("\n")
        .trim()

    /**
     * Join the lines a commit message got wrapped into, so it can wrap to the screen width.
     * Blank lines, list items, indented lines and tags keep their own line.
     *
     * @param text The text
     */
    fun unwrap(text: String): String {
        val lines = text.lines()

        return buildString {
            lines.forEachIndexed { index, line ->
                val previousLine = lines.getOrNull(index - 1)

                when {
                    previousLine == null -> append(line)
                    previousLine.continuesWith(line) -> append(' ').append(line)
                    else -> append('\n').append(line)
                }
            }
        }
    }

    /**
     * Find the links in a text.
     *
     * @param text The text
     * @return The ranges of the links
     */
    fun findUrls(text: String) = urlRegex.findAll(text).map {
        val url = it.value.trimEnd { char -> char in URL_TRAILING_CHARACTERS }
        it.range.first until it.range.first + url.length
    }

    private fun String.continuesWith(line: String) = isNotBlank() &&
            !tagRegex.containsMatchIn(this) &&
            line.isNotBlank() &&
            !line.first().isWhitespace() &&
            !listItemRegex.containsMatchIn(line) &&
            !tagRegex.containsMatchIn(line)
}
