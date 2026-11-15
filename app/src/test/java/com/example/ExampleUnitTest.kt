package com.example

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.audio.LyricsParser
import com.example.audio.OrionStereoAudioProcessor
import com.example.ui.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ExampleUnitTest {

    @Test
    fun lyricsParser_parsesSyncedLrcAndFindsActiveLine() {
        val raw = """
            [ti:Starlight]
            [ar:Orion]
            [00:05.50]First line
            [00:12.00][01:02.25]Chorus line
            [00:25.00]Verse two
        """.trimIndent()

        val data = LyricsParser.parseRawLyrics(1L, raw, "Local .lrc File")
        assertTrue(data.isSynced)
        assertEquals(4, data.lines.size)
        assertEquals(5_500L, data.lines[0].timestampMs)
        assertEquals("First line", data.lines[0].text)
        assertEquals(62_250L, data.lines[3].timestampMs)

        assertEquals(-1, LyricsParser.findActiveLineIndex(data.lines, 2_000L))
        assertEquals(0, LyricsParser.findActiveLineIndex(data.lines, 7_000L))
        assertEquals(1, LyricsParser.findActiveLineIndex(data.lines, 15_000L))
        assertEquals(3, LyricsParser.findActiveLineIndex(data.lines, 63_000L))
    }

    @Test
    fun lyricsParser_handlesPlainTextLyrics() {
        val raw = "Line one\nLine two\nLine three"
        val data = LyricsParser.parseRawLyrics(2L, raw, "Manual")
        assertFalse(data.isSynced)
        assertEquals(3, data.lines.size)
        assertEquals("Line two", data.lines[1].text)
    }

    @OptIn(UnstableApi::class)
    @Test
    fun stereoProcessor_appliesMonoAndBalanceCorrectly() {
        val processor = OrionStereoAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        processor.monoAudio = true
        processor.balance = 0f

        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        input.putShort(1000.toShort())
        input.putShort(3000.toShort())
        input.flip()

        processor.queueInput(input)
        val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        val outL = out.short
        val outR = out.short
        assertEquals(2000.toShort(), outL)
        assertEquals(2000.toShort(), outR)

        // Full right balance should silence left channel
        processor.monoAudio = false
        processor.balance = 1.0f
        val input2 = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN)
        input2.putShort(1200.toShort())
        input2.putShort(2400.toShort())
        input2.flip()

        processor.queueInput(input2)
        val out2 = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0.toShort(), out2.short)
        assertEquals(2400.toShort(), out2.short)
    }

    @Test
    fun formatUtils_formatsDurationAndSizesAccurately() {
        assertEquals("3:45", FormatUtils.formatDuration(225_000L))
        assertEquals("1:02:03", FormatUtils.formatDuration(3_723_000L))
        assertEquals("3.6 kHz", FormatUtils.formatFrequency(3600))
        assertEquals("230 Hz", FormatUtils.formatFrequency(230))
    }
}
