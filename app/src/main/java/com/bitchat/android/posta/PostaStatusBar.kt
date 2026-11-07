package com.bitchat.android.posta

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.R
import com.bitchat.android.geohash.ChannelID
import com.bitchat.android.geohash.LocationChannelManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

/** En los avisos de ayuda la ubicación se ofrece activada; en el resto, desactivada. */
private val PostaStatus.locationByDefault: Boolean
    get() = severity == PostaStatus.Severity.WARNING || severity == PostaStatus.Severity.DANGER

/**
 * Fila de estados de emergencia de un toque, encima del campo de mensaje,
 * más el acceso a las señales SOS (linterna, pantalla, sonido y 112).
 */
@Composable
fun PostaStatusBar(
    selectedPrivatePeer: String?,
    currentChannel: String?,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pending by remember { mutableStateOf<PostaStatus?>(null) }
    var showSos by rememberSaveable { mutableStateOf(false) }
    val barLabel = stringResource(R.string.posta_status_bar_label)
    val context = LocalContext.current
    val locationChannels = remember { LocationChannelManager.getInstance(context) }
    val selectedChannel by locationChannels.selectedChannel.collectAsState()
    val geohashChannel = (selectedChannel as? ChannelID.Location)?.channel?.displayName
    // Only the open Bluetooth mesh benefits from repeating: geohash channels go over the internet.
    val isMeshPublic = selectedPrivatePeer == null && currentChannel == null && geohashChannel == null
    val beacon by PostaBeacon.state.collectAsState()

    beacon?.let { active ->
        val parsed = remember(active.content) { PostaStatus.parse(active.content) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.posta_beacon_active,
                    parsed?.let { "${it.status.emoji} ${stringResource(it.status.labelRes())}" } ?: "",
                    active.repeats,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = (parsed?.status?.severity ?: PostaStatus.Severity.INFO).color(),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { PostaBeacon.stop() }) { Text(stringResource(R.string.posta_beacon_stop)) }
        }
    }

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
        AssistChip(
            onClick = { showSos = true },
            label = {
                Text("🆘 ${stringResource(R.string.posta_sos_chip)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            },
            border = BorderStroke(1.5.dp, Color(0xFFE5484D)),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color(0xFFE5484D).copy(alpha = 0.18f),
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }

    pending?.let { status ->
        PostaStatusConfirmDialog(
            status = status,
            selectedPrivatePeer = selectedPrivatePeer,
            currentChannel = currentChannel,
            geohashChannel = geohashChannel,
            willRepeat = isMeshPublic && PostaSettings.values.value.beaconEnabled,
            onDismiss = { pending = null },
            onConfirm = { wire ->
                pending = null
                onSend(wire)
                if (isMeshPublic) PostaBeacon.start(wire)
            },
        )
    }
    if (showSos) SosScreen(onDismiss = { showSos = false })
}

@Composable
private fun PostaStatusConfirmDialog(
    status: PostaStatus,
    selectedPrivatePeer: String?,
    currentChannel: String?,
    geohashChannel: String?,
    willRepeat: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var note by rememberSaveable(status) { mutableStateOf("") }
    val canLocate = remember { PostaLocationProvider.hasPermission(context) }
    var withLocation by rememberSaveable(status) { mutableStateOf(status.locationByDefault && canLocate) }
    var locating by remember { mutableStateOf(false) }
    var locateJob by remember { mutableStateOf<Job?>(null) }
    var locationProblem by remember { mutableStateOf<String?>(null) }
    var lastLocation by remember { mutableStateOf<PostaCoordinates?>(null) }

    val label = stringResource(status.labelRes())
    val destination = when {
        selectedPrivatePeer != null -> stringResource(R.string.posta_status_dest_private)
        currentChannel != null -> stringResource(R.string.posta_status_dest_channel, currentChannel)
        geohashChannel != null -> stringResource(R.string.posta_status_dest_geohash, geohashChannel)
        else -> stringResource(R.string.posta_status_dest_public)
    }
    val repeatNote = if (willRepeat) {
        val v = PostaSettings.values.value
        stringResource(R.string.posta_beacon_will_repeat, v.beaconIntervalMin, v.beaconDurationHours)
    } else null
    val msgNoPermission = stringResource(R.string.posta_location_no_permission)
    val msgOff = stringResource(R.string.posta_location_off)
    val msgTimeout = stringResource(R.string.posta_location_timeout)

    fun sendWith(location: PostaCoordinates?) = onConfirm(status.toWire(note, location))

    fun confirm() {
        if (!withLocation) return sendWith(null)
        locating = true
        locationProblem = null
        locateJob = scope.launch {
            when (val r = PostaLocationProvider.current(context)) {
                is PostaLocationProvider.Result.Found -> {
                    lastLocation = r.coordinates
                    locating = false
                    sendWith(r.coordinates)
                }
                PostaLocationProvider.Result.NoPermission -> { locating = false; locationProblem = msgNoPermission }
                PostaLocationProvider.Result.LocationOff -> { locating = false; locationProblem = msgOff }
                PostaLocationProvider.Result.Timeout -> { locating = false; locationProblem = msgTimeout }
            }
        }
    }

    fun launchSms(location: PostaCoordinates?) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                    .putExtra("sms_body", status.toWire(note, location))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    // El SMS a veces sale cuando no hay datos ni llamadas: necesita muy poca señal.
    fun openSms() {
        if (!withLocation || lastLocation != null) return launchSms(lastLocation)
        locating = true
        locateJob = scope.launch {
            val r = PostaLocationProvider.current(context)
            locating = false
            val found = (r as? PostaLocationProvider.Result.Found)?.coordinates
            lastLocation = found
            launchSms(found)
        }
    }

    AlertDialog(
        onDismissRequest = { locateJob?.cancel(); onDismiss() },
        title = { Text(stringResource(R.string.posta_status_confirm_title, "${status.emoji} $label")) },
        text = {
            Column {
                Text(destination, style = MaterialTheme.typography.bodyMedium)
                repeatNote?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(PostaStatus.MAX_NOTE_LENGTH) },
                    label = { Text(stringResource(R.string.posta_status_note_label)) },
                    placeholder = { Text(stringResource(R.string.posta_status_note_hint)) },
                    singleLine = true,
                    enabled = !locating,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (!locating) confirm() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.posta_location_toggle), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(
                                if (canLocate) R.string.posta_location_toggle_help else R.string.posta_location_no_permission
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = withLocation,
                        onCheckedChange = { withLocation = it },
                        enabled = canLocate && !locating,
                    )
                }
                if (locating) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.posta_location_searching), style = MaterialTheme.typography.bodySmall)
                    }
                }
                locationProblem?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = PostaStatus.Severity.WARNING.color())
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.posta_status_preview_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = status.toWire(note) + if (withLocation) " · 📍 …" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = status.severity.color(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { openSms() }, enabled = !locating) {
                    Text(stringResource(R.string.posta_status_also_sms))
                }
                Text(
                    text = stringResource(R.string.posta_status_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            when {
                locating -> TextButton(onClick = { locateJob?.cancel(); locating = false; sendWith(null) }) {
                    Text(stringResource(R.string.posta_location_send_without))
                }
                locationProblem != null -> TextButton(onClick = { sendWith(null) }) {
                    Text(stringResource(R.string.posta_location_send_without), fontWeight = FontWeight.Bold)
                }
                else -> TextButton(onClick = { confirm() }) {
                    Text(stringResource(R.string.posta_status_send), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { locateJob?.cancel(); onDismiss() }) {
                Text(stringResource(R.string.posta_status_cancel))
            }
        },
    )
}
