/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector
import org.derpfest.changelog.R
import org.derpfest.changelog.models.ChangeSource

val ChangeSource.labelResId: Int
    get() = when (this) {
        ChangeSource.DERPFEST -> R.string.source_derpfest
        ChangeSource.LINEAGEOS -> R.string.source_lineageos
        ChangeSource.DEVICES -> R.string.source_devices
    }

val ChangeSource.openLabelResId: Int
    get() = when (this) {
        ChangeSource.LINEAGEOS -> R.string.change_open_gerrit
        ChangeSource.DERPFEST,
        ChangeSource.DEVICES -> R.string.change_open_github
    }

val ChangeSource.icon: ImageVector
    get() = when (this) {
        ChangeSource.DERPFEST -> Icons.Filled.Commit
        ChangeSource.LINEAGEOS -> Icons.Filled.RateReview
        ChangeSource.DEVICES -> Icons.Filled.Smartphone
    }
