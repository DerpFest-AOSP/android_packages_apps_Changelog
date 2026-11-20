/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import android.util.AtomicFile
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Decode the JSON content of this file.
 *
 * @return The decoded value, or null if the file doesn't exist or can't be decoded
 */
inline fun <reified T> AtomicFile.readJson(json: Json): T? = try {
    json.decodeFromString<T>(readFully().decodeToString())
} catch (e: IOException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

/**
 * Replace the content of this file with [value] encoded as JSON. Failures are ignored since the
 * file is only used as a cache.
 */
inline fun <reified T> AtomicFile.writeJson(json: Json, value: T) {
    val outputStream = try {
        startWrite()
    } catch (e: IOException) {
        return
    }

    try {
        outputStream.write(json.encodeToString(value).encodeToByteArray())
        finishWrite(outputStream)
    } catch (e: IOException) {
        failWrite(outputStream)
    }
}
