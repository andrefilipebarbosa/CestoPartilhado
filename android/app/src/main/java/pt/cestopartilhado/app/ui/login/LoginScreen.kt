package pt.cestopartilhado.app.ui.login

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import pt.cestopartilhado.app.R
import pt.cestopartilhado.app.ui.theme.CestoColors

@Composable
fun LoginScreen(viewModel: LoginViewModel, onSignedIn: () -> Unit) {
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSignInResult(result.data, onSignedIn) }

    val activity = LocalContext.current as ComponentActivity
    val facebookCallbackManager = remember { CallbackManager.Factory.create() }
    DisposableEffect(facebookCallbackManager) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                viewModel.onFacebookLoginResult(result.accessToken.token, onSignedIn)
            }
            override fun onCancel() {}
            override fun onError(error: FacebookException) {
                viewModel.onFacebookLoginError(error.message)
            }
        }
        LoginManager.getInstance().registerCallback(facebookCallbackManager, callback)
        onDispose { LoginManager.getInstance().unregisterCallback(facebookCallbackManager) }
    }

    Column(modifier = Modifier.fillMaxSize().background(CestoColors.Bg)) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CestoColors.GreenTint),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(10.dp).align(Alignment.TopStart).padding(start = 44.dp, top = 56.dp).background(CestoColors.Orange.copy(alpha = 0.55f), CircleShape))
            Box(modifier = Modifier.size(6.dp).align(Alignment.TopEnd).padding(end = 64.dp, top = 120.dp).background(CestoColors.GreenDark.copy(alpha = 0.35f), CircleShape))
            Box(modifier = Modifier.size(8.dp).align(Alignment.BottomStart).padding(start = 70.dp, bottom = 64.dp).background(CestoColors.Orange.copy(alpha = 0.4f), CircleShape))

            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_app_mark),
                contentDescription = null,
                modifier = Modifier.size(180.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(CestoColors.Surface)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 34.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_app_mark),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = CestoColors.Text,
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.login_welcome),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = CestoColors.Text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CestoColors.Text2,
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { launcher.launch(viewModel.signInIntent()) },
                enabled = !isSigningIn,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Surface, contentColor = CestoColors.Text),
                border = BorderStroke(1.dp, CestoColors.Border),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.action_sign_in_google), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    LoginManager.getInstance()
                        .logIn(activity, facebookCallbackManager, listOf("email", "public_profile"))
                },
                enabled = !isSigningIn,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CestoColors.Surface, contentColor = CestoColors.Text),
                border = BorderStroke(1.dp, CestoColors.Border),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_facebook_logo),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.action_sign_in_facebook), fontWeight = FontWeight.SemiBold)
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = CestoColors.OrangeDark, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
