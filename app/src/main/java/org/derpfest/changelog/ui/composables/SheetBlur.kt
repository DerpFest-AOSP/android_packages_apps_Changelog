/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.changelog.ui.composables

import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.derpfest.changelog.R

/**
 * Bottom sheet used by the changelog.
 *
 * The media panel blurs only the sheet. The frost is the volume panel tint, opened slightly so
 * the list reads through. A blur
 * behind flag on the sheet dialog cannot do that: that dialog is full screen, so the blur lands
 * on the list outside the sheet and the sheet itself is only a translucent color. The blur
 * therefore lives on its own window, sized and placed to the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val policy = remember(dark) { sheetBlurPolicy(context, dark) }
    val crossWindowBlur = rememberCrossWindowBlurEnabled()
    val showBlur = policy.eligible && crossWindowBlur
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cornerPx = with(density) { SheetCornerRadius.toPx() }
    val radiusPx = with(density) { SheetBlurRadius.roundToPx() }
    val blurWindow = remember { SheetBlurWindow(context) }

    DisposableEffect(blurWindow) {
        blurWindow.show()
        onDispose { blurWindow.dismiss() }
    }

    val dismiss: () -> Unit = {
        scope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
                if (!sheetState.isVisible) onDismissRequest()
            }
    }

    // The sheet's layout node stays at the top of the dialog. anchoredDraggable moves the
    // drawn sheet down afterwards, so the blur has to follow that offset rather than the node.
    var sheetLayout by remember { mutableStateOf<Rect?>(null) }
    val sheetOffset = sheetState.currentOffset()
    val sheetBounds = sheetLayout?.let { layout ->
        val top = sheetOffset ?: return@let layout
        Rect(layout.left, top, layout.right, top + layout.height)
    }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (sheetState.targetValue != SheetValue.Hidden) 1f else 0f,
        label = "sheetScrim",
    )
    val scrimColor = BottomSheetDefaults.ScrimColor
    val dismissDescription = stringResource(R.string.dismiss)
    val dismissState = rememberUpdatedState(dismiss)
    // The frost is the blur window's own background, one layer, same as the media panel's
    // blur drawable. The sheet stays clear so that tint is not painted a second time.
    val containerColor = when {
        showBlur -> Color.Transparent
        policy.eligible -> policy.fallback
        else -> BottomSheetDefaults.ContainerColor
    }
    val contentColor = when {
        showBlur || policy.eligible -> MaterialTheme.colorScheme.onSurface
        else -> contentColorFor(containerColor)
    }

    // Shown after the blur window, so the blur stays behind this dialog.
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            // The dialog must not dim or blur on its own. Dimming would darken the sheet blur,
            // and a window blur here would cover the list outside the sheet.
            view.dialogWindow()?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0f)
                window.setBackgroundBlurRadius(0)
                window.setFormat(PixelFormat.TRANSLUCENT)
            }
            onDispose {}
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .semantics { dialog() },
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { dismissState.value() } }
                    .semantics {
                        contentDescription = dismissDescription
                        onClick {
                            dismiss()
                            true
                        }
                    },
            ) {
                val bounds = sheetBounds
                if (!showBlur || bounds == null) {
                    drawRect(color = scrimColor, alpha = scrimAlpha)
                    return@Canvas
                }
                val hole = RoundRect(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom,
                    topLeftCornerRadius = CornerRadius(cornerPx, cornerPx),
                    topRightCornerRadius = CornerRadius(cornerPx, cornerPx),
                )
                val path = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRoundRect(hole)
                    fillType = PathFillType.EvenOdd
                }
                drawPath(path, color = scrimColor, alpha = scrimAlpha)
            }

            BottomSheet(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { coordinates ->
                        val inParent = coordinates.boundsInParent()
                        if (inParent != sheetLayout) sheetLayout = inParent
                    },
                state = sheetState,
                onDismissRequest = onDismissRequest,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = SheetCornerRadius,
                    topEnd = SheetCornerRadius,
                ),
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                content = content,
            )
        }
    }

    LaunchedEffect(blurWindow, showBlur, radiusPx, cornerPx, policy.frosted) {
        blurWindow.setActive(showBlur, radiusPx, cornerPx, policy.frosted.toArgb())
    }
    val blurBounds = sheetBounds
    if (showBlur && blurBounds != null) {
        SideEffect { blurWindow.place(blurBounds, cornerPx) }
    }
    LaunchedEffect(sheetState) { sheetState.show() }
}

/**
 * Window that covers only the sheet and blurs what is behind that rectangle.
 */
