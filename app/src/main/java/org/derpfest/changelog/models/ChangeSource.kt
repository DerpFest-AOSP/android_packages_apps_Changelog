/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.models

/**
 * Where a change comes from.
 */
enum class ChangeSource {
    /**
     * A repository of the ROM hosted by DerpFest.
     */
    DERPFEST,

    /**
     * A repository the ROM tracks straight from LineageOS.
     */
    LINEAGEOS,

    /**
     * A device repository hosted by DerpFest.
     */
    DEVICES,
}
