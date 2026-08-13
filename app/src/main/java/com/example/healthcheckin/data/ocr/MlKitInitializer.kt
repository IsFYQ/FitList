package com.example.healthcheckin.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MlKitInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var prepared = false

    suspend fun ensureChineseModelReady(): Result<Unit> = mutex.withLock {
        if (prepared) return Result.success(Unit)
        withContext(Dispatchers.IO) {
            runCatching {
                val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                val image = InputImage.fromBitmap(bitmap, 0)
                try {
                    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                        recognizer.process(image)
                            .addOnSuccessListener {
                                prepared = true
                                cont.resume(Unit, onCancellation = null)
                            }
                            .addOnFailureListener { cont.resumeWithException(it) }
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }
}
