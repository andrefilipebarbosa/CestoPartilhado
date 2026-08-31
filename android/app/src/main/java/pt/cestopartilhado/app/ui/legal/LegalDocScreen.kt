package pt.cestopartilhado.app.ui.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.ui.theme.CestoColors
import java.util.Locale

/** Documento legal muito simples (título "# "/"## " a negrito, resto como parágrafos). */
@Composable
fun LegalDocScreen(assetBaseName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }

    LaunchedEffect(assetBaseName) {
        val lang = if (Locale.getDefault().language == "pt") "pt" else "en"
        val fileName = "legal/$assetBaseName.$lang.md"
        content = runCatching {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    Scaffold(containerColor = CestoColors.Bg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = CestoColors.Text)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                content.lines().forEach { line ->
                    when {
                        line.startsWith("## ") -> Text(
                            line.removePrefix("## "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CestoColors.Text,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                        line.startsWith("# ") -> Text(
                            line.removePrefix("# "),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = CestoColors.Text,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        line.isBlank() -> {}
                        else -> Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CestoColors.Text2,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
