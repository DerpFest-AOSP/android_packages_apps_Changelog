/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

import kotlinx.serialization.Serializable

/**
 * A device that DerpFest ships builds for.
 *
 * @param codename The codename of the device
 * @param displayName The user-facing name of the device
 * @param maintainer Who maintains the device, if known
 */
@Serializable
data class Device(
    val codename: String,
    val displayName: String,
    val maintainer: String? = null,
)
