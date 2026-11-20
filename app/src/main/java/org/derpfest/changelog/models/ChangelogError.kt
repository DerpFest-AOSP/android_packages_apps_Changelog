/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

/**
 * Errors that can happen while loading the changelog.
 */
sealed class ChangelogError : Exception() {
    /**
     * The server couldn't be reached.
     */
    data object Network : ChangelogError()

    /**
     * The server limits the amount of requests and the limit got reached.
     *
     * @param resetAt When requests are allowed again, in milliseconds since the epoch
     */
    data class RateLimited(val resetAt: Long) : ChangelogError()

    /**
     * The server answered with an unexpected HTTP status code.
     *
     * @param code The HTTP status code
     */
    data class Http(val code: Int) : ChangelogError()

    /**
     * The server answered with data that couldn't be parsed.
     */
    data object InvalidResponse : ChangelogError()
}
