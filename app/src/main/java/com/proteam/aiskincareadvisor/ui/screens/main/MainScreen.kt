// MainScreen.kt
package com.proteam.aiskincareadvisor.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.proteam.aiskincareadvisor.R
import com.proteam.aiskincareadvisor.ui.screens.analysis.SkinAnalysisScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {

    val navController = rememberNavController()
    val screens = listOf(
        BottomNavItem("home", "Home", ImageVector.vectorResource(id = R.drawable.ic_home)),
        BottomNavItem("analysis", "Analyze", ImageVector.vectorResource(id = R.drawable.ic_camera)),
        BottomNavItem("products", "Products", Icons.Default.ShoppingCart),
        BottomNavItem("profile", "Profile", Icons.Default.Person)
    )

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBars = currentRoute != "chat"

    Scaffold(
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Skin Butler",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your AI skincare advisor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    actions = {
                        // 进入聊天页
                        IconButton(onClick = {
                            navController.navigate("chat") {
                                launchSingleTop = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = "Chat"
                            )
                        }

                        // 预留通知按钮
                        IconButton(onClick = { /* TODO: Handle notifications */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            if (showBars) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    val currentDestination =
                        navController.currentBackStackEntryAsState().value?.destination?.route
                    screens.forEach { screen ->
                        val selected = currentDestination == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(screen.route) {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(
                                    screen.label,
                                    fontSize = 12.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("analysis") {
                AnalysisScreen(
                    navController = navController,
                    onNavigateToAnalysis = {
                        navController.navigate("skin_analysis")
                    }
                )
            }
            composable("skin_analysis") {
                SkinAnalysisScreen()
            }
            composable("products") { ProductScreen() }
            composable("profile") {
                ProfileScreen(navController = navController, onLogout = onLogout)
            }
            composable("chat") {
                ChatScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("change_password") {
                ChangePasswordScreen(navController = navController)
            }
            composable("routine") {
                RoutineScreen()
            }
            composable("settings") {
                com.proteam.aiskincareadvisor.ui.screens.SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)
