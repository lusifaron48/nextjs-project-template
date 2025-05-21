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

    fun classifyImage(imageFile: File): String {
        try {
            // Load and preprocess the image
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
            
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
            
            return categories[maxIndex]
        } catch (e: Exception) {
            e.printStackTrace()
            return "Other" // Default category in case of error
        }
    }

    fun close() {
        interpreter?.close()
    }
}
