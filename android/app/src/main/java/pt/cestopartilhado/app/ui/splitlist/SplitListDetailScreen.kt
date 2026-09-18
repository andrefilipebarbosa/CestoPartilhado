package pt.cestopartilhado.app.ui.splitlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.model.SplitItem
import pt.cestopartilhado.app.ui.components.AvatarCircle
import pt.cestopartilhado.app.ui.theme.CestoColors

private fun formatValue(value: Double): String = "%.2f €".format(value)

@Composable
fun SplitListDetailScreen(
    viewModel: SplitListDetailViewModel,
    listId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    LaunchedEffect(listId) { viewModel.load(listId) }
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val inviteResult by viewModel.inviteResult.collectAsState()
    val memberLabels by viewModel.memberLabels.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemValue by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }

    if (error != null) {
        Column(modifier = Modifier.fillMaxSize().background(CestoColors.Bg).padding(32.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = CestoColors.Text)
            }
            Text(stringResource(R.string.error_generic), color = CestoColors.OrangeDark)
        }
        return
    }

    val data = state ?: return
    val list = data.list
    val locked = list.isLocked

    Scaffold(containerColor = CestoColors.Bg) { padding ->
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
                if (locked) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = CestoColors.Text3, modifier = Modifier.padding(end = 8.dp))
                }
                if (viewModel.isOwner) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = CestoColors.Text)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.split_delete_list), color = CestoColors.OrangeDark) },
                                onClick = { menuOpen = false; viewModel.deleteList(onDeleted) },
                            )
                        }
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 32.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CestoColors.GreenTint, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.split_detail_total, formatValue(list.totalValue)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CestoColors.Text,
                        )
                        Text(
                            stringResource(R.string.split_detail_your_share, formatValue(list.shareValue)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CestoColors.GreenDark,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                if (locked) {
                    item {
                        Text(
                            stringResource(R.string.split_detail_locked_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = CestoColors.OrangeDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CestoColors.OrangeTint, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                        )
                    }
                }

                items(data.items, key = { it.id }) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CestoColors.Surface, RoundedCornerShape(14.dp))
                            .border(1.dp, CestoColors.Border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(item.name, color = CestoColors.Text, modifier = Modifier.weight(1f))
                        Text(formatValue(item.value), color = CestoColors.Text2, fontWeight = FontWeight.Medium)
                        if (!locked) {
                            IconButton(onClick = { viewModel.removeItem(item) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove), tint = CestoColors.Text3)
                            }
                        }
                    }
                }

                if (!locked) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newItemName,
                                onValueChange = { newItemName = it },
                                placeholder = { Text(stringResource(R.string.split_item_name_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = newItemValue,
                                onValueChange = { newItemValue = it },
                                placeholder = { Text(stringResource(R.string.split_item_value_placeholder)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(110.dp),
                            )
                            IconButton(onClick = {
                                val value = newItemValue.replace(',', '.').toDoubleOrNull()
                                if (newItemName.isNotBlank() && value != null) {
                                    viewModel.addItem(newItemName.trim(), value)
                                    newItemName = ""; newItemValue = ""
                                }
                            }) {
                                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_add), tint = CestoColors.GreenDark)
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.split_people_title, list.memberIds.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = CestoColors.Text2,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(list.memberIds, key = { it }) { memberUid ->
                    val paid = memberUid in list.paidMemberIds
                    val isSelf = memberUid == viewModel.currentUid
                    val label = if (isSelf) viewModel.displayName.ifBlank { "?" } else memberLabels[memberUid] ?: memberUid.take(8)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    ) {
                        AvatarCircle(
                            label = label,
                            background = if (memberUid == list.ownerId) CestoColors.Green else CestoColors.Orange,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (isSelf) "$label (tu)" else label,
                            color = CestoColors.Text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatValue(list.shareValue), color = CestoColors.Text2, modifier = Modifier.padding(end = 10.dp))
                        val statusText = if (paid) stringResource(R.string.split_status_paid) else stringResource(R.string.split_status_owing)
                        val statusColor = if (paid) CestoColors.GreenDark else CestoColors.OrangeDark
                        val statusBg = if (paid) CestoColors.GreenTint else CestoColors.OrangeTint
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier
                                .background(statusBg, RoundedCornerShape(999.dp))
                                .let { if (viewModel.isOwner) it.clickable { viewModel.setMemberPaid(memberUid, !paid) } else it }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                items(list.pendingInvites, key = { it }) { email ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(email, color = CestoColors.Text3, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(R.string.invite_role_pending),
                            style = MaterialTheme.typography.labelSmall,
                            color = CestoColors.OrangeDark,
                            modifier = Modifier.background(CestoColors.OrangeTint, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                if (viewModel.isOwner && !locked) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = inviteEmail,
                                onValueChange = { inviteEmail = it },
                                placeholder = { Text(stringResource(R.string.split_invite_email_placeholder)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                if (inviteEmail.isNotBlank()) { viewModel.invite(inviteEmail.trim()); inviteEmail = "" }
                            }) {
                                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_invite), tint = CestoColors.GreenDark)
                            }
                        }
                        inviteResult?.let { result ->
                            Text(
                                if (result == "error") stringResource(R.string.error_generic) else stringResource(R.string.invite_sent),
                                color = if (result == "error") CestoColors.OrangeDark else CestoColors.GreenDark,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else if (!viewModel.isOwner) {
                    item {
                        Text(
                            stringResource(R.string.split_owner_only_payments),
                            style = MaterialTheme.typography.bodySmall,
                            color = CestoColors.Text3,
                        )
                    }
                }
            }
        }
    }
}
