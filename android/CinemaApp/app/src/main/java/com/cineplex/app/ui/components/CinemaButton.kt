package com.cineplex.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cineplex.app.ui.theme.BackgroundDeep
import com.cineplex.app.ui.theme.CinemaGold
import com.cineplex.app.ui.theme.CinemaTypography
import com.cineplex.app.ui.theme.TextDisabled

@Composable
fun CinemaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = CinemaGold,
    contentColor: Color = BackgroundDeep,
    height: Dp = 52.dp,
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Button(
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= 800L && !isLoading && enabled) {
                lastClickTime = currentTime
                onClick()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = TextDisabled
        )
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Please wait...",
                    style = CinemaTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = text,
                style = CinemaTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
