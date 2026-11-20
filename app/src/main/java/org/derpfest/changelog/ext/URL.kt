/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import org.derpfest.changelog.models.ChangelogError
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TIMEOUT_MS = 15_000

/**
 * Do a blocking GET request and return the response body.
 *
 * @param headers Additional request headers
 * @throws ChangelogError If the request failed
 */
fun URL.fetch(vararg headers: Pair<String, String>): String {
    val connection = try {
        openConnection() as HttpURLConnection
    } catch (e: IOException) {
        throw ChangelogError.Network
    }

    return try {
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        headers.forEach { (name, value) ->
            connection.setRequestProperty(name, value)
        }

        connection.readBody()
    } finally {
        connection.disconnect()
    }
}
