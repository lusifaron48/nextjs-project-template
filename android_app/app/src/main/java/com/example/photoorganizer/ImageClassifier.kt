package com.example.photoorganizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val imageSize = 224 // Input size for the model
    private val numChannels = 3 // RGB
    private val numClasses = 5 // Number of categories
    private val categories = arrayOf("People", "Nature", "Documents", "Screenshots", "Other")

    init {
        try {
            val model = loadModelFile()
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val modelPath = "image_classifier.tflite"
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = assetFileDescriptor.createInputStream()
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    sealed class ClassificationResult {
        data class Success(val category: String) : ClassificationResult()
        data class Error(val message: String) : ClassificationResult()
    }

    fun classifyImage(imageFile: File): ClassificationResult {
        try {
            if (!imageFile.exists()) {
                return ClassificationResult.Error("File does not exist")
            }

            // Load and preprocess the image
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: run {
                return ClassificationResult.Error("Failed to decode image")
            }

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                bitmap.recycle()
                return ClassificationResult.Error("Invalid image dimensions")
            }

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
            bitmap.recycle() // Free up the original bitmap memory
            
            // Convert bitmap to ByteBuffer
            val inputBuffer = ByteBuffer.allocateDirect(imageSize * imageSize * numChannels * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            val pixels = IntArray(imageSize * imageSize)
            resizedBitmap.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
            
            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val value = pixels[pixel++]
                    // Normalize pixel values to [-1, 1]
                    inputBuffer.putFloat(((value shr 16 and 0xFF) - 128) / 128.0f)
                    inputBuffer.putFloat(((value shr 8 and 0xFF) - 128) / 128.0f)
                    inputBuffer.putFloat(((value and 0xFF) - 128) / 128.0f)
                }
            }
            
            // Run inference
            val outputBuffer = ByteBuffer.allocateDirect(numClasses * 4)
            outputBuffer.order(ByteOrder.nativeOrder())
            
            interpreter?.run(inputBuffer, outputBuffer)
            
            // Get results
            val outputs = FloatArray(numClasses)
            outputBuffer.rewind()
            for (i in outputs.indices) {
                outputs[i] = outputBuffer.getFloat()
            }
            
            // Find the category with highest probability
            var maxIndex = 0
            var maxValue = outputs[0]
            for (i in 1 until outputs.size) {
                if (outputs[i] > maxValue) {
                    maxIndex = i
                    maxValue = outputs[i]
                }
            }

            // Check confidence threshold
            return if (maxValue > 0.3f) { // Minimum 30% confidence
                ClassificationResult.Success(categories[maxIndex])
            } else {
                ClassificationResult.Error("Low confidence classification")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ImageClassifier", "Classification error", e)
            return ClassificationResult.Error("Classification failed: ${e.message}")
        }
    }

    fun close() {
        interpreter?.close()
    }
}
