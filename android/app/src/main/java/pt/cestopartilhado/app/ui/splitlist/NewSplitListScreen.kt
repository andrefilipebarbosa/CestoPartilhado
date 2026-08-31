package pt.cestopartilhado.app.ui.splitlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.theme.CestoColors

@Composable
fun NewSplitListScreen(viewModel: NewSplitListViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val isCreating by viewModel.isCreating.collectAsState()

    Scaffold(
        containerColor = CestoColors.Bg,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().background(CestoColors.Surface).padding(20.dp)) {
                Button(
                    onClick = { viewModel.createList(onCreated) },
                    enabled = !isCreating && viewModel.listName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Green, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.split_new_create_button), fontWeight = FontWeight.Bold)
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
                    stringResource(R.string.split_new_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CestoColors.Text,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.new_list_name_label), style = MaterialTheme.typography.labelMedium, color = CestoColors.Text2)
                OutlinedTextField(
                    value = viewModel.listName,
                    onValueChange = { viewModel.listName = it },
                    placeholder = { Text(stringResource(R.string.split_new_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
