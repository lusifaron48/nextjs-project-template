package com.example.photoorganizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class CategoryAdapter(
    private var categories: List<CategoryItem> = emptyList(),
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    data class CategoryItem(
        val name: String,
        val imageCount: Int,
        val directory: File
    )

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(android.R.id.text1)
        val countText: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categories[position]
        holder.nameText.text = item.name
        holder.countText.text = "${item.imageCount} images"
        holder.itemView.setOnClickListener { onCategoryClick(item) }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<CategoryItem>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
