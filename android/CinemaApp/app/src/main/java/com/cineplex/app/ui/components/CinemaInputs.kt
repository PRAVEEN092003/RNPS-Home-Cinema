package com.cineplex.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cineplex.app.ui.theme.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = CinemaTypography.bodyMedium) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = IconTint) }
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = ErrorRed, style = CinemaTypography.bodySmall) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CinemaGold,
            unfocusedBorderColor = Divider,
            focusedLabelColor = CinemaGold,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            focusedContainerColor = BackgroundCard,
            unfocusedContainerColor = BackgroundCard,
            errorBorderColor = ErrorRed,
        ),
    )
}
