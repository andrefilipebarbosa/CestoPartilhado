package pt.cestopartilhado.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pt.cestopartilhado.app.ui.theme.CestoColors

@Composable
fun AvatarCircle(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    background: Color = CestoColors.Green,
    border: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape)
            .then(if (border) Modifier.border(2.dp, CestoColors.Surface, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Avatar do utilizador atual + um badge "+N" se houver mais membros na lista. */
@Composable
fun MemberAvatarStack(selfLabel: String, extraMembers: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        AvatarCircle(label = selfLabel, size = 32.dp, background = CestoColors.Green, border = true)
        if (extraMembers > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-10).dp)
                    .size(32.dp)
                    .background(CestoColors.Surface2, CircleShape)
                    .border(2.dp, CestoColors.Surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$extraMembers",
                    color = CestoColors.Text2,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
