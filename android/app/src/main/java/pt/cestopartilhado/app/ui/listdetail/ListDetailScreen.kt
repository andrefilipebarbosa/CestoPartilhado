package pt.cestopartilhado.app.ui.listdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.model.ShoppingItem
import pt.cestopartilhado.app.model.ShoppingList
import pt.cestopartilhado.app.model.StoreWithItems
import pt.cestopartilhado.app.ui.theme.CestoColors

@Composable
fun ListDetailScreen(
    viewModel: ListDetailViewModel,
    listId: String,
    onBack: () -> Unit,
    onInvite: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(listId) { viewModel.load(listId) }
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var addStoreOpen by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }

    if (error != null) {
        Column(modifier = Modifier.fillMaxSize().background(CestoColors.Bg).padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = CestoColors.Text)
                }
            }
            Text(stringResource(R.string.error_generic), color = CestoColors.OrangeDark)
        }
        return
    }

    val listWithStores = state ?: return
    val list = listWithStores.list

    Scaffold(
        containerColor = CestoColors.Bg,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(CestoColors.Surface).padding(20.dp)) {
                if (list.status == ShoppingList.STATUS_ACTIVE) {
                    Button(
                        onClick = viewModel::closeList,
                        colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Green, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_close_list), fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = viewModel::reopenList,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(stringResource(R.string.action_recover), fontWeight = FontWeight.Bold, color = CestoColors.GreenDark)
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = CestoColors.Text)
                }
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CestoColors.Text,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = CestoColors.Text)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.invite_title)) },
                            onClick = { menuOpen = false; onInvite(list.id) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.list_detail_add_store)) },
                            onClick = { menuOpen = false; addStoreOpen = true },
                        )
                        if (viewModel.isOwner) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_detail_delete_button), color = CestoColors.OrangeDark) },
                                onClick = { menuOpen = false; viewModel.deleteList(onDeleted) },
                            )
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.list_detail_stores_count, listWithStores.stores.size) + "  ·  " +
                    stringResource(R.string.home_items_progress, listWithStores.boughtItems, listWithStores.totalItems),
                style = MaterialTheme.typography.bodySmall,
                color = CestoColors.Text2,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            if (addStoreOpen) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = newStoreName,
                        onValueChange = { newStoreName = it },
                        placeholder = { Text(stringResource(R.string.list_detail_add_store)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        if (newStoreName.isNotBlank()) { viewModel.addStore(newStoreName.trim()); newStoreName = ""; addStoreOpen = false }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_add), tint = CestoColors.Green)
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 4.dp, 20.dp, 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(listWithStores.stores, key = { it.store.id }) { storeWithItems ->
                    StoreSection(
                        storeWithItems = storeWithItems,
                        onToggleItem = { itemId, bought -> viewModel.toggleItem(storeWithItems.store.id, itemId, bought) },
                        onRemoveItem = { item -> viewModel.removeItem(storeWithItems.store.id, item.id, item.bought) },
                        onAddItem = { name -> viewModel.addItem(storeWithItems.store.id, name, "") },
                        onRemoveStore = { viewModel.removeStore(storeWithItems.store.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreSection(
    storeWithItems: StoreWithItems,
    onToggleItem: (String, Boolean) -> Unit,
    onRemoveItem: (ShoppingItem) -> Unit,
    onAddItem: (String) -> Unit,
    onRemoveStore: () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    var newItemName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CestoColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, CestoColors.Border, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        ) {
            Text(
                storeWithItems.store.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CestoColors.Text,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${storeWithItems.boughtCount}/${storeWithItems.items.size}",
                style = MaterialTheme.typography.bodySmall,
                color = CestoColors.Text3,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = CestoColors.Text3,
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                storeWithItems.items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onToggleItem(item.id, !item.bought) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(if (item.bought) CestoColors.Green else Color.Transparent, CircleShape)
                                .border(if (item.bought) 0.dp else 2.dp, CestoColors.Border, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.bought) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.bought) CestoColors.Text3 else CestoColors.Text,
                            textDecoration = if (item.bought) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveItem(item) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove), tint = CestoColors.Text3)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        placeholder = { Text(stringResource(R.string.item_name_placeholder)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        if (newItemName.isNotBlank()) { onAddItem(newItemName.trim()); newItemName = "" }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_add), tint = CestoColors.GreenDark)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onRemoveStore() }.padding(top = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.action_remove) + " " + storeWithItems.store.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = CestoColors.OrangeDark,
                    )
                }
            }
        }
    }
}
