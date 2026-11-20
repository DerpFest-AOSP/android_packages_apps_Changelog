/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import org.derpfest.changelog.utils.CommitMessageUtils

/**
 * The body of a commit message, with its links made clickable.
 */
@Composable
fun CommitMessage(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )

    val annotatedText = remember(text, linkStyles) {
        buildAnnotatedString {
            append(text)

            CommitMessageUtils.findUrls(text).forEach {
                addLink(
                    url = LinkAnnotation.Url(url = text.substring(it), styles = linkStyles),
                    start = it.first,
                    end = it.last + 1,
                )
            }
        }
    }

    Text(
        text = annotatedText,
        modifier = modifier,
        style = style,
    )
}
