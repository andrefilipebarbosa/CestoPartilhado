package pt.cestopartilhado.app.ui.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.theme.CestoColors
import pt.cestopartilhado.app.util.shareText

@Composable
fun InviteScreen(viewModel: InviteViewModel, listId: String, onBack: () -> Unit) {
    LaunchedEffect(listId) { viewModel.load(listId) }
    val list by viewModel.list.collectAsState()
    val inviteResult by viewModel.inviteResult.collectAsState()
    val isInviting by viewModel.isInviting.collectAsState()
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(containerColor = CestoColors.Bg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = CestoColors.Text)
                }
                Text(
                    stringResource(R.string.invite_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CestoColors.Text,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    stringResource(R.string.invite_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CestoColors.Text3,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                Button(
                    onClick = { shareText(context, viewModel.inviteLink()) },
                    colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Green, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(stringResource(R.string.invite_share_link), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(24.dp))

                if (viewModel.isOwner) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text(stringResource(R.string.invite_email_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.invite(email); email = "" },
                            enabled = !isInviting && email.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Text, contentColor = Color.White),
                        ) {
                            Text(stringResource(R.string.invite_button))
                        }
                    }

                    inviteResult?.let { result ->
                        Text(
                            text = when (result) {
                                "added" -> stringResource(R.string.invite_sent)
                                "pending" -> stringResource(R.string.invite_sent)
                                else -> stringResource(R.string.error_generic)
                            },
                            color = if (result == "error") CestoColors.OrangeDark else CestoColors.GreenDark,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.invite_error_owner_only),
                        color = CestoColors.Text3,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(28.dp))

                list?.let { currentList ->
                    Text(
                        stringResource(R.string.invite_people_with_access, currentList.memberIds.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = CestoColors.Text2,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Column {
                        currentList.memberIds.forEach { uid ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(if (uid == currentList.ownerId) stringResource(R.string.invite_role_owner) else stringResource(R.string.invite_role_editor), color = CestoColors.Text)
                            }
                        }
                        currentList.pendingInvites.forEach { pendingEmail ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(pendingEmail, color = CestoColors.Text)
                                Text(
                                    stringResource(R.string.invite_role_pending),
                                    color = CestoColors.OrangeDark,
                                    modifier = Modifier
                                        .background(CestoColors.OrangeTint, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
