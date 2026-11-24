package com.levelup.gym.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.random.Random

/**
 * Real-world surface materials for the UI: stone for the background, worn steel and leather for panels,
 * forged metal for buttons and frames. Grain comes from a small tiled noise texture made once in code.
 */
object Materials {
    /** Fine grain: grey speckles with some larger blotches, like hammered metal or stone. */
    val grain: ImageBitmap by lazy {
        val n = 160
        val rnd = Random(7)
        val px = IntArray(n * n)
        val blotch = FloatArray(n * n)
        repeat(60) {
            val cx = rnd.nextInt(n); val cy = rnd.nextInt(n); val r = 6 + rnd.nextInt(18)
            val v = rnd.nextFloat() * 0.5f
            for (y in -r..r) for (x in -r..r) {
                val d = (x * x + y * y).toFloat() / (r * r)
                if (d < 1f) {
                    val i = ((cy + y).mod(n)) * n + (cx + x).mod(n)
                    blotch[i] += v * (1f - d)
                }
            }
        }
        for (i in px.indices) {
            val speck = rnd.nextFloat()
            val light = speck > 0.5f
            val a = ((kotlin.math.abs(speck - 0.5f) * 2f) * 60 + blotch[i] * 50).toInt().coerceIn(0, 120)
            val c = if (light) 255 else 0
            px[i] = (a shl 24) or (c shl 16) or (c shl 8) or c
        }
        android.graphics.Bitmap.createBitmap(px, n, n, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    val grainBrush: ShaderBrush by lazy { ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated)) }

    val Ink = Color(0xFF07080B)
    val SteelLight = Color(0xFFC7CCD6)
    val SteelMid = Color(0xFF6E7482)
    val SteelDark = Color(0xFF2A2E37)
    val GoldLight = Color(0xFFFFE8A3)
    val GoldMid = Color(0xFFD6A33A)
    val GoldDark = Color(0xFF6E4A12)
}

/** Stone wall behind every screen: cool dark gradient, grain and a vignette. */
fun Modifier.stoneBackground(): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1E26), Color(0xFF111318), Color(0xFF0A0B0E))))
    drawRect(Materials.grainBrush, alpha = 0.22f)
    drawRect(
        Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            center = Offset(size.width / 2, size.height * 0.4f), radius = size.maxDimension * 0.75f,
        )
    )
}

/** A forged frame: a bevelled metal stroke that is light on top and dark below. */
fun DrawScope.metalFrame(color: Color, radius: Float, width: Float, topLeft: Offset = Offset.Zero, frameSize: Size = size) {
    val light = androidx.compose.ui.graphics.lerp(color, Color.White, 0.45f)
    val dark = androidx.compose.ui.graphics.lerp(color, Color.Black, 0.6f)
    drawRoundRect(
        Brush.verticalGradient(listOf(light, color, dark), topLeft.y, topLeft.y + frameSize.height),
        topLeft + Offset(width / 2, width / 2), Size(frameSize.width - width, frameSize.height - width),
        CornerRadius(radius), style = Stroke(width),
    )
    // inner dark line gives the frame depth
    drawRoundRect(
        Color.Black.copy(alpha = 0.6f), topLeft + Offset(width * 1.5f, width * 1.5f),
        Size(frameSize.width - width * 3, frameSize.height - width * 3), CornerRadius((radius - width).coerceAtLeast(0f)),
        style = Stroke(width * 0.5f),
    )
}

/** A domed rivet with a specular spot. */
fun DrawScope.rivet(c: Offset, r: Float, metal: Color = Materials.SteelMid) {
    drawCircle(Color.Black.copy(alpha = 0.6f), r * 1.15f, c + Offset(r * 0.25f, r * 0.35f))
    drawCircle(
        Brush.radialGradient(
            listOf(androidx.compose.ui.graphics.lerp(metal, Color.White, 0.7f), metal, androidx.compose.ui.graphics.lerp(metal, Color.Black, 0.6f)),
            center = c - Offset(r * 0.35f, r * 0.35f), radius = r * 1.5f,
        ),
        r, c,
    )
}
