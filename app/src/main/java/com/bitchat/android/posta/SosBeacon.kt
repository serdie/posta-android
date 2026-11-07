package com.bitchat.android.posta

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Baliza SOS: repite ··· ––– ··· con la linterna, con un tono agudo o con ambos.
 *
 * Funciona sin red y sin permisos especiales (la linterna no necesita el
 * permiso de cámara). El estado «encendido» de cada paso se publica para que
 * la pantalla también pueda parpadear a la vez.
 */
class SosBeacon(private val context: Context, private val scope: CoroutineScope) {

    data class Mode(val torch: Boolean, val sound: Boolean)

    private val _signalOn = MutableStateFlow(false)
    /** true mientras el paso actual del Morse está «encendido». */
    val signalOn: StateFlow<Boolean> = _signalOn.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var job: Job? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val torchId: String? = runCatching {
        cameraManager?.cameraIdList?.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    val hasTorch: Boolean get() = torchId != null

    fun start(mode: Mode) {
        stop()
        _running.value = true
        job = scope.launch(Dispatchers.Default) {
            val tone = if (mode.sound) buildTone() else null
            try {
                while (isActive) {
                    for (step in SosPattern.cycle()) {
                        if (!isActive) break
                        _signalOn.value = step.on
                        if (mode.torch) setTorch(step.on)
                        if (tone != null) {
                            if (step.on) tone.play() else tone.pauseAndFlush()
                        }
                        delay(step.durationMs)
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    _signalOn.value = false
                    if (mode.torch) setTorch(false)
                    tone?.release()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _running.value = false
        _signalOn.value = false
        setTorch(false)
    }

    private fun setTorch(on: Boolean) {
        val id = torchId ?: return
        runCatching { cameraManager?.setTorchMode(id, on) }
    }

    /** ~2,8 kHz: la frecuencia de un silbato de emergencia, se oye bien entre escombros. */
    private fun buildTone(): ToneTrack? = runCatching { ToneTrack(2800.0) }.getOrNull()

    private class ToneTrack(frequency: Double) {
        private val sampleRate = 22_050
        private val samples = ShortArray(sampleRate) // 1 s en bucle
        private val track: AudioTrack

        init {
            for (i in samples.indices) {
                samples[i] = (sin(2 * PI * frequency * i / sampleRate) * Short.MAX_VALUE * 0.9).toInt().toShort()
            }
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            track.write(samples, 0, samples.size)
            track.setLoopPoints(0, samples.size, -1)
        }

        fun play() = runCatching { track.play() }
        fun pauseAndFlush() = runCatching {
            track.pause()
            track.setPlaybackHeadPosition(0)
        }
        fun release() = runCatching {
            track.stop()
            track.release()
        }
    }
}
