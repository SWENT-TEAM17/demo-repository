package com.github.se.orator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object Route {
    const val HOME = "Home"
    const val FRIENDS = "Friends"
    const val PROFILE = "Profile"
    const val AUTH = "Auth"
    const val EDIT_PROFILE = "EditProfile"
    const val CREATE_PROFILE = "CreateProfile"
}

object Screen {
    const val AUTH = "Auth Screen"
    const val HOME = "Home Screen"
    const val FRIENDS = "Friends Screen"
    const val PROFILE = "Profile Screen"
    const val EDIT_PROFILE = "EditProfile Screen"
    const val CREATE_PROFILE = "CreateProfile Screen"
}

data class TopLevelDestination(val route: String, val icon: ImageVector, val textId: String)

object TopLevelDestinations {
    val HOME =
        TopLevelDestination(route = Route.HOME, icon = Icons.Outlined.Menu, textId = "Home")
    val FRIENDS =
        TopLevelDestination(route = Route.FRIENDS, icon = Icons.Outlined.Place, textId = "Friends")
    val PROFILE =
        TopLevelDestination(route = Route.PROFILE, icon = Icons.Outlined.Place, textId = "Profile")
}

val LIST_TOP_LEVEL_DESTINATION =
    listOf(TopLevelDestinations.HOME, TopLevelDestinations.FRIENDS, TopLevelDestinations.PROFILE)

open class NavigationActions(
    private val navController: NavHostController,
) {
    /**
     * Navigate to the specified [TopLevelDestination]
     *
     * @param destination The top level destination to navigate to Clear the back stack when
     *   navigating to a new destination This is useful when navigating to a new screen from the
     *   bottom navigation bar as we don't want to keep the previous screen in the back stack
     */
    open fun navigateTo(destination: TopLevelDestination) {

        navController.navigate(destination.route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
                inclusive = true
            }

            // Avoid multiple copies of the same destination when reselecting same item
            launchSingleTop = true

            // Restore state when reselecting a previously selected item
            if (destination.route != Route.AUTH) {
                restoreState = true
            }
        }
    }

    /**
     * Navigate to the specified screen.
     *
     * @param screen The screen to navigate to
     */
    open fun navigateTo(screen: String) {
        navController.navigate(screen)
    }

    /** Navigate back to the previous screen. */
    open fun goBack() {
        navController.popBackStack()
    }

    /**
     * Get the current route of the navigation controller.
     *
     * @return The current route
     */
    open fun currentRoute(): String {
        return navController.currentDestination?.route ?: ""
    }
}