package com.example.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class OrionStereoAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var balance: Float = 0f // -1.0f (left) .. 0.0f (center) .. +1.0f (right)

    @Volatile
    var monoAudio: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val bal = balance.coerceIn(-1f, 1f)
        val mono = monoAudio

        // Fast pass-through if centered stereo
        if (!mono && bal == 0f) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        val leftGain = if (bal > 0f) (1f - bal) else 1f
        val rightGain = if (bal < 0f) (1f + bal) else 1f

        val out = replaceOutputBuffer(remaining)
        while (inputBuffer.remaining() >= 4) {
            val leftSample = inputBuffer.short.toFloat()
            val rightSample = inputBuffer.short.toFloat()

            val (outL, outR) = if (mono) {
                val mixed = (leftSample + rightSample) * 0.5f
                (mixed * leftGain) to (mixed * rightGain)
            } else {
                (leftSample * leftGain) to (rightSample * rightGain)
            }

            out.putShort(outL.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            out.putShort(outR.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }
}
