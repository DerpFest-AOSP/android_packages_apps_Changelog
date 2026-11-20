/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import org.derpfest.changelog.models.ChangelogError
import java.io.IOException
import java.net.HttpURLConnection

private const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * Read the response body, turning failures into [ChangelogError]s.
 *
 * @throws ChangelogError If the request failed
 */
fun HttpURLConnection.readBody(): String {
    val code = try {
        responseCode
    } catch (e: IOException) {
        throw ChangelogError.Network
    }

    if (code !in 200..299) {
        throw rateLimitResetAt(code)?.let {
            ChangelogError.RateLimited(it)
        } ?: ChangelogError.Http(code)
    }

    return try {
        inputStream.bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        throw ChangelogError.Network
    }
}

/**
 * When the rate limit of the server resets, in milliseconds since the epoch, or null if the
 * request didn't fail because of a rate limit.
 */
private fun HttpURLConnection.rateLimitResetAt(code: Int): Long? {
    if (code != HttpURLConnection.HTTP_FORBIDDEN && code != HTTP_TOO_MANY_REQUESTS) {
        return null
    }

    val now = System.currentTimeMillis()

    getHeaderField("Retry-After")?.toLongOrNull()?.let {
        return now + it * 1000
    }

    if (getHeaderField("X-RateLimit-Remaining") != "0") {
        return null
    }

    return getHeaderField("X-RateLimit-Reset")?.toLongOrNull()?.let {
        it * 1000
    } ?: (now + 60_000)
}
