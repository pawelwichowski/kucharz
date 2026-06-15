package com.example.kucharz2.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kucharz2.ui.ingredients.IngredientInputScreen
import com.example.kucharz2.ui.pantry.PantryScreen
import com.example.kucharz2.ui.recipes.RecipeResultsScreen
import com.example.kucharz2.ui.saved.SavedRecipesScreen
import com.example.kucharz2.ui.settings.SettingsScreen
import com.example.kucharz2.ui.shopping.ShoppingListScreen

private sealed class Screen(val route: String, val label: String, val icon: String) {
    data object Ingredients : Screen("ingredients", "Składniki", "🥕")
    data object Recipes : Screen("recipes", "Przepisy", "🍲")
    data object Shopping : Screen("shopping", "Zakupy", "✅")
    data object History : Screen("history", "Zapisane", "⭐")
    data object Pantry : Screen("pantry", "Stałe", "🧂")
    data object Settings : Screen("settings", "Ustawienia", "⚙️")
}

private val bottomScreens = listOf(
    Screen.Ingredients,
    Screen.Recipes,
    Screen.Shopping,
    Screen.History,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KucharzApp(
    isDarkTheme: Boolean = false,
    onDarkThemeChange: (Boolean) -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.Ingredients.route

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kucharz", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            NavigationBar {
                bottomScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route || (screen == Screen.Settings && currentRoute == Screen.Pantry.route),
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = false
                                popUpTo(Screen.Ingredients.route) { saveState = false }
                            }
                        },
                        icon = { Text(screen.icon) },
                        label = { Text(screen.label, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Ingredients.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Ingredients.route) { IngredientInputScreen() }
            composable(Screen.Recipes.route) { RecipeResultsScreen() }
            composable(Screen.Shopping.route) { ShoppingListScreen() }
            composable(Screen.History.route) { SavedRecipesScreen() }
            composable(Screen.Pantry.route) { PantryScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChange = onDarkThemeChange,
                    onOpenPantry = {
                        navController.navigate(Screen.Pantry.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
