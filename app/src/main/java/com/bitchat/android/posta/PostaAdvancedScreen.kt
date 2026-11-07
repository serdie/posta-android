package com.bitchat.android.posta

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bitchat.android.R
import com.bitchat.android.mesh.PowerManager
import com.bitchat.android.ui.debug.DebugSettingsManager

/** Fila de entrada en Ajustes que abre la pantalla de conectividad avanzada. */
@Composable
fun PostaAdvancedEntry() {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            stringResource(R.string.posta_adv_section).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().clickable { open = true },
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📡", fontSize = 22.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.posta_adv_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.posta_adv_entry_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (open) PostaAdvancedScreen(onDismiss = { open = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostaAdvancedScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings by PostaSettings.values.collectAsState()
    val profile by remember { PowerManager.getInstance(context).profile }.collectAsState()
    val debug = remember { DebugSettingsManager.getInstance() }
    val wifiAware by debug.wifiAwareEnabled.collectAsState()
    val awareSupported = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.posta_adv_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.posta_close)) }
                }
                Text(
                    stringResource(R.string.posta_adv_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // ---------- Avisos que se repiten ----------
                SectionTitle(stringResource(R.string.posta_adv_beacon_title))
                SwitchRow(
                    title = stringResource(R.string.posta_adv_beacon_enable),
                    desc = stringResource(R.string.posta_adv_beacon_desc),
                    checked = settings.beaconEnabled,
                    onChange = { on ->
                        PostaSettings.update { it.copy(beaconEnabled = on) }
                        if (!on) PostaBeacon.stop()
                    },
                )
                if (settings.beaconEnabled) {
                    Label(stringResource(R.string.posta_adv_beacon_interval))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in PostaSettings.BEACON_INTERVALS_MIN) {
                            FilterChip(
                                selected = settings.beaconIntervalMin == m,
                                onClick = { PostaSettings.update { it.copy(beaconIntervalMin = m) } },
                                label = { Text(stringResource(R.string.posta_adv_minutes, m)) },
                            )
                        }
                    }
                    Label(stringResource(R.string.posta_adv_beacon_duration))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (h in PostaSettings.BEACON_DURATIONS_H) {
                            FilterChip(
                                selected = settings.beaconDurationHours == h,
                                onClick = { PostaSettings.update { it.copy(beaconDurationHours = h) } },
                                label = { Text(stringResource(R.string.posta_adv_hours, h)) },
                            )
                        }
                    }
                }
                SwitchRow(
                    title = stringResource(R.string.posta_adv_collapse),
                    desc = stringResource(R.string.posta_adv_collapse_desc),
                    checked = settings.collapseRepeats,
                    onChange = { on -> PostaSettings.update { it.copy(collapseRepeats = on) } },
                )

                HorizontalDivider()

                // ---------- Modo supervivencia ----------
                SectionTitle(stringResource(R.string.posta_adv_survival_title))
                Text(
                    stringResource(R.string.posta_adv_survival_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(
                        if (profile.postaSurvival) R.string.posta_adv_survival_now_on else R.string.posta_adv_survival_now_off
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (profile.postaSurvival) PostaStatus.Severity.OK.color() else MaterialTheme.colorScheme.onSurface,
                )
                RadioRow(
                    stringResource(R.string.posta_adv_survival_auto, settings.survivalThreshold),
                    settings.survivalMode == PostaSettings.SurvivalMode.AUTO,
                ) { PostaSettings.update { it.copy(survivalMode = PostaSettings.SurvivalMode.AUTO) } }
                if (settings.survivalMode == PostaSettings.SurvivalMode.AUTO) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 48.dp),
                    ) {
                        for (t in PostaSettings.SURVIVAL_THRESHOLDS) {
                            FilterChip(
                                selected = settings.survivalThreshold == t,
                                onClick = { PostaSettings.update { it.copy(survivalThreshold = t) } },
                                label = { Text("$t %") },
                            )
                        }
                    }
                }
                RadioRow(
                    stringResource(R.string.posta_adv_survival_always),
                    settings.survivalMode == PostaSettings.SurvivalMode.ALWAYS,
                ) { PostaSettings.update { it.copy(survivalMode = PostaSettings.SurvivalMode.ALWAYS) } }
                RadioRow(
                    stringResource(R.string.posta_adv_survival_off),
                    settings.survivalMode == PostaSettings.SurvivalMode.OFF,
                ) { PostaSettings.update { it.copy(survivalMode = PostaSettings.SurvivalMode.OFF) } }

                HorizontalDivider()

                // ---------- Wi-Fi Aware ----------
                SectionTitle(stringResource(R.string.posta_adv_aware_title))
                SwitchRow(
                    title = stringResource(R.string.posta_adv_aware_enable),
                    desc = stringResource(
                        if (awareSupported) R.string.posta_adv_aware_desc else R.string.posta_adv_aware_unsupported
                    ),
                    checked = wifiAware && awareSupported,
                    enabled = awareSupported,
                    onChange = { on -> debug.setWifiAwareEnabled(on) },
                )

                HorizontalDivider()
                OutlinedButton(
                    onClick = {
                        PostaSettings.update { PostaSettings.Values() }
                        debug.setWifiAwareEnabled(false)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.posta_adv_reset)) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}
