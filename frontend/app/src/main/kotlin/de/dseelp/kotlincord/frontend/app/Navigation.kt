package de.dseelp.kotlincord.frontend.app

import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

object Navigation {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val currentPage = remember { NormalPage("") {} }

    val isLoginView
    get() = currentPage == loginPage

    fun showLogin() {
        navigate(loginPage)
    }

    val pageContent = remember { currentPage.content }
    val pageTitle = remember { currentPage.title }

    fun navigate(page: Page) {

    }


}