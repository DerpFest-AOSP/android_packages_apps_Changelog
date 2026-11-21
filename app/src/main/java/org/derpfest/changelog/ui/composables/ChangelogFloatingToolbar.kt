/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt
import org.derpfest.changelog.ui.theme.ChangelogTheme
import org.derpfest.changelog.R

/**
 * How far past its own height the toolbar slides so it clears the navigation bar.
 */
private val ToolbarSlideClearance = 64.dp

/**
 * Fraction of the sheet frost kept on the toolbar. A little under 1 lets the blurred list
 * show through without changing the tint.
 */
private const val TOOLBAR_FROST_SCALE = 0.8f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogFloatingToolbar(
    visible: Boolean,
    covered: Boolean,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onShowFilters: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blur = rememberSurfaceBlur()
    if (!blur.active) {
        val clearance = with(LocalDensity.current) { ToolbarSlideClearance.roundToPx() }
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = slideInVertically { it + clearance },
            exit = slideOutVertically { it + clearance },
        ) {
            ToolbarButtons(
                onRefresh = onRefresh,
                onSearch = onSearch,
                onShowFilters = onShowFilters,
                onShowDevices = onShowDevices,
                onShowSettings = onShowSettings,
            )
        }
        return
    }

    // A sheet or the search screen covers this spot. The window would otherwise sit above them.
    if (covered) return

    val slide by animateDpAsState(
        targetValue = if (visible) 0.dp else FloatingToolbarDefaults.ContainerSize + ToolbarSlideClearance,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "toolbarSlide",
    )
    val hidden = FloatingToolbarDefaults.ContainerSize + ToolbarSlideClearance
    if (!visible && slide >= hidden) return

    // The pill stays in the scaffold slot so its position matches the flat toolbar, but it is not
    // drawn there. Background blur is the window background, so the icons have to be drawn in
    // that window; a window placed over the activity would frost over the icons.
    var bounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val inWindow = coordinates.boundsInWindow()
            if (inWindow != bounds) bounds = inWindow
        },
    ) {
        ToolbarIcons(
            onRefresh = {},
            onSearch = {},
            onShowFilters = {},
            onShowDevices = {},
            onShowSettings = {},
            modifier = Modifier
                .clearAndSetSemantics {}
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {}
                },
        )
    }

    val place = bounds ?: return
    val context = LocalContext.current
    // Read the tint here. The dialog window does not inherit this theme's content color.
    val iconTint = FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContentColor
    val blurWindow = remember { ToolbarBlurWindow(context) }
    val slidePx = with(LocalDensity.current) { slide.roundToPx() }
    val radiusPx = with(LocalDensity.current) { SheetBlurRadius.roundToPx() }
    // Same frost as the sheets, opened slightly so the list reads through the pill.
    val frosted = thinToolbarFrost(blur.frosted.toArgb())

    DisposableEffect(blurWindow) {
        blurWindow.show()
        onDispose { blurWindow.dismiss() }
    }
    SideEffect {
        blurWindow.setToolbarContent {
            ChangelogTheme {
                // No container fill here. HorizontalFloatingToolbar draws one, and that
                // covers the window blur. The frost and the blur are the window background.
                ToolbarIcons(
                    onRefresh = onRefresh,
                    onSearch = onSearch,
                    onShowFilters = onShowFilters,
                    onShowDevices = onShowDevices,
                    onShowSettings = onShowSettings,
                    tint = iconTint,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        blurWindow.place(place, slidePx, radiusPx, frosted)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolbarButtons(
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onShowFilters: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSettings: () -> Unit,
    containerColor: Color = FloatingToolbarDefaults.standardFloatingToolbarColors()
        .toolbarContainerColor,
    modifier: Modifier = Modifier,
) {
    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors()
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = colors.copy(toolbarContainerColor = containerColor),
        expandedShadowElevation = 0.dp,
        collapsedShadowElevation = 0.dp,
    ) {
        ToolbarButton(
            icon = Icons.Filled.Search,
            label = stringResource(R.string.search),
            onClick = onSearch,
        )

        ToolbarButton(
            icon = Icons.Filled.FilterList,
            label = stringResource(R.string.filters),
            onClick = onShowFilters,
        )

        ToolbarButton(
            icon = Icons.Filled.Devices,
            label = stringResource(R.string.devices),
            onClick = onShowDevices,
        )

        ToolbarButton(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.settings),
            onClick = onShowSettings,
        )

        ToolbarButton(
            icon = Icons.Filled.Refresh,
            label = stringResource(R.string.refresh),
            onClick = onRefresh,
        )
    }
}

/**
 * The toolbar icons without a container. The blur window supplies the pill.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolbarIcons(
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onShowFilters: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSettings: () -> Unit,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(FloatingToolbarDefaults.ContainerSize)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        ToolbarButton(Icons.Filled.Search, stringResource(R.string.search), onSearch, tint)
        ToolbarButton(Icons.Filled.FilterList, stringResource(R.string.filters), onShowFilters, tint)
        ToolbarButton(Icons.Filled.Devices, stringResource(R.string.devices), onShowDevices, tint)
        ToolbarButton(Icons.Filled.Settings, stringResource(R.string.settings), onShowSettings, tint)
        ToolbarButton(Icons.Filled.Refresh, stringResource(R.string.refresh), onRefresh, tint)
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified,
) {
    ToolbarTooltip(label) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
            )
        }
    }
}

/**
 * Pill-sized window. Its background blurs the list behind the toolbar and the icons are drawn
 * on top of that, in this same window.
 */
private class ToolbarBlurWindow(
    context: Context,
) : android.app.Dialog(context, org.derpfest.changelog.R.style.Theme_Changelog_SheetBlur) {
    private val composeView: ComposeView
    private var blurRadiusPx = 0
    private var cornerRadiusPx = 0f
    private var tintColor = android.graphics.Color.TRANSPARENT
    private var placedX = Int.MIN_VALUE
    private var placedY = Int.MIN_VALUE
    private var placedWidth = 0
    private var placedHeight = 0
    private var blurLatched = false

    override fun show() {
        super.show()
        window?.decorView?.post { applyBlur() }
    }

    init {
        val activity = context.findActivity()
        val window = window ?: error("Toolbar blur window is missing")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(activity)
            setViewTreeViewModelStoreOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
        }
        setContentView(composeView)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setDimAmount(0f)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        window.setElevation(0f)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes = window.attributes.apply {
            gravity = Gravity.TOP or Gravity.LEFT
            fitInsetsTypes = 0
            fitInsetsSides = 0
            horizontalMargin = 0f
            verticalMargin = 0f
            x = 0
            y = context.resources.displayMetrics.heightPixels
            width = 1
            height = 1
        }
        window.decorView.setPadding(0, 0, 0, 0)
        clearContentBackground()
    }

    fun setToolbarContent(content: @Composable () -> Unit) {
        composeView.setContent(content)
    }

    fun place(bounds: Rect, dy: Int, radiusPx: Int, tint: Int) {
        val window = window ?: return
        if (bounds.width <= 0f || bounds.height <= 0f) return
        val corner = bounds.height / 2f
        setActive(radiusPx, corner, tint)
        val x = bounds.left.roundToInt()
        val y = bounds.top.roundToInt() + dy
        val width = bounds.width.roundToInt().coerceAtLeast(1)
        val height = bounds.height.roundToInt().coerceAtLeast(1)
        if (x == placedX && y == placedY && width == placedWidth && height == placedHeight) return
        placedX = x
        placedY = y
        placedWidth = width
        placedHeight = height
        val params = window.attributes
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = x
        params.y = y
        params.width = width
        params.height = height
        window.attributes = params
        if (!blurLatched) applyBlur()
    }

    private fun setActive(radiusPx: Int, cornerPx: Float, tint: Int) {
        val window = window ?: return
        if (radiusPx == blurRadiusPx && cornerPx == cornerRadiusPx && tint == tintColor) return
        blurRadiusPx = radiusPx
        cornerRadiusPx = cornerPx
        tintColor = tint
        blurLatched = false
        window.setBackgroundDrawable(
            GradientDrawable().apply {
                setColor(tint)
                cornerRadius = cornerPx
            },
        )
        clearContentBackground()
        applyBlur()
    }

    private fun applyBlur() {
        val window = window ?: return
        if (!window.decorView.isAttachedToWindow) return
        window.setBackgroundBlurRadius(0)
        window.setBackgroundBlurRadius(blurRadiusPx)
        blurLatched = true
    }

    private fun clearContentBackground() {
        var view: View? = composeView
        while (view != null && view !== window?.decorView) {
            view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            view = view.parent as? View
        }
    }
}

private fun thinToolbarFrost(color: Int): Int {
    val alpha = (android.graphics.Color.alpha(color) * TOOLBAR_FROST_SCALE).roundToInt()
    return android.graphics.Color.argb(
        alpha.coerceIn(0, 255),
        android.graphics.Color.red(color),
        android.graphics.Color.green(color),
        android.graphics.Color.blue(color),
    )
}

private tailrec fun Context.findActivity(): ComponentActivity {
    if (this is ComponentActivity) return this
    if (this is ContextWrapper) return baseContext.findActivity()
    error("Toolbar blur window requires an activity")
}
