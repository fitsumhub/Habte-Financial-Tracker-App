package com.mobile.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

enum class NotificationGlyph { UP, DOWN, DOT }

/**
 * Draws a small flat-color circle with a simple directional glyph, used as every
 * notifier's large icon. The status bar's small icon (see ic_notification.xml) is
 * required to be a flat white silhouette, so it can't carry any color — this is what
 * actually lets a transaction notification read as "money in" (green, up) or "money
 * out" (red, down) at a glance in the notification shade, instead of every notification
 * from this app looking identical.
 */
object NotificationIcons {
    private const val SIZE_PX = 128

    fun build(colorArgb: Int, glyph: NotificationGlyph): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = SIZE_PX / 2f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
        canvas.drawCircle(center, center, center, circlePaint)

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        when (glyph) {
            NotificationGlyph.UP -> canvas.drawPath(trianglePath(pointingUp = true), glyphPaint)
            NotificationGlyph.DOWN -> canvas.drawPath(trianglePath(pointingUp = false), glyphPaint)
            NotificationGlyph.DOT -> canvas.drawCircle(center, center, SIZE_PX * 0.16f, glyphPaint)
        }
        return bitmap
    }

    private fun trianglePath(pointingUp: Boolean): Path {
        val inset = SIZE_PX * 0.28f
        val top = inset
        val bottom = SIZE_PX - inset
        val left = inset
        val right = SIZE_PX - inset
        return Path().apply {
            if (pointingUp) {
                moveTo(SIZE_PX / 2f, top)
                lineTo(right, bottom)
                lineTo(left, bottom)
            } else {
                moveTo(SIZE_PX / 2f, bottom)
                lineTo(right, top)
                lineTo(left, top)
            }
            close()
        }
    }
}
