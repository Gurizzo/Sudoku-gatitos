package com.gatitos.sudoku

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate

enum class Pattern { NONE, TABBY, SIAMESE, CALICO, SPOT, TUXEDO }

data class CatStyle(
    val name: String,
    val fur: Color,
    val accent: Color,
    val pattern: Pattern,
    val eyes: Color = Color(0xFF3A2E2A),
    val outline: Boolean = false,
)

val CATS = listOf(
    CatStyle("Naranja", Color(0xFFF5A04A), Color(0xFFD9762B), Pattern.TABBY),
    CatStyle("Gris", Color(0xFFA3ACB8), Color(0xFF7D8794), Pattern.SPOT),
    CatStyle("Negro", Color(0xFF34343E), Color(0xFF34343E), Pattern.NONE, eyes = Color(0xFFE8D44D)),
    CatStyle("Blanco", Color(0xFFFFFDF8), Color(0xFFE6DED3), Pattern.NONE, outline = true),
    CatStyle("Siamés", Color(0xFFEFDFC8), Color(0xFF6B4A35), Pattern.SIAMESE, eyes = Color(0xFF4A8FD8)),
    CatStyle("Calicó", Color(0xFFFFF8EE), Color(0xFFE8893A), Pattern.CALICO, outline = true),
    CatStyle("Chocolate", Color(0xFF8B5A3C), Color(0xFF6A3F27), Pattern.TABBY),
    CatStyle("Rosita", Color(0xFFF4A7C0), Color(0xFFE07FA0), Pattern.SPOT),
    CatStyle("Esmoquin", Color(0xFF3B3B45), Color(0xFFFFFFFF), Pattern.TUXEDO, eyes = Color(0xFF7CCB6B)),
)

/** Dibuja la cara del gatito número [value] (1..9). */
@Composable
fun CatFace(value: Int, modifier: Modifier = Modifier, alpha: Float = 1f, gray: Boolean = false) {
    val style = CATS[value - 1]
    Canvas(modifier) {
        val s = size.minDimension
        if (gray) drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), GrayPaint) }
        translate((size.width - s) / 2, (size.height - s) / 2) {
            drawCat(style, s, alpha)
        }
        if (gray) drawIntoCanvas { it.restore() }
    }
}

/** Pinta el gatito en blanco y negro y semitransparente (gatito mal colocado). */
private val GrayPaint = Paint().apply {
    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    alpha = 0.5f
}

