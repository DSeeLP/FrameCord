package de.dseelp.kotlincord.frontend.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun navigation(scaffoldState: ScaffoldState, scope: CoroutineScope) {
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = {
                    Text("KotlinCord")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            launch {
                                scaffoldState.snackbarHostState.showSnackbar("Hi")
                            }
                            scaffoldState.drawerState.toggle()
                        }
                    }) {
                        Icon(Icons.Filled.Menu, "Hamburger")
                    }
                }
            )
        }
    ) {
        ModalDrawer(
            drawerState = scaffoldState.drawerState,
            drawerContent = {
                Text("Hi")
            },
            drawerElevation = 5.dp,
            drawerShape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
        ) {

        }
    }
}