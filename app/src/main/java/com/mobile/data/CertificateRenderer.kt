package com.mobile.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/** Everything CertificateRenderer needs to draw one certificate — see computeCertificateAchievement for how the achievement fields are derived from real transaction data. */
data class CertificateContent(
    val recipientName: String,
    val photo: Bitmap?,
    val period: CertificatePeriod,
    val periodLabel: String,
    val achievementTitle: String,
    val achievementSubtitle: String,
    val dateLabel: String,
    val template: CertificateTemplate = CertificateTemplate.CLASSIC_GOLD
)

/** Resolved colors for one CertificateTemplate — everything else about the layout is shared. */
private data class Palette(
    val bgColors: IntArray,
    val bgStops: FloatArray,
    val accent: Int,
    val accentSoft: Int,
    val cream: Int,
    val muted: Int
)

/**
 * Draws a certificate straight onto a Bitmap with android.graphics (Canvas/Paint), rather than
 * capturing a Composable — there's no reliable, headless way to rasterize an arbitrary
 * Composable to a Bitmap outside of a measured/attached View, whereas Canvas drawing works
 * identically whether or not anything is on screen, which is exactly what "generate a
 * certificate for a photo + name that were just picked, then immediately download/share it"
 * requires. The live preview in AchievementCertificatesScreen renders this same Bitmap via
 * Image(bitmap = ...) rather than a separate Compose layout, so preview and exported file can
 * never visually drift apart.
 *
 * Three templates share this exact same layout (header, heading, photo, name, badge, subtitle,
 * period stat, footer) and differ only in [Palette] plus a template-specific background/frame
 * treatment — Ethiopian Heritage adds a tricolor interlocking-diamond tibeb border, Birr
 * Banknote adds a guilloché rosette pattern and a faint "ETB" watermark, evoking a real
 * banknote. All three share a vignette background, drop-shadowed headline text, ornamental
 * flourish corners, and a sunburst wax-seal footer for a consistent "engraved" feel.
 */
object CertificateRenderer {
    private const val WIDTH = 1200
    private const val HEIGHT = 1600
    private const val CENTER_X = WIDTH / 2f

    private val ETHIOPIA_GREEN = Color.parseColor("#078930")
    private val ETHIOPIA_YELLOW = Color.parseColor("#FCDD09")
    private val ETHIOPIA_RED = Color.parseColor("#DA121A")

    private fun paletteFor(template: CertificateTemplate): Palette = when (template) {
        CertificateTemplate.CLASSIC_GOLD -> Palette(
            bgColors = intArrayOf(Color.parseColor("#3730A3"), Color.parseColor("#1E1B4B"), Color.parseColor("#0B0A1F")),
            bgStops = floatArrayOf(0f, 0.55f, 1f),
            accent = Color.parseColor("#FBBF24"),
            accentSoft = Color.parseColor("#80FBBF24"),
            cream = Color.parseColor("#F5F3FF"),
            muted = Color.parseColor("#C7C4E8")
        )
        CertificateTemplate.ETHIOPIAN_HERITAGE -> Palette(
            bgColors = intArrayOf(Color.parseColor("#0E4A2C"), Color.parseColor("#0A2E1B"), Color.parseColor("#05130B")),
            bgStops = floatArrayOf(0f, 0.55f, 1f),
            accent = ETHIOPIA_YELLOW,
            accentSoft = Color.parseColor("#80FCDD09"),
            cream = Color.parseColor("#FDF6E3"),
            muted = Color.parseColor("#BFDCC7")
        )
        CertificateTemplate.BIRR_BANKNOTE -> Palette(
            bgColors = intArrayOf(Color.parseColor("#0D5A43"), Color.parseColor("#073024"), Color.parseColor("#020F0A")),
            bgStops = floatArrayOf(0f, 0.55f, 1f),
            accent = Color.parseColor("#EFC55E"),
            accentSoft = Color.parseColor("#80EFC55E"),
            cream = Color.parseColor("#F3ECD8"),
            muted = Color.parseColor("#BFD6CA")
        )
    }

    fun render(content: CertificateContent): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(content.template)