private fun DrawScope.drawCat(st: CatStyle, s: Float, a: Float) {
    val outline = Color(0xFF5A4A42)
    val lineW = s * 0.035f
    val dark = st.fur.luminance() < 0.3f

    fun ear(left: Boolean): Path = Path().apply {
        val m = if (left) 1f else -1f
        val o = if (left) 0f else s
        moveTo(o + m * s * 0.12f, s * 0.50f)
        lineTo(o + m * s * 0.17f, s * 0.08f)
        lineTo(o + m * s * 0.48f, s * 0.30f)
        close()
    }
    fun innerEar(left: Boolean): Path = Path().apply {
        val m = if (left) 1f else -1f
        val o = if (left) 0f else s
        moveTo(o + m * s * 0.19f, s * 0.40f)
        lineTo(o + m * s * 0.21f, s * 0.17f)
        lineTo(o + m * s * 0.38f, s * 0.30f)
        close()
    }

    val earColor = if (st.pattern == Pattern.SIAMESE) st.accent else st.fur
    for (left in listOf(true, false)) {
        drawPath(ear(left), earColor, alpha = a)
        if (st.outline) drawPath(ear(left), outline, alpha = a, style = Stroke(lineW))
        drawPath(innerEar(left), Color(0xFFF7B9C9), alpha = a * if (dark) 0.7f else 1f)
    }
    if (st.pattern == Pattern.CALICO) drawPath(ear(false), Color(0xFF3B3B45), alpha = a)

    // Cabeza
    val headTop = Offset(s * 0.08f, s * 0.24f)
    val headSize = Size(s * 0.84f, s * 0.70f)
    val head = Path().apply { addOval(Rect(headTop, headSize)) }
    drawPath(head, st.fur, alpha = a)

    clipPath(head) {
        when (st.pattern) {
            Pattern.TABBY -> {
                val w = s * 0.055f
                drawLine(st.accent, Offset(s * 0.5f, s * 0.24f), Offset(s * 0.5f, s * 0.38f), w, StrokeCap.Round, alpha = a)
                drawLine(st.accent, Offset(s * 0.40f, s * 0.26f), Offset(s * 0.42f, s * 0.36f), w, StrokeCap.Round, alpha = a)
                drawLine(st.accent, Offset(s * 0.60f, s * 0.26f), Offset(s * 0.58f, s * 0.36f), w, StrokeCap.Round, alpha = a)
                drawLine(st.accent, Offset(s * 0.06f, s * 0.60f), Offset(s * 0.20f, s * 0.62f), w, StrokeCap.Round, alpha = a)
                drawLine(st.accent, Offset(s * 0.94f, s * 0.60f), Offset(s * 0.80f, s * 0.62f), w, StrokeCap.Round, alpha = a)
            }
            Pattern.SIAMESE -> drawOval(st.accent, Offset(s * 0.30f, s * 0.52f), Size(s * 0.40f, s * 0.36f), alpha = a * 0.85f)
            Pattern.CALICO -> {
                drawCircle(st.accent, s * 0.22f, Offset(s * 0.18f, s * 0.36f), alpha = a)
                drawCircle(Color(0xFF3B3B45), s * 0.18f, Offset(s * 0.86f, s * 0.40f), alpha = a)
            }
            Pattern.SPOT -> drawCircle(st.accent, s * 0.13f, Offset(s * 0.68f, s * 0.36f), alpha = a)
            Pattern.TUXEDO -> drawOval(st.accent, Offset(s * 0.28f, s * 0.58f), Size(s * 0.44f, s * 0.45f), alpha = a)
            Pattern.NONE -> Unit
        }
    }
    if (st.outline) drawPath(head, outline, alpha = a, style = Stroke(lineW))

    // Mejillas
    drawCircle(Color(0xFFFF8FAB), s * 0.07f, Offset(s * 0.25f, s * 0.70f), alpha = a * 0.45f)
    drawCircle(Color(0xFFFF8FAB), s * 0.07f, Offset(s * 0.75f, s * 0.70f), alpha = a * 0.45f)

    // Ojos
    for (x in listOf(0.34f, 0.66f)) {
        val c = Offset(s * x, s * 0.56f)
        drawOval(st.eyes, Offset(c.x - s * 0.065f, c.y - s * 0.08f), Size(s * 0.13f, s * 0.16f), alpha = a)
        if (st.eyes != Color(0xFF3A2E2A)) {
            drawOval(Color(0xFF1E1E22), Offset(c.x - s * 0.025f, c.y - s * 0.07f), Size(s * 0.05f, s * 0.14f), alpha = a)
        }
        drawCircle(Color.White, s * 0.025f, Offset(c.x + s * 0.02f, c.y - s * 0.035f), alpha = a)
    }

    // Nariz y boca
    val mouthColor = if (dark && st.pattern != Pattern.TUXEDO) Color(0xFFE8E0DA) else Color(0xFF5A4A42)
    val nose = Path().apply {
        moveTo(s * 0.45f, s * 0.67f)
        lineTo(s * 0.55f, s * 0.67f)
        lineTo(s * 0.5f, s * 0.73f)
        close()
    }
    drawPath(nose, Color(0xFFE86A8A), alpha = a)
    val mw = Stroke(s * 0.03f, cap = StrokeCap.Round)
    drawArc(mouthColor, 0f, 180f, false, Offset(s * 0.40f, s * 0.68f), Size(s * 0.10f, s * 0.09f), alpha = a, style = mw)
    drawArc(mouthColor, 0f, 180f, false, Offset(s * 0.50f, s * 0.68f), Size(s * 0.10f, s * 0.09f), alpha = a, style = mw)

    // Bigotes
    val wc = if (dark) Color(0xFFE8E0DA) else Color(0xFF7A6A62)
    val ww = s * 0.018f
    for (dy in listOf(-0.02f, 0.04f)) {
        drawLine(wc, Offset(s * 0.30f, s * (0.70f + dy)), Offset(s * 0.02f, s * (0.66f + dy * 2)), ww, StrokeCap.Round, alpha = a * 0.8f)
        drawLine(wc, Offset(s * 0.70f, s * (0.70f + dy)), Offset(s * 0.98f, s * (0.66f + dy * 2)), ww, StrokeCap.Round, alpha = a * 0.8f)
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
