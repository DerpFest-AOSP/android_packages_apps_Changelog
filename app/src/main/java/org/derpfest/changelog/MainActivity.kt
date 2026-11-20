/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.derpfest.changelog.ui.ChangelogApp
import org.derpfest.changelog.ui.theme.ChangelogTheme
import org.derpfest.changelog.viewmodels.ChangelogViewModel
import org.derpfest.changelog.viewmodels.DevicesViewModel

class MainActivity : ComponentActivity() {
    // View models
    private val changelogViewModel: ChangelogViewModel by viewModels()
    private val devicesViewModel: DevicesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ChangelogTheme {
                ChangelogApp(
                    changelogViewModel = changelogViewModel,
                    devicesViewModel = devicesViewModel,
                )
            }
        }
    }
}