        drawBackground(canvas, content.template, palette)
        drawFrame(canvas, content.template, palette)
        drawHeader(canvas, palette)
        var y = drawHeading(canvas, palette)
        y = drawPhoto(canvas, content.photo, content.recipientName, palette, y)
        y = drawRecipientName(canvas, content.recipientName, palette, y)
        y = drawAchievementBadge(canvas, content.achievementTitle, palette, y)
        y = drawSubtitle(canvas, content.achievementSubtitle, palette, y)
        drawPeriodStat(canvas, content.period, content.periodLabel, palette, y)
        drawFooter(canvas, content.dateLabel, palette)

        return bitmap
    }

    private fun drawBackground(canvas: Canvas, template: CertificateTemplate, palette: Palette) {
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                palette.bgColors, palette.bgStops, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), gradientPaint)

        if (template == CertificateTemplate.BIRR_BANKNOTE) {
            drawGuillochePattern(canvas, palette)
            drawWatermark(canvas, "ETB", palette)
        }

        // Subtle vignette — a soft radial darkening toward the corners is what separates a flat
        // color fill from something that reads as printed/engraved with real depth.
        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                CENTER_X, HEIGHT * 0.42f, HEIGHT * 0.85f,
                intArrayOf(Color.TRANSPARENT, Color.argb(110, 0, 0, 0)),
                floatArrayOf(0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), vignettePaint)
    }

    /** Concentric rosette rings with radiating spokes — a proper guilloché "engraving," not just plain circles, anchored in two corners so it reads as a security pattern rather than noise. */
    private fun drawGuillochePattern(canvas: Canvas, palette: Palette) {
        val ringColor = Color.argb(30, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent))
        val spokeColor = Color.argb(18, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent))
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; color = ringColor }
        val spokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = spokeColor }

        listOf(190f to 230f, (WIDTH - 190f) to (HEIGHT - 270f)).forEach { (cx, cy) ->
            var r = 20f
            while (r < 280f) {
                canvas.drawCircle(cx, cy, r, ringPaint)
                r += 13f
            }
            val spokeCount = 24
            for (i in 0 until spokeCount) {
                val angle = Math.toRadians((360.0 / spokeCount) * i)
                val x = cx + (280f * Math.cos(angle)).toFloat()
                val y = cy + (280f * Math.sin(angle)).toFloat()
                canvas.drawLine(cx, cy, x, y, spokePaint)
            }
        }
    }

    /** A large, faint, rotated brand mark drawn behind everything else — the "hold up to the light" watermark banknotes use. */
    private fun drawWatermark(canvas: Canvas, text: String, palette: Palette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(22, Color.red(palette.cream), Color.green(palette.cream), Color.blue(palette.cream))
            textSize = 420f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.save()
        canvas.rotate(-18f, CENTER_X, HEIGHT / 2f)
        canvas.drawText(text, CENTER_X, HEIGHT / 2f + 140f, paint)
        canvas.restore()
    }

    private fun drawFrame(canvas: Canvas, template: CertificateTemplate, palette: Palette) {
        val outer = 44f
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = palette.accent
            setShadowLayer(10f, 0f, 0f, Color.argb(90, 0, 0, 0))
        }
        canvas.drawRoundRect(RectF(outer, outer, WIDTH - outer, HEIGHT - outer), 28f, 28f, outerPaint)

        val inner = outer + 18f
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = palette.accentSoft
        }
        canvas.drawRoundRect(RectF(inner, inner, WIDTH - inner, HEIGHT - inner), 20f, 20f, innerPaint)

        // A thin third hairline just inside the double frame — the extra bit of "engraving"
        // that separates an ornate border from two plain rectangles.
        val hairline = inner + 10f
        val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.argb(90, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent))
        }
        canvas.drawRoundRect(RectF(hairline, hairline, WIDTH - hairline, HEIGHT - hairline), 14f, 14f, hairlinePaint)

        when (template) {
            CertificateTemplate.ETHIOPIAN_HERITAGE -> drawTibebBands(canvas, inner)
            CertificateTemplate.BIRR_BANKNOTE -> drawCornerRosettes(canvas, inner, palette)
            CertificateTemplate.CLASSIC_GOLD -> drawCornerFlourishes(canvas, inner, palette)
        }
    }

    /** An "L"-shaped ornamental flourish with a curled tail in each corner — the classic engraved-certificate corner motif, built from two mirrored bezier curls rather than a plain shape. */
    private fun drawCornerFlourishes(canvas: Canvas, inner: Float, palette: Palette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            color = palette.accent
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        val len = 76f
        val margin = inner + 26f

        listOf(
            Triple(margin, margin, 1f) to 1f,
            Triple(WIDTH - margin, margin, -1f) to 1f,
            Triple(margin, HEIGHT - margin, 1f) to -1f,
            Triple(WIDTH - margin, HEIGHT - margin, -1f) to -1f
        ).forEach { (corner, signY) ->
            val (cx, cy, signX) = corner
            val path = Path().apply {
                moveTo(cx, cy + signY * len)
                quadTo(cx, cy, cx + signX * len, cy)
            }
            canvas.drawPath(path, paint)
            val innerPath = Path().apply {
                moveTo(cx + signX * 14f, cy + signY * len * 0.72f)
                quadTo(cx + signX * 14f, cy + signY * 14f, cx + signX * len * 0.72f, cy + signY * 14f)
            }
            canvas.drawPath(innerPath, paint)
            canvas.drawCircle(cx + signX * len, cy, 5f, dotPaint)
            canvas.drawCircle(cx, cy + signY * len, 5f, dotPaint)
        }
    }

    private fun drawCornerRosettes(canvas: Canvas, inner: Float, palette: Palette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = palette.accent
        }
        val spokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.argb(160, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent))
        }
        listOf(
            inner to inner, (WIDTH - inner) to inner,
            inner to (HEIGHT - inner), (WIDTH - inner) to (HEIGHT - inner)
        ).forEach { (cx, cy) ->
            canvas.drawCircle(cx, cy, 11f, paint)
            canvas.drawCircle(cx, cy, 22f, paint)
            canvas.drawCircle(cx, cy, 32f, paint)
            val spokes = 12
            for (i in 0 until spokes) {
                val angle = Math.toRadians((360.0 / spokes) * i)
                val x1 = cx + (22f * Math.cos(angle)).toFloat()
                val y1 = cy + (22f * Math.sin(angle)).toFloat()
                val x2 = cx + (32f * Math.cos(angle)).toFloat()
                val y2 = cy + (32f * Math.sin(angle)).toFloat()
                canvas.drawLine(x1, y1, x2, y2, spokePaint)
            }
        }
    }

    /**
     * A tricolor interlocking-diamond band along the top and bottom inner edges — closer to an
     * actual tibeb (the woven trim on traditional Ethiopian dress) than a row of plain
     * triangles: alternating up/down triangles that interlock into a continuous diamond chain,
     * outlined in a dark keyline so each diamond reads as a distinct woven motif.
     */
    private fun drawTibebBands(canvas: Canvas, inner: Float) {
        val triSize = 26f
        val triHeight = triSize * 0.86f
        val colors = listOf(ETHIOPIA_GREEN, ETHIOPIA_YELLOW, ETHIOPIA_RED)
        val margin = inner + 28f
        val keylinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.argb(140, 0, 0, 0)
        }
        val count = ((WIDTH - 2 * margin) / triSize).toInt()

        listOf(margin + 12f, HEIGHT - margin - 12f).forEachIndexed { bandIndex, bandY ->
            val pointsDown = bandIndex == 0
            for (i in 0 until count) {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors[i % colors.size] }
                val x = margin + i * triSize
                val path = Path().apply {
                    if (pointsDown) {
                        moveTo(x, bandY - triHeight / 2f)
                        lineTo(x + triSize, bandY - triHeight / 2f)
                        lineTo(x + triSize / 2f, bandY + triHeight / 2f)
                    } else {
                        moveTo(x, bandY + triHeight / 2f)
                        lineTo(x + triSize, bandY + triHeight / 2f)
                        lineTo(x + triSize / 2f, bandY - triHeight / 2f)
                    }
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, keylinePaint)
            }
        }
    }

    private fun drawHeader(canvas: Canvas, palette: Palette) {
        val wordmarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            letterSpacing = 0.14f
            setShadowLayer(6f, 0f, 3f, Color.argb(130, 0, 0, 0))
        }
        canvas.drawText("HABTE", CENTER_X, 150f, wordmarkPaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            textSize = 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            letterSpacing = 0.3f
        }
        canvas.drawText("FINANCIAL TRACKER", CENTER_X, 186f, taglinePaint)
    }

    /** Returns the y-coordinate to continue drawing from. */
    private fun drawHeading(canvas: Canvas, palette: Palette): Float {
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.cream
            textSize = 58f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            setShadowLayer(8f, 0f, 4f, Color.argb(150, 0, 0, 0))
        }
        val y = 300f
        canvas.drawText("CERTIFICATE", CENTER_X, y, headingPaint)
        canvas.drawText("OF ACHIEVEMENT", CENTER_X, y + 64f, headingPaint)

        // Ornamental divider — a center diamond flanked by two lines, instead of one bare line.
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            strokeWidth = 2.5f
        }
        val dividerY = y + 100f
        canvas.drawLine(CENTER_X - 110f, dividerY, CENTER_X - 14f, dividerY, dividerPaint)
        canvas.drawLine(CENTER_X + 14f, dividerY, CENTER_X + 110f, dividerY, dividerPaint)
        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        val diamondPath = Path().apply {
            moveTo(CENTER_X, dividerY - 9f); lineTo(CENTER_X + 9f, dividerY)
            lineTo(CENTER_X, dividerY + 9f); lineTo(CENTER_X - 9f, dividerY); close()
        }
        canvas.drawPath(diamondPath, diamondPaint)

        val certifiesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.muted
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("This certifies that", CENTER_X, y + 150f, certifiesPaint)
        return y + 150f
    }

    private fun drawPhoto(canvas: Canvas, photo: Bitmap?, recipientName: String, palette: Palette, startY: Float): Float {
        val radius = 130f
        val cy = startY + 60f + radius
        val rect = RectF(CENTER_X - radius, cy - radius, CENTER_X + radius, cy + radius)

        // Soft glow behind the photo — drawn before the clip/photo itself so it only shows
        // past the ring, giving the medallion a sense of light rather than a flat cutout.
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                CENTER_X, cy, radius + 34f,
                intArrayOf(Color.argb(70, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent)), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(CENTER_X, cy, radius + 34f, glowPaint)

        canvas.save()
        val clipPath = Path().apply { addOval(rect, Path.Direction.CW) }
        canvas.clipPath(clipPath)
        if (photo != null) {
            val src = centerCropRect(photo.width, photo.height)
            canvas.drawBitmap(photo, src, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            val avatarPaint = Paint().apply {
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    Color.parseColor("#818CF8"), Color.parseColor("#4C1D95"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(rect, avatarPaint)
            val initials = initialsFor(recipientName)
            val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = radius
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            }
            val metrics = initialsPaint.fontMetrics
            val textY = cy - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(initials, CENTER_X, textY, initialsPaint)
        }
        canvas.restore()

        // Double ring — thin accent line just outside a thicker main ring — reads as a proper
        // medallion frame rather than a single flat stroke.
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 7f
            color = palette.accent
        }
        canvas.drawOval(rect, ringPaint)
        val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = palette.accentSoft
        }
        canvas.drawOval(RectF(rect.left - 10f, rect.top - 10f, rect.right + 10f, rect.bottom + 10f), outerRingPaint)

        return cy + radius
    }

    private fun drawRecipientName(canvas: Canvas, name: String, palette: Palette, startY: Float): Float {
        val y = startY + 90f
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            textSize = 54f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            setShadowLayer(7f, 0f, 3f, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(name, CENTER_X, y, namePaint)

        val halfWidth = (namePaint.measureText(name) / 2f).coerceAtMost(360f)
        val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentSoft
            strokeWidth = 2f
        }
        canvas.drawLine(CENTER_X - halfWidth, y + 18f, CENTER_X + halfWidth, y + 18f, underlinePaint)
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        canvas.drawCircle(CENTER_X - halfWidth, y + 18f, 3.5f, tickPaint)
        canvas.drawCircle(CENTER_X + halfWidth, y + 18f, 3.5f, tickPaint)

        return y + 18f
    }

    private fun drawAchievementBadge(canvas: Canvas, title: String, palette: Palette, startY: Float): Float {
        val y = startY + 80f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val textWidth = titlePaint.measureText(title)
        val badgeRect = RectF(CENTER_X - textWidth / 2f - 40f, y - 54f, CENTER_X + textWidth / 2f + 40f, y + 20f)

        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                badgeRect.left, badgeRect.top, badgeRect.left, badgeRect.bottom,
                Color.argb(70, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent)),
                Color.argb(28, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent)),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(badgeRect, 44f, 44f, badgeFill)
        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = palette.accent
        }
        canvas.drawRoundRect(badgeRect, 44f, 44f, badgeBorder)

        // Small flanking flourish ticks either side of the badge — a light "ribbon end" cue.
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = palette.accent
        }
        canvas.drawLine(badgeRect.left - 22f, y - 16f, badgeRect.left - 6f, y - 16f, tickPaint)
        canvas.drawLine(badgeRect.right + 6f, y - 16f, badgeRect.right + 22f, y - 16f, tickPaint)

        canvas.drawText(title, CENTER_X, y, titlePaint)
        return y + 20f
    }

    private fun drawSubtitle(canvas: Canvas, subtitle: String, palette: Palette, startY: Float): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.muted
            textSize = 27f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        val lines = wrapText(subtitle, paint, WIDTH - 260f)
        var y = startY + 60f
        lines.forEach { line ->
            canvas.drawText(line, CENTER_X, y, paint)
            y += 38f
        }
        return y
    }

    private fun drawPeriodStat(canvas: Canvas, period: CertificatePeriod, periodLabel: String, palette: Palette, startY: Float) {
        val y = startY + 30f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val text = "${period.label.uppercase()} ACHIEVEMENT · ${periodLabel.uppercase()}"
        canvas.drawText(text, CENTER_X, y, paint)

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accentSoft; strokeWidth = 1.5f }
        val halfWidth = paint.measureText(text) / 2f + 24f
        canvas.drawLine(CENTER_X - halfWidth - 30f, y - 8f, CENTER_X - halfWidth, y - 8f, tickPaint)
        canvas.drawLine(CENTER_X + halfWidth, y - 8f, CENTER_X + halfWidth + 30f, y - 8f, tickPaint)
    }

    private fun drawFooter(canvas: Canvas, dateLabel: String, palette: Palette) {
        val dividerY = HEIGHT - 170f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accentSoft; strokeWidth = 2f }
        canvas.drawLine(90f, dividerY, WIDTH - 90f, dividerY, dividerPaint)

        drawSunburstSeal(canvas, WIDTH - 170f, dividerY + 70f, palette)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.muted
            textSize = 22f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("Generated on", 90f, dividerY + 50f, labelPaint)
        val dateValuePaint = Paint(labelPaint).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText(dateLabel, 90f, dividerY + 80f, dateValuePaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.muted
            textSize = 20f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Habte Financial Tracker", 90f, HEIGHT - 60f, brandPaint)
    }

    /** A wax-seal-style medallion — radiating sunburst rays behind a double ring and checkmark — instead of a bare circle+check, so the "approval stamp" actually reads as official. */
    private fun drawSunburstSeal(canvas: Canvas, cx: Float, cy: Float, palette: Palette) {
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(160, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent))
        }
        val rays = 16
        val innerR = 50f
        val outerR = 64f
        for (i in 0 until rays) {
            val angle = Math.toRadians((360.0 / rays) * i)
            val x1 = cx + (innerR * Math.cos(angle)).toFloat()
            val y1 = cy + (innerR * Math.sin(angle)).toFloat()
            val x2 = cx + (outerR * Math.cos(angle)).toFloat()
            val y2 = cy + (outerR * Math.sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, rayPaint)
        }

        val sealRadius = 46f
        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = palette.accent
        }
        canvas.drawCircle(cx, cy, sealRadius, sealPaint)
        canvas.drawCircle(cx, cy, sealRadius - 10f, sealPaint)
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            color = palette.accent
        }
        val checkPath = Path().apply {
            moveTo(cx - 16f, cy)
            lineTo(cx - 4f, cy + 14f)
            lineTo(cx + 18f, cy - 14f)
        }
        canvas.drawPath(checkPath, checkPaint)
    }

    private fun centerCropRect(width: Int, height: Int): Rect {
        val size = minOf(width, height)
        val left = (width - size) / 2
        val top = (height - size) / 2
        return Rect(left, top, left + size, top + size)
    }

    private fun initialsFor(name: String): String =
        name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .take(2).map { it.first().uppercaseChar() }.joinToString("").ifBlank { "?" }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
