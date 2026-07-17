package com.xraypulse.app.ui.navigation

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Servers : Route("servers")
    data object Import : Route("import")
    data object Settings : Route("settings")
    data object Subscriptions : Route("subscriptions")
    data object PerApp : Route("per_app")
    data object QuickSetup : Route("quick_setup")
    data object ManualEdit : Route("manual_edit?id={id}") {
        fun create(id: Long = 0) = "manual_edit?id=$id"
    }
}
