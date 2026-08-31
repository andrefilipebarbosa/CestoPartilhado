package pt.cestopartilhado.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tokens de marca — ver export/tokens/colors.json no pacote de design para a origem (oklch).
object CestoColors {
    val Bg = Color(0xFFFDF8ED)
    val Surface = Color(0xFFFFFDF9)
    val Surface2 = Color(0xFFF6EFE3)
    val Border = Color(0xFFE2DDD4)
    val Text = Color(0xFF1F1A10)
    val Text2 = Color(0xFF635D51)
    val Text3 = Color(0xFF918C80)
    val Green = Color(0xFF409D48)
    val GreenDark = Color(0xFF146720)
    val GreenTint = Color(0xFFD7F5D7)
    val Orange = Color(0xFFD67523)
    val OrangeDark = Color(0xFF8C3F00)
    val OrangeTint = Color(0xFFFFE1C6)
}

object CestoType {
    val DisplayLg = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
    val DisplayMd = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val DisplaySm = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val BodyLg = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    val BodyMd = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val BodySm = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val BodyXs = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
}

private val CestoColorScheme = lightColorScheme(
    primary = CestoColors.Green,
    onPrimary = Color.White,
    secondary = CestoColors.Orange,
    background = CestoColors.Bg,
    surface = CestoColors.Surface,
    onBackground = CestoColors.Text,
    onSurface = CestoColors.Text,
    outline = CestoColors.Border,
    error = CestoColors.OrangeDark,
)

@Composable
fun CestoPartilhadoTheme(content: @Composable () -> Unit) {
    // A app usa sempre a paleta clara definida na direção de marca (sem modo escuro dedicado por agora).
    MaterialTheme(
        colorScheme = CestoColorScheme,
        content = content,
    )
}
