package pt.cestopartilhado.app.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import pt.cestopartilhado.app.ui.theme.CestoColors

@Composable
fun LoginScreen(viewModel: LoginViewModel, onSignedIn: () -> Unit) {
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSignInResult(result.data, onSignedIn) }

    Box(
        modifier = Modifier.fillMaxSize().background(CestoColors.Bg).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CestoColors.Text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_welcome),
                style = MaterialTheme.typography.titleLarge,
                color = CestoColors.Text,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CestoColors.Text2,
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { launcher.launch(viewModel.signInIntent()) },
                enabled = !isSigningIn,
                colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Surface, contentColor = CestoColors.Text),
                border = BorderStroke(1.dp, CestoColors.Border),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.action_sign_in_google), fontWeight = FontWeight.SemiBold)
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = CestoColors.OrangeDark, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
