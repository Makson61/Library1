package com.example.library

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HomeAdapter(
    private val assets: AssetManager,
    private val items: List<GenreItem>,
    private val spanCount: Int,
    private val spacingPx: Int,
    private val onGenreClick: (GenreItem) -> Unit
) : RecyclerView.Adapter<HomeAdapter.ItemViewHolder>() {

    private val sizeCache = mutableMapOf<String, Pair<Int, Int>>()
    private var containerWidth = 0

    fun updateContainerWidth(width: Int) {
        if (width != containerWidth) {
            containerWidth = width
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_genre_tile, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        if (containerWidth > 0) {
            val columnWidth = (containerWidth - spacingPx * (spanCount - 1)) / spanCount
            val (w, h) = getAssetSize(item.imagePath)
            if (w > 0 && h > 0) {
                val targetHeight = (columnWidth * (h / w.toFloat())).toInt()
                holder.image.layoutParams = holder.image.layoutParams.apply {
                    height = targetHeight
                }
            }
        }

        holder.name.text = item.name
        holder.itemView.setOnClickListener { onGenreClick(item) }

        val uri = "file:///android_asset/${item.imagePath}"
        Glide.with(holder.image)
            .load(uri)
            .centerCrop()
            .into(holder.image)
    }

    override fun getItemCount(): Int = items.size

    private fun getAssetSize(path: String): Pair<Int, Int> {
        sizeCache[path]?.let { return it }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return try {
            assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val size = options.outWidth to options.outHeight
            sizeCache[path] = size
            size
        } catch (_: Exception) {
            0 to 0
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.genreImage)
        val name: TextView = view.findViewById(R.id.genreName)
    }
}
