package com.maxxed.goldestimator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object BitmapPipeline {
    fun decodeSamples(file: File, gridWidth: Int = 96): Pair<List<PixelSample>, IntArray> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sampleSize = generateSequence(1) { it * 2 }
            .takeWhile { bounds.outWidth / it > gridWidth * 2 }
            .lastOrNull() ?: 1
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize }))
        val width = gridWidth.coerceAtMost(bitmap.width)
        val height = (bitmap.height * width.toDouble() / bitmap.width).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        val samples = pixels.mapIndexed { index, color ->
            PixelSample(
                r = color shr 16 and 0xff,
                g = color shr 8 and 0xff,
                b = color and 0xff,
                x = index % width,
                y = index / width
            )
        }
        scaled.recycle()
        return samples to intArrayOf(bounds.outWidth, bounds.outHeight)
    }
}
