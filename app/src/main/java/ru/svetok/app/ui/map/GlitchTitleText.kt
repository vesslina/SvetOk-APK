package ru.svetok.app.ui.map

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

// ── Glitch character pools ────────────────────────────────────
private val BLOCK_CHARS  = "█▓▒░▌▐▀▄■□"
private val CHINESE_CHARS = "漢字亦可你好我是矩阵世界代码京東南北风云雷电火水山川"
private val SYMBOL_CHARS  = "@#\$%^&*!?~|<>{}[]±≠≡"

private fun randomGlitchChar(): Char {
    val roll = (0..99).random()
    return when {
        roll < 50 -> BLOCK_CHARS.random()
        roll < 85 -> CHINESE_CHARS.random()
        else      -> SYMBOL_CHARS.random()
    }
}

/**
 * Matrix-style scanning reveal on first composition,
 * then a periodic glitch sweep every [glitchIntervalMs] ms.
 *
 * Phase 1 – fast scan: scanner █ races left→right with no pause between letters.
 * Phase 2 – idle.
 * Phase 3 – periodic glitch sweep (repeating).
 */
@Composable
fun GlitchTitleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    glitchIntervalMs: Long = 30_000L,
) {
    var displayText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        // ── Phase 1: fast initial reveal ──────────────────────
        val settled = StringBuilder()
        for (i in text.indices) {
            // 6 rapid glitch frames per character, no pause after settling
            repeat(6) { frame ->
                val scanChar = if (frame < 3) '█' else randomGlitchChar()
                val tail = buildString {
                    if (i + 1 < text.length) append(randomGlitchChar())
                    if (i + 2 < text.length) append(randomGlitchChar())
                }
                displayText = settled.toString() + scanChar + tail
                delay(55L)
            }
            // Settle — no extra pause, immediately move to next character
            settled.append(text[i])
            displayText = settled.toString()
        }

        // ── Phase 2 + 3: idle then periodic glitch sweep ──────
        while (true) {
            delay(glitchIntervalMs)

            val chars = text.toCharArray()
            for (i in text.indices) {
                chars[i] = '█'
                if (i + 1 < text.length) chars[i + 1] = randomGlitchChar()
                if (i + 2 < text.length) chars[i + 2] = randomGlitchChar()
                displayText = String(chars)
                delay(50L)

                chars[i] = text[i]
                if (i + 1 < text.length) chars[i + 1] = text[i + 1]
                if (i + 2 < text.length) chars[i + 2] = text[i + 2]
                displayText = String(chars)
                delay(20L)
            }
            displayText = text
        }
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}
