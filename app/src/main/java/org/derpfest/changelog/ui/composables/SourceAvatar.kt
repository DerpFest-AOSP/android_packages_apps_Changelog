/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.derpfest.changelog.ext.icon
import org.derpfest.changelog.ext.labelResId
import org.derpfest.changelog.models.ChangeSource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourceAvatar(
    source: ChangeSource,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = source.containerColor,
                shape = source.shape.toShape(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = source.icon,
            contentDescription = stringResource(source.labelResId),
            modifier = Modifier.size(20.dp),
            tint = source.contentColor,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ChangeSource.shape
    get() = when (this) {
        ChangeSource.DERPFEST -> MaterialShapes.Cookie9Sided
        ChangeSource.LINEAGEOS -> MaterialShapes.Clover4Leaf
        ChangeSource.DEVICES -> MaterialShapes.Pill
    }

private val ChangeSource.containerColor: Color
    @Composable
    get() = when (this) {
        ChangeSource.DERPFEST -> MaterialTheme.colorScheme.primaryContainer
        ChangeSource.LINEAGEOS -> MaterialTheme.colorScheme.secondaryContainer
        ChangeSource.DEVICES -> MaterialTheme.colorScheme.tertiary
    }

private val ChangeSource.contentColor: Color
    @Composable
    get() = when (this) {
        ChangeSource.DERPFEST -> MaterialTheme.colorScheme.onPrimaryContainer
        ChangeSource.LINEAGEOS -> MaterialTheme.colorScheme.onSecondaryContainer
        ChangeSource.DEVICES -> MaterialTheme.colorScheme.onTertiary
    }