private class SheetBlurWindow(
    context: Context,
) : android.app.Dialog(context, org.derpfest.changelog.R.style.Theme_Changelog_SheetBlur) {
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
        // setBackgroundBlurRadius is ignored until the view root exists, and a later call with
        // the same radius does not try again. Apply it once the window is attached.
        window?.decorView?.post { applyBlur() }
    }

    init {
        val window = window ?: error("Sheet blur window is missing")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        val content = View(context)
        content.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setContentView(content)
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.setDimAmount(0f)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
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
    }

    fun setActive(active: Boolean, radiusPx: Int, cornerPx: Float, tint: Int) {
        val window = window ?: return
        val radius = if (active) radiusPx else 0
        val color = if (active) tint else android.graphics.Color.TRANSPARENT
        if (radius == blurRadiusPx && cornerPx == cornerRadiusPx && color == tintColor) return
        blurRadiusPx = radius
        cornerRadiusPx = cornerPx
        tintColor = color
        blurLatched = false
        // The drawable is drawn over the blur and is what gives the blur its corner radius.
        window.setBackgroundDrawable(
            GradientDrawable().apply {
                setColor(color)
                cornerRadius = cornerPx
            },
        )
        applyBlur()
    }

    private fun applyBlur() {
        val window = window ?: return
        if (!window.decorView.isAttachedToWindow) return
        window.setBackgroundBlurRadius(0)
        window.setBackgroundBlurRadius(blurRadiusPx)
        blurLatched = true
    }

    fun place(bounds: Rect, cornerPx: Float) {
        val window = window ?: return
        if (bounds.width <= 0f || bounds.height <= 0f) return
        val extra = cornerPx.roundToInt()
        val x = bounds.left.roundToInt()
        val y = bounds.top.roundToInt()
        val width = bounds.width.roundToInt().coerceAtLeast(1)
        // Push the bottom corner radius off the bottom of the screen so only the top corners
        // of this uniform radius are visible.
        val height = (bounds.height.roundToInt() + extra).coerceAtLeast(1)
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
}

@Composable
private fun rememberCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val windowManager = remember(context) {
        context.getSystemService(WindowManager::class.java)
    }
    var enabled by remember { mutableStateOf(windowManager.isCrossWindowBlurEnabled) }

    DisposableEffect(windowManager) {
        val listener = java.util.function.Consumer<Boolean> { enabled = it }
        windowManager.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }

    return enabled
}

@OptIn(ExperimentalMaterial3Api::class)
private fun SheetState.currentOffset(): Float? {
    return try {
        requireOffset()
    } catch (_: IllegalStateException) {
        null
    }
}

/**
 * Whether the sheet and the toolbar should blur, and the frost drawn over that blur.
 *
 * @param active Cross-window blur can be drawn right now
 * @param frosted surface_effect_0 composited over surface_effect_2
 */
internal class SurfaceBlur(
    val active: Boolean,
    val frosted: Color,
)

/**
 * The blur policy shared by the sheet and the floating toolbar.
 */
@Composable
internal fun rememberSurfaceBlur(): SurfaceBlur {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val policy = remember(dark) { sheetBlurPolicy(context, dark) }
    val crossWindowBlur = rememberCrossWindowBlurEnabled()
    return SurfaceBlur(
        active = policy.eligible && crossWindowBlur,
        frosted = policy.frosted,
    )
}

private fun View.dialogWindow(): Window? {
    var current: View? = this
    while (current != null) {
        if (current is DialogWindowProvider) {
            return current.window
        }
        current = current.parent as? View
    }
    return null
}

private class SheetBlurPolicy(
    val eligible: Boolean,
    val frosted: Color,
    val fallback: Color,
)

private fun sheetBlurPolicy(context: Context, dark: Boolean): SheetBlurPolicy {
    return SheetBlurPolicy(
        eligible = isSheetBlurEligible(),
        frosted = Color(frostedSheetColor(context, dark)),
        fallback = Color(
            context.getColor(
                // surface_effect_0_fallback: secondary tone 20 in dark, tone 80 in light.
                when {
                    dark -> android.R.color.system_accent2_800
                    else -> android.R.color.system_accent2_200
                },
            ),
        ),
    )
}

/**
 * True when SystemUI would blur the volume panel and the media sheet: the flag is on, blur
 * expansion is enabled, and this device can render background blurs. A value that cannot be read
 * does not turn the effect off; the live cross-window blur listener still does.
 */
private fun isSheetBlurEligible(): Boolean {
    return readBlurOnMoreSurfaces() != false &&
        readBlurExpansionEnabled() != false &&
        readSupportsBackgroundBlur() != false
}

