package de.dseelp.kotlincord.frontend.app

import androidx.compose.runtime.Composable

sealed interface Page {
    val isPluginPage: Boolean
    val title: String

    val content: @Composable () -> Unit
    val isNavigationEnabled: Boolean
}

data class NormalPage(
    override val title: String,
    override val isNavigationEnabled: Boolean = true,
    override val content: @Composable () -> Unit
): Page {
    override val isPluginPage: Boolean = false
}