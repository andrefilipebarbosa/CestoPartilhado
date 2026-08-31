package pt.cestopartilhado.app.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.model.ShoppingList
import pt.cestopartilhado.app.ui.components.AvatarCircle
import pt.cestopartilhado.app.ui.components.BottomDestination
import pt.cestopartilhado.app.ui.components.CestoBottomBar
import pt.cestopartilhado.app.ui.components.MemberAvatarStack
import pt.cestopartilhado.app.ui.theme.CestoColors
import pt.cestopartilhado.app.util.relativeTimeText

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
            FloatingActionButton(onClick = onCreateList, containerColor = CestoColors.Green, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_list_title))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_greeting, viewModel.displayName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CestoColors.Text,
                )
                AvatarCircle(label = viewModel.displayName.ifBlank { "?" }, size = 44.dp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CestoColors.Text,
                )
                if (lists.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.home_active_count, lists.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CestoColors.Text2,
                        modifier = Modifier.background(CestoColors.Surface2, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            if (lists.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_empty),
                    color = CestoColors.Text3,
                    modifier = Modifier.padding(top = 32.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(lists, key = { it.id }) { list ->
                        ListCard(list = list, selfLabel = viewModel.displayName, onClick = { onOpenList(list.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ListCard(list: ShoppingList, selfLabel: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val extraMembers = (list.memberIds.size - 1).coerceAtLeast(0)
    val progress = if (list.itemCount > 0) list.boughtCount.toFloat() / list.itemCount.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CestoColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, CestoColors.Border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            list.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = CestoColors.Text,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            MemberAvatarStack(selfLabel = selfLabel.ifBlank { "?" }, extraMembers = extraMembers)
            Text(
                text = stringResource(R.string.home_stores_count, list.storeCount.toInt()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = CestoColors.OrangeDark,
                modifier = Modifier.background(CestoColors.OrangeTint, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(CestoColors.Surface2, RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(CestoColors.Green, RoundedCornerShape(999.dp)),
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_items_progress, list.boughtCount.toInt(), list.itemCount.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = CestoColors.Text2,
            )
            Text(
                text = relativeTimeText(context, list.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = CestoColors.Text3,
            )
        }
    }
}
