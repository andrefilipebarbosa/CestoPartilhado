package pt.cestopartilhado.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.model.ShoppingList
import pt.cestopartilhado.app.ui.components.BottomDestination
import pt.cestopartilhado.app.ui.components.CestoBottomBar
import pt.cestopartilhado.app.ui.theme.CestoColors

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenList: (String) -> Unit,
    onCreateList: () -> Unit,
    onNavigate: (BottomDestination) -> Unit,
) {
    val lists by viewModel.activeLists.collectAsState()

    Scaffold(
        containerColor = CestoColors.Bg,
        bottomBar = { CestoBottomBar(BottomDestination.LISTS, onNavigate) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateList, containerColor = CestoColors.Green, contentColor = androidx.compose.ui.graphics.Color.White) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_list_title))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.home_greeting, viewModel.displayName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = CestoColors.Text,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.titleMedium,
                color = CestoColors.Text2,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (lists.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_empty),
                    color = CestoColors.Text3,
                    modifier = Modifier.padding(top = 32.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(lists, key = { it.id }) { list ->
                        ListCard(list = list, onClick = { onOpenList(list.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ListCard(list: ShoppingList, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CestoColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, CestoColors.Border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column {
            Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CestoColors.Text)
            Text(
                text = stringResource(R.string.home_stores_count, list.storeCount.toInt()) +
                    "  ·  " + stringResource(R.string.home_items_progress, list.boughtCount.toInt(), list.itemCount.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = CestoColors.Text3,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
