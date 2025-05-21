package com.example.photoorganizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File

class CategoryAdapter(
    private var categories: List<CategoryItem> = emptyList(),
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val loadingJobs = mutableMapOf<ImageView, Job>()

    data class CategoryItem(
        val name: String,
        val imageCount: Int,
        val directory: File
    ) {
        fun getPreviewImage(): File? {
            if (!directory.exists()) return null
            return directory.listFiles { file -> 
                file.isFile && file.extension.lowercase() in 
                    setOf("jpg", "jpeg", "png", "gif")
            }?.firstOrNull()
        }

        fun getPlaceholderResource(): Int = when (name) {
            "People" -> android.R.drawable.ic_menu_gallery
            "Nature" -> android.R.drawable.ic_menu_gallery
            "Documents" -> android.R.drawable.ic_menu_agenda
            "Screenshots" -> android.R.drawable.ic_menu_crop
            else -> android.R.drawable.ic_menu_gallery
        }
    }

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val previewImage: ImageView = view.findViewById(R.id.previewImage)
        val categoryName: TextView = view.findViewById(R.id.categoryName)
        val imageCount: TextView = view.findViewById(R.id.imageCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categories[position]
        
        holder.categoryName.text = item.name
        holder.imageCount.text = "${item.imageCount} images"
        
        // Cancel any existing loading job for this ImageView
        loadingJobs[holder.previewImage]?.cancel()
        
        // Set placeholder immediately
        holder.previewImage.setImageResource(item.getPlaceholderResource())
        
        // Load preview image
        val previewImage = item.getPreviewImage()
        if (previewImage != null) {
            val job = coroutineScope.launch {
                try {
                    val bitmap = ImageLoader.loadImagePreview(previewImage)
                    if (bitmap != null && isActive) {
                        holder.previewImage.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadingJobs[holder.previewImage] = job
        }
        
        holder.itemView.setOnClickListener { onCategoryClick(item) }
    }

    override fun onViewRecycled(holder: CategoryViewHolder) {
        super.onViewRecycled(holder)
        // Cancel image loading when view is recycled
        loadingJobs[holder.previewImage]?.cancel()
        loadingJobs.remove(holder.previewImage)
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategoryItem>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    fun cleanup() {
        coroutineScope.cancel()
        loadingJobs.clear()
    }
}
