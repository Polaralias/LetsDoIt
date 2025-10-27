package com.polaralias.letsdoit.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class AppDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppBottomBar(
    destinations: List<AppDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = destination.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                label = { Text(text = destination.label) }
            )
        }
    }
}
