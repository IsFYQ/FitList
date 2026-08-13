package com.example.healthcheckin.data.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.healthcheckin.domain.algorithm.ReceiptLineParser
import com.example.healthcheckin.domain.algorithm.ReceiptParseResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

@Singleton
class ReceiptOcrEngine @Inject constructor() {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeReceipt(bitmap: Bitmap): ReceiptParseResult {
        val lines = recognizeLines(bitmap)
        return ReceiptLineParser.parse(lines)
    }

    suspend fun recognizeLines(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val lines = visionText.textBlocks
                    .flatMap { block -> block.lines.map { it.text.trim() } }
                    .filter { it.isNotBlank() }
                cont.resume(lines)
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    fun decodeAndCompress(bytes: ByteArray, maxLongEdge: Int = 2048): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxLongEdge) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: error("Unable to decode image")
        return scaleToMaxEdge(decoded, maxLongEdge)
    }

    fun compressJpeg(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }

    private fun scaleToMaxEdge(source: Bitmap, maxLongEdge: Int): Bitmap {
        val longEdge = max(source.width, source.height).toFloat()
        if (longEdge <= maxLongEdge) return source
        val scale = maxLongEdge / longEdge
        val targetW = (source.width * scale).toInt().coerceAtLeast(1)
        val targetH = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        if (scaled !== source) source.recycle()
        return scaled
    }
}
