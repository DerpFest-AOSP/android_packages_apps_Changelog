/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import org.derpfest.changelog.R
import org.derpfest.changelog.models.ChangelogError
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/**
 * The text explaining the error to the user. Rate limits count down until they get reset.
 */
val ChangelogError.text: String
    @Composable
    get() = when (this) {
        ChangelogError.Network -> stringResource(R.string.error_network)

        is ChangelogError.RateLimited -> {
            val minutes by produceState(minutesUntil(resetAt), resetAt) {
                while (value > 0) {
                    delay(TimeUnit.SECONDS.toMillis(1))
                    value = minutesUntil(resetAt)
                }
            }

            when (minutes) {
                0 -> stringResource(R.string.error_rate_limit_over)
                else -> pluralStringResource(R.plurals.error_rate_limited, minutes, minutes)
            }
        }

        is ChangelogError.Http -> stringResource(R.string.error_http, code)

        ChangelogError.InvalidResponse -> stringResource(R.string.error_invalid_response)
    }

private fun minutesUntil(time: Long) = ceil(
    (time - System.currentTimeMillis()) / TimeUnit.MINUTES.toMillis(1).toDouble()
).toInt().coerceAtLeast(0)
