package com.example.photoorganizer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
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
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var progressBar: View
    private lateinit var progressText: TextView
    private val PERMISSION_REQUEST_CODE = 123
    
    override fun onDestroy() {
        super.onDestroy()
        categoryAdapter.cleanup()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        recyclerView = findViewById(R.id.categoryRecyclerView)
        scanButton = findViewById(R.id.scanFab)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        classifier = ImageClassifier(this)
        
        categoryAdapter = CategoryAdapter { category ->
            // Handle category click - open folder in gallery
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(category.directory), "image/*")
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = categoryAdapter
        
        // Initial load of categories
        loadCategories()
        
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
    
    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = arrayOf("People", "Nature", "Documents", "Screenshots", "Other")
            val categoryItems = categories.map { categoryName ->
                val dir = File(getExternalFilesDir(null), categoryName)
                val imageCount = if (dir.exists()) {
                    dir.listFiles { file -> 
                        file.isFile && file.extension.lowercase() in 
                            setOf("jpg", "jpeg", "png", "gif")
                    }?.size ?: 0
                } else {
                    0
                }
                CategoryAdapter.CategoryItem(
                    name = categoryName,
                    imageCount = imageCount,
                    directory = dir
                )
            }
            
            withContext(Dispatchers.Main) {
                categoryAdapter.updateCategories(categoryItems)
                emptyView.visibility = if (categoryItems.all { it.imageCount == 0 }) 
                    View.VISIBLE else View.GONE
                recyclerView.visibility = if (categoryItems.all { it.imageCount == 0 }) 
                    View.GONE else View.VISIBLE
            }
        }
    }

    private fun startScanning() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                findViewById<View>(R.id.progressContainer).visibility = View.VISIBLE
                scanButton.isEnabled = false
                
                withContext(Dispatchers.IO) {
                    // Scan images from gallery
                    val images = getImagesFromGallery()
                    var processedCount = 0
                    var successCount = 0
                    var errorCount = 0
                    
                    // Process each image
                    images.forEach { image ->
                        processedCount++
                        withContext(Dispatchers.Main) {
                            progressText.text = "Processing ${processedCount}/${images.size} images..."
                        }
                        
                        when (val result = classifier.classifyImage(image)) {
                            is ImageClassifier.ClassificationResult.Success -> {
                                moveImageToCategory(image, result.category)
                                successCount++
                            }
                            is ImageClassifier.ClassificationResult.Error -> {
                                moveImageToCategory(image, "Other")
                                errorCount++
                                android.util.Log.w("MainActivity", 
                                    "Failed to classify ${image.name}: ${result.message}")
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        val message = buildString {
                            append("Processing complete!\n")
                            append("Successfully classified: $successCount\n")
                            if (errorCount > 0) {
                                append("Failed to classify: $errorCount")
                            }
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    }
                }
                
                // Refresh categories after scanning
                loadCategories()
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                findViewById<View>(R.id.progressContainer).visibility = View.GONE
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
