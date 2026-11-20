/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

/**
 * The default shared preferences of the app.
 */
val Context.sharedPreferences: SharedPreferences
    get() = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

/**
 * Share a link through the share sheet.
 *
 * @param url The link
 * @param title The title shown in the share sheet
 */
fun Context.shareLink(url: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_TITLE, title)
    }

    startActivity(Intent.createChooser(intent, null))
}

/**
 * Copy a link to the clipboard.
 *
 * @param url The link
 * @param label A user-visible label for the link
 */
fun Context.copyLink(url: String, label: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(
        ClipData.newPlainText(label, url)
    )
}
