package com.github.se.orator.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.se.orator.model.profile.UserProfileViewModel
import com.github.se.orator.model.theme.AppThemeViewModel
import com.github.se.orator.ui.navigation.NavigationActions
import com.github.se.orator.ui.theme.AppDimensions
import com.github.se.orator.ui.theme.AppFontSizes

// class for all that is needed about a section for settings
data class SettingBar(
    val text: String,
    val testTag: String,
    val icon: ImageVector,
    val iconDescription: String
)

// creating a list of all settings to implement them with a simple loop
val listOfSettings =
    listOf(
        SettingBar("Permissions", "permissions", Icons.Outlined.Lock, "lock icon"),
        SettingBar("Theme", "theme", Icons.Outlined.DarkMode, "theme"),
        SettingBar("About", "about", Icons.Outlined.Info, "info icon"))

// reusable function that is called to add a section to settings
@Composable
fun TextButtonFun(settingBar: SettingBar, onClick: () -> Unit = {}) {
  TextButton(
      onClick = { onClick() },
      modifier = Modifier.fillMaxWidth().testTag(settingBar.testTag),
      contentPadding = PaddingValues(AppDimensions.nullPadding) // Remove default padding
      ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start) {
              Icon(
                  imageVector = settingBar.icon,
                  contentDescription = settingBar.iconDescription,
                  modifier = Modifier.size(AppDimensions.iconSizeLarge).testTag("icon"))

              Text(
                  modifier =
                      Modifier.padding(
                          start = AppDimensions.paddingSmallMedium,
                          top = AppDimensions.paddingTopSmall),
                  text = settingBar.text,
                  color = MaterialTheme.colorScheme.onBackground,
                  fontSize = AppFontSizes.titleLarge)
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    navigationActions: NavigationActions,
    userProfileViewModel: UserProfileViewModel,
    themeViewModel: AppThemeViewModel? = null
) {
  val context = LocalContext.current
  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  "Settings",
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.testTag("SettingsText"))
            },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("back_button")) {
                    androidx.compose.material.Icon(
                        Icons.Outlined.ArrowBackIosNew,
                        contentDescription = "Back button",
                        modifier = Modifier.size(AppDimensions.iconSizeMedium),
                        tint = MaterialTheme.colorScheme.onSurface)
                  }
            },
            colors =
                TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ))
      }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("settingsScreen"),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacerWidthMedium)) {

              // Permissions
              item {
                TextButtonFun(listOfSettings[0]) {
                  context.startActivity(
                      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                      })
                }
              }
              item {
                TextButtonFun(listOfSettings[1]) {
                  themeViewModel?.switchTheme()
                  Log.d("SettingsScreen", "Theme switch")
                }
              }
              item { TextButtonFun(listOfSettings[2]) { Log.d("SettingsScreen", "About") } }
            }
      }
}
