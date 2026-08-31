package pt.cestopartilhado.app.ui.newlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.theme.CestoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewListScreen(viewModel: NewListViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val suggestions by viewModel.suggestions.collectAsState()
    val addedStores by viewModel.addedStores.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()

    Scaffold(
        containerColor = CestoColors.Bg,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().background(CestoColors.Surface).padding(20.dp)) {
                Button(
                    onClick = { viewModel.createList(onCreated) },
                    enabled = !isCreating && viewModel.listName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Green, contentColor = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.new_list_create_button), fontWeight = FontWeight.Bold)
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
                    stringResource(R.string.new_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CestoColors.Text,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize()) {
                Text(stringResource(R.string.new_list_name_label), style = MaterialTheme.typography.labelMedium, color = CestoColors.Text2)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.listName,
                    onValueChange = { viewModel.listName = it },
                    placeholder = { Text(stringResource(R.string.new_list_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )

                Spacer(Modifier.height(28.dp))
                Text(stringResource(R.string.new_list_stores_label), style = MaterialTheme.typography.titleMedium, color = CestoColors.Text, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.new_list_stores_subtitle), style = MaterialTheme.typography.bodySmall, color = CestoColors.Text3)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.storeQuery,
                    onValueChange = viewModel::onQueryChanged,
                    placeholder = { Text(stringResource(R.string.new_list_search_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )

                if (viewModel.storeQuery.isNotBlank()) {
                    Column(modifier = Modifier.fillMaxWidth().background(CestoColors.Surface, RoundedCornerShape(14.dp)).border(1.dp, CestoColors.Border, RoundedCornerShape(14.dp))) {
                        if (suggestions.isEmpty()) {
                            Text(
                                stringResource(R.string.new_list_no_matches),
                                style = MaterialTheme.typography.bodySmall,
                                color = CestoColors.Text3,
                                modifier = Modifier.padding(14.dp),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.addStore(viewModel.storeQuery) }
                                    .padding(14.dp),
                            ) {
                                Text(
                                    stringResource(R.string.new_list_add_store, viewModel.storeQuery),
                                    color = CestoColors.GreenDark,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else {
                            suggestions.forEach { entry ->
                                Text(
                                    entry.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.addStore(entry.name) }
                                        .padding(14.dp),
                                    color = CestoColors.Text,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.new_list_stores_added, addedStores.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = CestoColors.Text2,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    addedStores.forEach { store ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.background(CestoColors.Surface2, CircleShape).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        ) {
                            Text(store, color = CestoColors.Text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { viewModel.removeStore(store) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove), tint = CestoColors.Text3)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = CestoColors.Surface,
    unfocusedContainerColor = CestoColors.Surface,
    focusedIndicatorColor = CestoColors.Green,
    unfocusedIndicatorColor = CestoColors.Border,
)

