package com.bitchat.android.posta

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.R

/** Color de cada nivel. Fijos y con contraste suficiente sobre fondo claro y oscuro. */
fun PostaStatus.Severity.color(): Color = when (this) {
    PostaStatus.Severity.OK -> Color(0xFF2E9E5B)
    PostaStatus.Severity.INFO -> Color(0xFF4C8DF6)
    PostaStatus.Severity.WARNING -> Color(0xFFE08A00)
    PostaStatus.Severity.DANGER -> Color(0xFFE5484D)
}

@StringRes
fun PostaStatus.labelRes(): Int = when (this) {
    PostaStatus.OK -> R.string.posta_status_ok
    PostaStatus.MEETING_POINT -> R.string.posta_status_meeting
    PostaStatus.NEED_HELP -> R.string.posta_status_help
    PostaStatus.MEDICAL -> R.string.posta_status_medical
}

/**
 * Fila de estados de emergencia de un toque, encima del campo de mensaje.
 *
 * Cada botón abre una confirmación que dice exactamente a quién llegará el
 * mensaje (privado y cifrado, canal o público para todos los cercanos) y permite
 * añadir una nota breve. Se envía por el mismo camino que cualquier mensaje.
 */
@Composable
fun PostaStatusBar(
    selectedPrivatePeer: String?,
    currentChannel: String?,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pending by remember { mutableStateOf<PostaStatus?>(null) }
    val barLabel = stringResource(R.string.posta_status_bar_label)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .semantics { contentDescription = barLabel },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (status in PostaStatus.entries) {
            val tint = status.severity.color()
            AssistChip(
                onClick = { pending = status },
                label = {
                    Text(
                        text = "${status.emoji} ${stringResource(status.labelRes())}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                },
                border = BorderStroke(1.dp, tint.copy(alpha = 0.7f)),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = tint.copy(alpha = 0.12f),
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }

    pending?.let { status ->
        PostaStatusConfirmDialog(
            status = status,
            selectedPrivatePeer = selectedPrivatePeer,
            currentChannel = currentChannel,
            onDismiss = { pending = null },
            onConfirm = { note ->
                pending = null
                onSend(status.toWire(note))
            },
        )
    }
}

@Composable
private fun PostaStatusConfirmDialog(
    status: PostaStatus,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by rememberSaveable(status) { mutableStateOf("") }
    val label = stringResource(status.labelRes())
    val destination = when {
        selectedPrivatePeer != null -> stringResource(R.string.posta_status_dest_private)
        currentChannel != null -> stringResource(R.string.posta_status_dest_channel, currentChannel)
        else -> stringResource(R.string.posta_status_dest_public)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.posta_status_confirm_title, "${status.emoji} $label")) },
        text = {
            Column {
                Text(destination, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(PostaStatus.MAX_NOTE_LENGTH) },
                    label = { Text(stringResource(R.string.posta_status_note_label)) },
                    placeholder = { Text(stringResource(R.string.posta_status_note_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = { onConfirm(note) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.posta_status_preview_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = status.toWire(note),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = status.severity.color(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.posta_status_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }) {
                Text(stringResource(R.string.posta_status_send), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.posta_status_cancel)) }
        },
    )
}
