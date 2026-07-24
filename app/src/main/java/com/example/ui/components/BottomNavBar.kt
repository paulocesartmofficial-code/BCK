package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBluePrimary
import com.example.ui.viewmodel.AppTab

@Composable
fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentTab is AppTab.Dashboard,
            onClick = { onTabSelected(AppTab.Dashboard) },
            icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Início") },
            label = { Text("Início") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBluePrimary,
                selectedTextColor = DarkBluePrimary,
                indicatorColor = DarkBluePrimary.copy(alpha = 0.12f)
            )
        )

        NavigationBarItem(
            selected = currentTab is AppTab.Clients,
            onClick = { onTabSelected(AppTab.Clients) },
            icon = { Icon(imageVector = Icons.Default.People, contentDescription = "Clientes") },
            label = { Text("Clientes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBluePrimary,
                selectedTextColor = DarkBluePrimary,
                indicatorColor = DarkBluePrimary.copy(alpha = 0.12f)
            )
        )

        NavigationBarItem(
            selected = currentTab is AppTab.Payments,
            onClick = { onTabSelected(AppTab.Payments) },
            icon = { Icon(imageVector = Icons.Default.Payment, contentDescription = "Pagamentos") },
            label = { Text("Pagamentos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBluePrimary,
                selectedTextColor = DarkBluePrimary,
                indicatorColor = DarkBluePrimary.copy(alpha = 0.12f)
            )
        )

        NavigationBarItem(
            selected = currentTab is AppTab.Reports,
            onClick = { onTabSelected(AppTab.Reports) },
            icon = { Icon(imageVector = Icons.Default.BarChart, contentDescription = "Relatórios") },
            label = { Text("Relatórios") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBluePrimary,
                selectedTextColor = DarkBluePrimary,
                indicatorColor = DarkBluePrimary.copy(alpha = 0.12f)
            )
        )

        NavigationBarItem(
            selected = currentTab is AppTab.Settings,
            onClick = { onTabSelected(AppTab.Settings) },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Ajustes") },
            label = { Text("Ajustes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBluePrimary,
                selectedTextColor = DarkBluePrimary,
                indicatorColor = DarkBluePrimary.copy(alpha = 0.12f)
            )
        )
    }
}

