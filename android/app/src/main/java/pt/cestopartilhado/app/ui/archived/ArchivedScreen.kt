package pt.cestopartilhado.app.ui.archived

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun ArchivedScreen(viewModel: ArchivedViewModel, onNavigate: (BottomDestination) -> Unit) {
    val lists by viewModel.closedLists.collectAsState()

    Scaffold(
        containerColor = CestoColors.Bg,
        bottomBar = { CestoBottomBar(BottomDestination.ARCHIVE, onNavigate) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.archived_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = CestoColors.Text,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            Text(
                stringResource(R.string.archived_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CestoColors.Text2,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(
                stringResource(R.string.archived_notice),
                style = MaterialTheme.typography.bodySmall,
                color = CestoColors.Text2,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CestoColors.Surface2, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            )
            Spacer(Modifier.height(12.dp))

            if (lists.isEmpty()) {
                Text(stringResource(R.string.archived_empty), color = CestoColors.Text3, modifier = Modifier.padding(top = 32.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)) {
                    items(lists, key = { it.id }) { list ->
                        ArchivedCard(
                            list = list,
                            daysLeft = viewModel.daysUntilDeletion(list),
                            isOwner = list.ownerId == viewModel.currentUid,
                            onRecover = { viewModel.recover(list.id) },
                            onDeleteNow = { viewModel.deleteNow(list.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedCard(
    list: ShoppingList,
    daysLeft: Int,
    isOwner: Boolean,
    onRecover: () -> Unit,
    onDeleteNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CestoColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, CestoColors.Border, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = CestoColors.Text)
            Text(
                stringResource(R.string.archived_deletes_in, daysLeft),
                style = MaterialTheme.typography.bodySmall,
                color = if (daysLeft <= 5) CestoColors.OrangeDark else CestoColors.Text2,
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            OutlinedButton(onClick = onRecover) {
                Text(stringResource(R.string.action_recover), color = CestoColors.GreenDark, fontWeight = FontWeight.SemiBold)
            }
            if (isOwner) {
                TextButton(onClick = onDeleteNow) {
                    Text(stringResource(R.string.archived_delete_now), color = CestoColors.Text3)
                }
            }
        }
    }
}
