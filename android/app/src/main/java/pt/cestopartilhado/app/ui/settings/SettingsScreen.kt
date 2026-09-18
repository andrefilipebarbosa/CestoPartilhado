package pt.cestopartilhado.app.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import pt.cestopartilhado.app.ui.components.BottomDestination
import pt.cestopartilhado.app.ui.components.CestoBottomBar
import pt.cestopartilhado.app.ui.theme.CestoColors
import pt.cestopartilhado.app.util.LocaleManager
import pt.cestopartilhado.app.util.shareTextAsFile

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (BottomDestination) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val isExporting by viewModel.isExporting.collectAsState()
    val notificationPrefs by viewModel.notificationPrefs.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = CestoColors.Bg,
        bottomBar = { CestoBottomBar(BottomDestination.SETTINGS, onNavigate) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = CestoColors.Text,
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
            )

            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium, color = CestoColors.Text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LanguageOption(LocaleManager.LANGUAGE_SYSTEM, stringResource(R.string.settings_language_system), viewModel, context)
            LanguageOption(LocaleManager.LANGUAGE_PT, stringResource(R.string.settings_language_pt), viewModel, context)
            LanguageOption(LocaleManager.LANGUAGE_EN, stringResource(R.string.settings_language_en), viewModel, context)

            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.settings_notifications_title), style = MaterialTheme.typography.titleMedium, color = CestoColors.Text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            NotificationToggle(stringResource(R.string.notification_pref_added), notificationPrefs["added"] ?: true) { viewModel.setNotificationPref("added", it) }
            NotificationToggle(stringResource(R.string.notification_pref_closed), notificationPrefs["closed"] ?: true) { viewModel.setNotificationPref("closed", it) }
            NotificationToggle(stringResource(R.string.notification_pref_edited), notificationPrefs["edited"] ?: false) { viewModel.setNotificationPref("edited", it) }

            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.settings_export_title), style = MaterialTheme.typography.titleMedium, color = CestoColors.Text, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_export_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = CestoColors.Text3,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            TextButton(
                enabled = !isExporting,
                onClick = {
                    viewModel.exportData { text ->
                        shareTextAsFile(context, text, "cesto-partilhado-dados.txt")
                    }
                },
            ) {
                Text(stringResource(R.string.settings_export_button), color = CestoColors.GreenDark, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.settings_legal), style = MaterialTheme.typography.titleMedium, color = CestoColors.Text, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onOpenTerms) { Text(stringResource(R.string.terms_of_service), color = CestoColors.Text2) }
            TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.privacy_policy), color = CestoColors.Text2) }

            Spacer(Modifier.height(28.dp))
            TextButton(onClick = { viewModel.signOut(onSignedOut) }) {
                Text(stringResource(R.string.settings_sign_out), color = CestoColors.OrangeDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NotificationToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Text(label, color = CestoColors.Text, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun LanguageOption(value: String, label: String, viewModel: SettingsViewModel, context: android.content.Context) {
    val selected = viewModel.currentLanguage(context) == value
    val onSelect: () -> Unit = {
        viewModel.setLanguage(context, value)
        if (context is Activity) context.recreate()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, color = CestoColors.Text)
    }
}