private fun readBlurOnMoreSurfaces(): Boolean? {
    return try {
        val deviceConfig = Class.forName("android.provider.DeviceConfig")
        val getBoolean = deviceConfig.getMethod(
            "getBoolean",
            String::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType,
        )
        getBoolean.invoke(
            null,
            "systemui",
            "com.android.systemui.blur_on_more_surfaces",
            true,
        ) as Boolean
    } catch (error: Throwable) {
        Log.w(TAG, "Could not read blur_on_more_surfaces", error)
        null
    }
}

private fun readBlurExpansionEnabled(): Boolean? {
    val resources = Resources.getSystem()
    val id = resources.getIdentifier("config_enableBlurExpansion", "bool", "android")
    if (id != 0) {
        return resources.getBoolean(id)
    }

    return try {
        val bools = Class.forName("com.android.internal.R\$bool")
        val fieldId = bools.getField("config_enableBlurExpansion").getInt(null)
        resources.getBoolean(fieldId)
    } catch (error: Throwable) {
        Log.w(TAG, "Could not read config_enableBlurExpansion", error)
        null
    }
}

private fun readSupportsBackgroundBlur(): Boolean? {
    val supported = readSystemBoolean("ro.surface_flinger.supports_background_blur") ?: return null
    val disabled = readSystemBoolean("persist.sysui.disableBlur") ?: false
    return supported && !disabled
}

private fun readSystemBoolean(name: String): Boolean? {
    return try {
        val properties = Class.forName("android.os.SystemProperties")
        val getBoolean = properties.getMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType,
        )
        getBoolean.invoke(null, name, false) as Boolean
    } catch (error: Throwable) {
        Log.w(TAG, "Could not read $name", error)
        null
    }
}

/**
 * surface_effect_0 composited over surface_effect_2, the media panel's sheet color.
 *
 * The framework colors already carry the right alpha (dark: primary tone 20 at 50% over tone 90
 * at 15%). That composite is then opened slightly so the list reads through the sheet and the
 * toolbar. The public accent shade numbers run the other way — accent1_200 is tone 80, not tone
 * 20 — so the fallback maps tone to shade explicitly.
 */
private fun frostedSheetColor(context: Context, dark: Boolean): Int {
    val effect0 = systemColor(context, "customColorSurfaceEffect0")
    val effect2 = systemColor(context, "customColorSurfaceEffect2")
    // The static framework values are opaque placeholders. The real tokens are translucent
    // and follow the wallpaper; if those did not resolve, build them from the accent shades.
    if (effect0 != null && effect2 != null &&
        android.graphics.Color.alpha(effect0) != 0xFF &&
        android.graphics.Color.alpha(effect2) != 0xFF
    ) {
        return thinFrost(ColorUtils.compositeColors(effect0, effect2))
    }
    val primary = if (dark) {
        android.R.color.system_accent1_800 to android.R.color.system_accent1_100
    } else {
        android.R.color.system_accent1_100 to android.R.color.system_accent1_0
    }
    val overlay = ColorUtils.setAlphaComponent(
        context.getColor(primary.first),
        (SURFACE_EFFECT_0_ALPHA * 255).roundToInt(),
    )
    val under = ColorUtils.setAlphaComponent(
        context.getColor(primary.second),
        ((if (dark) SURFACE_EFFECT_2_ALPHA_DARK else SURFACE_EFFECT_2_ALPHA_LIGHT) * 255)
            .roundToInt(),
    )
    return thinFrost(ColorUtils.compositeColors(overlay, under))
}

/**
 * Fraction of the volume-panel frost kept on the sheet and the toolbar. A little under 1
 * lets the blurred list show through without changing the tint.
 */
private const val FROST_SCALE = 0.8f

private fun thinFrost(color: Int): Int {
    val alpha = (android.graphics.Color.alpha(color) * FROST_SCALE).roundToInt()
    return android.graphics.Color.argb(
        alpha.coerceIn(0, 255),
        android.graphics.Color.red(color),
        android.graphics.Color.green(color),
        android.graphics.Color.blue(color),
    )
}

private fun systemColor(context: Context, name: String): Int? {
    val id = Resources.getSystem().getIdentifier(name, "color", "android")
    if (id == 0) return null
    return try {
        context.getColor(id)
    } catch (error: Throwable) {
        Log.w(TAG, "Could not read $name", error)
        null
    }
}

private const val TAG = "SheetBlur"

/** Matches frameworks/base SystemUI R.dimen.bottomsheet_blur_radius. */
internal val SheetBlurRadius = 30.dp

/** Matches frameworks/base SystemUI R.dimen.bottom_sheet_corner_radius. */
private val SheetCornerRadius = 28.dp

private const val SURFACE_EFFECT_0_ALPHA = 0.5f
private const val SURFACE_EFFECT_2_ALPHA_DARK = 0.15f
private const val SURFACE_EFFECT_2_ALPHA_LIGHT = 0.32f
