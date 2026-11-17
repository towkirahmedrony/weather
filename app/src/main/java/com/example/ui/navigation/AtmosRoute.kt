package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AtmosRoute {
    @Serializable
    data object Home : AtmosRoute

    @Serializable
    data object Map : AtmosRoute

    @Serializable
    data object Locations : AtmosRoute

    @Serializable
    data object Settings : AtmosRoute
}
