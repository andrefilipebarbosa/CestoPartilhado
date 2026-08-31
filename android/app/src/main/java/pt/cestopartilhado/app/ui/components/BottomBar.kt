package pt.cestopartilhado.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.theme.CestoColors

enum class BottomDestination { LISTS, ARCHIVE, SETTINGS }

@Composable
fun CestoBottomBar(current: BottomDestination, onSelect: (BottomDestination) -> Unit) {
    NavigationBar(
        containerColor = CestoColors.Surface,
        modifier = Modifier.border(1.dp, CestoColors.Border),
    ) {
        NavigationBarItem(
            selected = current == BottomDestination.LISTS,
            onClick = { onSelect(BottomDestination.LISTS) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_lists)) },
            colors = navColors(),
        )
        NavigationBarItem(
            selected = current == BottomDestination.ARCHIVE,
            onClick = { onSelect(BottomDestination.ARCHIVE) },
            icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_archive)) },
            colors = navColors(),
        )
        NavigationBarItem(
            selected = current == BottomDestination.SETTINGS,
            onClick = { onSelect(BottomDestination.SETTINGS) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            colors = navColors(),
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CestoColors.Green,
    selectedTextColor = CestoColors.Green,
    unselectedIconColor = CestoColors.Text3,
    unselectedTextColor = CestoColors.Text3,
    indicatorColor = Color.Transparent,
)
