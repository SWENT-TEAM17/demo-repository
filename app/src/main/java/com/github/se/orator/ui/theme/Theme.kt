package com.github.se.orator.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.github.se.orator.model.theme.AppThemeViewModel

object AppDimensions {

  // Reference Dimensions
  private const val MODEL_WIDTH_DP = 448.0f // Base width in dp from design
  private const val MODEL_HEIGHT_DP = 923.0f // Base height in dp from design

  // Scale factors based on current screen dimensions
  @Composable
  private fun scaleFactorWidth(): Float {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    return remember(screenWidthDp) { screenWidthDp / MODEL_WIDTH_DP }
  }

  @Composable
  private fun scaleFactorHeight(): Float {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    return remember(screenHeightDp) { screenHeightDp / MODEL_HEIGHT_DP }
  }

  // Padding
  val nullPadding = 0.dp
  val paddingSmall: Dp
    @Composable get() = (scaleFactorWidth() * 8.0f).dp

  val paddingMedium: Dp
    @Composable get() = (scaleFactorWidth() * 16.0f).dp

  val paddingLarge: Dp
    @Composable get() = (scaleFactorWidth() * 24.0f).dp

  val paddingExtraLarge: Dp
    @Composable get() = (scaleFactorWidth() * 32.0f).dp

  val paddingXXLarge: Dp
    @Composable get() = (scaleFactorWidth() * 42.0f).dp

  val paddingXXXLarge: Dp
    @Composable get() = (scaleFactorWidth() * 64.0f).dp

  val paddingExtraSmall: Dp
    @Composable get() = (scaleFactorWidth() * 4.0f).dp

  val paddingSmallMedium: Dp
    @Composable get() = (scaleFactorWidth() * 12.0f).dp

  val paddingMediumSmall: Dp
    @Composable get() = (scaleFactorWidth() * 20.0f).dp

  val paddingTopSmall: Dp
    @Composable get() = (scaleFactorHeight() * 5.0f).dp

  val statusBarPadding: Dp
    @Composable get() = (scaleFactorHeight() * 10.0f).dp

  val smallPadding: Dp
    @Composable get() = paddingExtraSmall

  // Spacer Dimensions
  val largeSpacerHeight: Dp
    @Composable get() = (scaleFactorHeight() * 100.0f).dp

  val mediumSpacerHeight: Dp
    @Composable get() = (scaleFactorHeight() * 50.0f).dp

  val smallSpacerHeight: Dp
    @Composable get() = (scaleFactorHeight() * 15.0f).dp

  val spacerWidthMedium: Dp
    @Composable get() = (scaleFactorWidth() * 16.0f).dp

  val spacerHeightLarge: Dp
    @Composable get() = (scaleFactorHeight() * 30.0f).dp

  val spacerHeightMedium: Dp
    @Composable get() = (scaleFactorHeight() * 8.0f).dp

  val spacerHeightDefault: Dp
    @Composable get() = (scaleFactorHeight() * 32.0f).dp

  // Button Heights and Sizes
  val buttonHeight: Dp
    @Composable get() = (scaleFactorHeight() * 48.0f).dp

  val buttonHeightLarge: Dp
    @Composable get() = (scaleFactorHeight() * 50.0f).dp

  val cardHeightmin: Dp
    @Composable get() = (scaleFactorHeight() * 200.0f).dp

  val cardHeightmax: Dp
    @Composable get() = (scaleFactorHeight() * 300.0f).dp

  val buttonHeightRounded: Dp
    @Composable get() = buttonHeightLarge

  val buttonSize: Dp
    @Composable get() = (scaleFactorWidth() * 80.0f).dp

  val buttonWidthMin: Dp
    @Composable get() = (scaleFactorWidth() * 200.0f).dp

  val buttonWidthMax: Dp
    @Composable get() = (scaleFactorWidth() * 300.0f).dp

  // Logo Dimensions
  val logoSize: Dp
    @Composable get() = (scaleFactorWidth() * 250.0f).dp

  val logoTextWidth: Dp
    @Composable get() = (scaleFactorWidth() * 276.0f).dp

  val logoTextHeight: Dp
    @Composable get() = (scaleFactorHeight() * 141.0f).dp

  val googleLogoSize: Dp
    @Composable get() = (scaleFactorWidth() * 30.0f).dp

  val imageLargeXXL: Dp
    @Composable get() = (scaleFactorWidth() * 350.0f).dp

  // Icon Sizes
  val iconSize: Dp
    @Composable get() = (scaleFactorWidth() * 100.0f).dp

  val iconSizeSmall: Dp
    @Composable get() = (scaleFactorWidth() * 32.0f).dp

  val iconSizeMedium: Dp
    @Composable get() = (scaleFactorWidth() * 24.0f).dp

  val iconSizeLarge: Dp
    @Composable get() = (scaleFactorWidth() * 35.0f).dp

  val iconSizeMic: Dp
    @Composable get() = (scaleFactorWidth() * 48.0f).dp

  // Elevation and Stroke
  val strokeWidth: Dp
    @Composable get() = (scaleFactorWidth() * 8.0f).dp

  val borderStrokeWidth: Dp
    @Composable get() = (scaleFactorWidth() * 1.0f).dp

