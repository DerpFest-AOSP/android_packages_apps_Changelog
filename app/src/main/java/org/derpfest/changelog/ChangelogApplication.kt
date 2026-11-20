/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog

import android.app.Application
import org.derpfest.changelog.repositories.ManifestRepository

class ChangelogApplication : Application() {
    val manifestRepository by lazy { ManifestRepository(applicationContext) }
}
