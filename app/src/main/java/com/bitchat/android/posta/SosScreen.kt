package com.bitchat.android.posta

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bitchat.android.R

/**
 * Pantalla de señales SOS a pantalla completa.
 *
 * - Linterna en Morse (alcance de cientos de metros de noche).
 * - Pantalla que parpadea en blanco: sirve sin linterna o para que te vean de cerca.
 * - Tono agudo tipo silbato: útil si estás atrapado y no te ven.
 * - Acceso directo al 112.
 * Mantiene la pantalla encendida mientras está abierta.
 */
@Composable
fun SosScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val beacon = remember { SosBeacon(context.applicationContext, scope) }
    val running by beacon.running.collectAsState()
    val on by beacon.signalOn.collectAsState()
    val view = LocalView.current

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            beacon.stop()
            view.keepScreenOn = false
        }
    }

    Dialog(
        onDismissRequest = { beacon.stop(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val background = if (running && on) Color.White else Color(0xFF121212)
        val foreground = if (running && on) Color.Black else Color.White
        Box(
            Modifier
                .fillMaxSize()
                .background(background)
                .safeDrawingPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "SOS",
                    color = if (running && on) Color.Black else Color(0xFFE5484D),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stringResource(R.string.posta_sos_explainer),
                    color = foreground,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(8.dp))

                val big = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                if (running) {
                    Button(
                        onClick = { beacon.stop() },
                        modifier = big,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D), contentColor = Color.White),
                    ) { Text(stringResource(R.string.posta_sos_stop), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                } else {
                    if (beacon.hasTorch) {
                        SosButton(stringResource(R.string.posta_sos_torch), big) {
                            beacon.start(SosBeacon.Mode(torch = true, sound = false))
                        }
                    }
                    SosButton(stringResource(R.string.posta_sos_screen), big) {
                        beacon.start(SosBeacon.Mode(torch = false, sound = false))
                    }
                    SosButton(stringResource(R.string.posta_sos_sound), big) {
                        beacon.start(SosBeacon.Mode(torch = false, sound = true))
                    }
                    SosButton(stringResource(R.string.posta_sos_all), big) {
                        beacon.start(SosBeacon.Mode(torch = beacon.hasTorch, sound = true))
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    modifier = big,
                ) { Text(stringResource(R.string.posta_call_112), color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold) }

                Text(
                    stringResource(R.string.posta_sos_tips),
                    color = foreground.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(onClick = { beacon.stop(); onDismiss() }) {
                    Text(stringResource(R.string.posta_close), color = foreground)
                }
            }
        }
    }
}

@Composable
private fun SosButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A), contentColor = Color.White),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
}
