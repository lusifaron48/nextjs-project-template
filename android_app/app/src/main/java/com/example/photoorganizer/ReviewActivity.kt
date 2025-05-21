package com.example.photoorganizer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.File

class ReviewActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var categorySpinner: Spinner
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button
    private lateinit var toolbar: Toolbar
    
    private var imagePath: String? = null
    private var currentCategory: String? = null
    
    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_CURRENT_CATEGORY = "current_category"
        private val CATEGORIES = arrayOf("People", "Nature", "Documents", "Screenshots", "Other")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)
        
        // Get extras
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        currentCategory = intent.getStringExtra(EXTRA_CURRENT_CATEGORY)
        
        if (imagePath == null) {
            Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize views
        imageView = findViewById(R.id.imageView)
        categorySpinner = findViewById(R.id.categorySpinner)
        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)
        toolbar = findViewById(R.id.toolbar)
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Setup spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, CATEGORIES)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        
        // Set current category if available
        currentCategory?.let { category ->
            val position = CATEGORIES.indexOf(category)
            if (position != -1) {
                categorySpinner.setSelection(position)
            }
        }
        
        // Load image
        imageView.setImageBitmap(android.graphics.BitmapFactory.decodeFile(imagePath))
        
        // Setup buttons
        confirmButton.setOnClickListener {
            val newCategory = categorySpinner.selectedItem as String
            if (moveImageToNewCategory(imagePath!!, newCategory)) {
                setResult(RESULT_OK)
                finish()
            }
        }
        
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
    
    private fun moveImageToNewCategory(imagePath: String, newCategory: String): Boolean {
        return try {
            val sourceFile = File(imagePath)
            val categoryDir = File(getExternalFilesDir(null), newCategory)
            
            if (!categoryDir.exists()) {
                categoryDir.mkdirs()
            }
            
            val destinationFile = File(categoryDir, sourceFile.name)
            sourceFile.copyTo(destinationFile, overwrite = true)
            
            Toast.makeText(this, "Image moved to $newCategory", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Error moving image: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