  val dividerThickness: Dp
    @Composable get() = (scaleFactorWidth() * 3.0f).dp

  val appBarElevation: Dp = 0.dp // Fixed value

  val elevationSmall: Dp
    @Composable get() = (scaleFactorWidth() * 4.0f).dp

  val shadowElevation: Dp
    @Composable get() = (scaleFactorWidth() * 5.0f).dp

  // Corner Radii
  val roundedCornerRadius: Dp
    @Composable get() = (scaleFactorWidth() * 12.0f).dp

  val cornerRadiusSmall: Dp
    @Composable get() = (scaleFactorWidth() * 8.0f).dp

  // Heights and Widths
  val mediumHeight: Dp
    @Composable get() = (scaleFactorHeight() * 64.0f).dp

  val heightMedium: Dp
    @Composable get() = (scaleFactorHeight() * 24.0f).dp

  val smallWidth: Dp
    @Composable get() = (scaleFactorWidth() * 12.0f).dp

  // Navigation
  val bottomNavigationHeight: Dp
    @Composable get() = (scaleFactorHeight() * 60.0f).dp

  // Input Fields
  val inputFieldHeight = 56.0.dp

  val bioFieldHeight: Dp
    @Composable get() = (scaleFactorHeight() * 150.0f).dp

  // Profile Picture and Cards
  val profilePictureDialogSize: Dp
    @Composable get() = (scaleFactorWidth() * 200.0f).dp

  val profilePictureSize: Dp
    @Composable get() = (scaleFactorWidth() * 100.0f).dp

  val slightlyLargerProfilePictureSize: Dp
    @Composable get() = (scaleFactorWidth() * 107.0f).dp
  // Circle with background image and profile picture

  val slightlyLowerProfilePictureSize: Dp
    @Composable get() = (scaleFactorWidth() * 97.0f).dp

  val profileBoxHeight: Dp
    @Composable get() = (scaleFactorHeight() * 250.0f).dp

  val profileCardHeight: Dp
    @Composable get() = (scaleFactorHeight() * 140.0f).dp

  val cardSectionHeight: Dp
    @Composable get() = (scaleFactorHeight() * 100.0f).dp

  val cardHorizontalPadding: Dp
    @Composable get() = (scaleFactorWidth() * 30.0f).dp

  val cardCornerRadius: Dp
    @Composable get() = roundedCornerRadius

  val cardImageHeight: Dp
    @Composable get() = (scaleFactorHeight() * 160.0f).dp

  // Spacing
  val spacingXLarge: Dp
    @Composable get() = (scaleFactorWidth() * 40.0f).dp

  // Visualizer Height
  val visualizerHeight: Dp
    @Composable get() = (scaleFactorHeight() * 100.0f).dp

  val jobDescriptionInputFieldHeight: Dp
    @Composable get() = (scaleFactorHeight() * 200.0f).dp

  val graphHeight: Dp
    @Composable get() = (scaleFactorHeight() * 150.0f).dp

  val graphWidth: Dp
    @Composable get() = (scaleFactorHeight() * 200.0f).dp

  // Drawer Padding
  val drawerPadding: Dp
    @Composable get() = spacerWidthMedium

  // Loading Indicator
  val loadingIndicatorSize: Dp
    @Composable get() = (scaleFactorWidth() * 64.0f).dp

  // Other Dimensions
  const val FULL: Float = 1f // Assuming this is a constant and doesn't need scaling
  const val ZERO: Float = 0f
  const val X_VALUE_FOR_OFFSET: Float = -20f
  const val DRAW_TEXT_TICK_LABEL_X: Float = -50f
  const val DRAW_TEXT_TICK_LABEL_OFFSET_VALUE_FOR_Y: Float = 10f
  const val TICK_LABEL_TEXT_SIZE: Float = 20f
  const val AXIS_STROKE_WIDTH: Float = 10f
  const val PLOT_LINE_STROKE_WIDTH: Float = 5f
  const val POINTS_RADIUS: Float = 7f
}

object Constants {
  // Progress Delay
  const val PROCESSING_INCREMENT_DELAY_MS: Long = 200L
  const val QUICK_FILL_INCREMENT_DELAY_MS: Long = 100L

  // Progress Increments
  const val PROCESSING_PROGRESS_INCREMENT: Float = 0.01f
  const val QUICK_FILL_PROGRESS_INCREMENT: Float = 0.02f
}

object AppShapes {
  // You may have an AppShapes object for reusable shapes
  val bottomNavigationItemCornerRadius =
      50.dp // Corresponds to the corner radius of BottomNavigationItem
  val bottomNavigationItemShape = RoundedCornerShape(bottomNavigationItemCornerRadius)
  val circleShape = CircleShape // Added for circular buttons
}

private val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

private val darkScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

val unspecified_scheme =
    ColorFamily(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ProjectTheme(
    themeViewModel: AppThemeViewModel = AppThemeViewModel(LocalContext.current),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
  themeViewModel.loadTheme(isSystemInDarkTheme())
  val theme by themeViewModel.isDark.collectAsState()

  val colorScheme = if (theme) darkScheme else lightScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.primary.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = theme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = CustomTypography, content = content)
}
