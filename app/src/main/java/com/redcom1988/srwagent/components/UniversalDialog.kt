package com.redcom1988.srwagent.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun UniversalDialog(
    title: String,
    message: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    confirmText: String = "OK",
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    dismissText: String? = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        },
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                content = {
                    Text(
                        text = confirmText,
                        color = confirmColor
                    )
                }
            )
        },
        dismissButton = dismissText?.let {
            {
                TextButton(
                    onClick = onDismiss,
                    content = { Text(text = it) }
                )
            }
        }
    )
}
