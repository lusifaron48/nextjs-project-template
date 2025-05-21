package com.example.photoorganizer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.*
import java.io.File

class ReviewActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var categorySpinner: Spinner
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button
    private lateinit var retryButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var confidenceText: TextView
    private lateinit var classifier: ImageClassifier
    
    private var imagePath: String? = null
    private var currentCategory: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
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
        retryButton = findViewById(R.id.retryButton)
        toolbar = findViewById(R.id.toolbar)
        confidenceText = findViewById(R.id.confidenceText)
        
        // Initialize classifier
        classifier = ImageClassifier(this)
        
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
        
        // Load and display image
        loadImage()
        
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
        
        retryButton.setOnClickListener {
            retryClassification()
        }
    }
    
    private fun loadImage() {
        try {
            val imageFile = File(imagePath!!)
            if (!imageFile.exists()) {
                showError("Image file not found")
                return
            }
            
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                showError("Failed to load image")
                return
            }
            
            imageView.setImageBitmap(bitmap)
            classifyImage()
            
        } catch (e: Exception) {
            showError("Error loading image: ${e.message}")
        }
    }
    
    private fun classifyImage() {
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    classifier.classifyImage(File(imagePath!!))
                }
                
                when (result) {
                    is ImageClassifier.ClassificationResult.Success -> {
                        confidenceText.visibility = View.VISIBLE
                        confidenceText.text = "Suggested category: ${result.category}"
                        retryButton.visibility = View.GONE
                        
                        val position = CATEGORIES.indexOf(result.category)
                        if (position != -1) {
                            categorySpinner.setSelection(position)
                        }
                    }
                    is ImageClassifier.ClassificationResult.Error -> {
                        showError("Classification failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                showError("Error during classification: ${e.message}")
            }
        }
    }
    
    private fun retryClassification() {
        confidenceText.visibility = View.GONE
        retryButton.visibility = View.GONE
        classifyImage()
    }
    
    private fun showError(message: String) {
        confidenceText.visibility = View.VISIBLE
        confidenceText.text = message
        retryButton.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        classifier.close()
    }
}
