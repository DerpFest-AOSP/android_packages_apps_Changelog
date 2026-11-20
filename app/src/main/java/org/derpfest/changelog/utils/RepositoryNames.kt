/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.utils

object RepositoryNames {
    /**
     * Strip the prefixes LineageOS and DerpFest use interchangeably for the same tree.
     *
     * @param name The repository name
     */
    fun normalize(name: String) = name
        .removePrefix("android_")
        .removePrefix("proprietary_")

    /**
     * Whether the repository belongs to the device with [codename].
     *
     * @param name The repository name
     * @param codename The codename of the device
     */
    fun matchesCodename(name: String, codename: String): Boolean {
        val normalized = normalize(name)
        val suffix = "_$codename"

        return normalized.equals(codename, ignoreCase = true) ||
                normalized.endsWith(suffix, ignoreCase = true)
    }
}
