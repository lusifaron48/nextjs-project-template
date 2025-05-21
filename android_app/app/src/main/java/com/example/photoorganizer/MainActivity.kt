package com.example.photoorganizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var scanButton: FloatingActionButton
    private lateinit var classifier: ImageClassifier
    private val PERMISSION_REQUEST_CODE = 123
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        recyclerView = findViewById(R.id.categoryRecyclerView)
        scanButton = findViewById(R.id.scanFab)
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        classifier = ImageClassifier(this)
        
        scanButton.setOnClickListener {
            if (checkPermissions()) {
                startScanning()
            } else {
                requestPermissions()
            }
        }
    }
    
    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PERMISSION_REQUEST_CODE
        )
    }
    
    private fun startScanning() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
                scanButton.isEnabled = false
                
                withContext(Dispatchers.IO) {
                    // Scan images from gallery
                    val images = getImagesFromGallery()
                    
                    // Process each image
                    images.forEach { image ->
                        val category = classifier.classifyImage(image)
                        moveImageToCategory(image, category)
                    }
                }
                
                Toast.makeText(
                    this@MainActivity,
                    "Scanning completed!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                findViewById<View>(R.id.progressBar).visibility = View.GONE
                scanButton.isEnabled = true
            }
        }
    }
    
    private fun getImagesFromGallery(): List<File> {
        val images = mutableListOf<File>()
        val projection = arrayOf(
            android.provider.MediaStore.Images.Media.DATA
        )
        
        contentResolver.query(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(columnIndex)
                images.add(File(imagePath))
            }
        }
        
        return images
    }
    
    private fun moveImageToCategory(image: File, category: String) {
        val categoryDir = File(getExternalFilesDir(null), category)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }
        
        val destination = File(categoryDir, image.name)
        image.copyTo(destination, overwrite = true)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startScanning()
            } else {
                Toast.makeText(
                    this,
                    "Permissions are required to scan images",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
