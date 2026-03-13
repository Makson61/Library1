package com.example.library

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GenreBooksAdapter(
    private val assets: AssetManager,
    private val items: List<BookItem>,
    private val genreCoverPath: String,
    private val onClick: (BookItem) -> Unit
) : RecyclerView.Adapter<GenreBooksAdapter.BookViewHolder>() {

    private val assetCache = mutableMapOf<String, Boolean>()
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_row, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        val cover = item.coverPath
        val resolvedCover = resolveCoverPath(cover)
        val fallbackModel = buildModel(genreCoverPath)
        Glide.with(holder.cover).clear(holder.cover)
        if (resolvedCover.isNotBlank()) {
            val model = buildModel(resolvedCover)
            if (resolvedCover == genreCoverPath) {
                val bitmap = loadAssetBitmap(genreCoverPath)
                if (bitmap != null) {
                    holder.cover.setImageBitmap(bitmap)
                } else {
                    Glide.with(holder.cover)
                        .load(model)
                        .centerCrop()
                        .placeholder(R.drawable.ic_books)
                        .error(R.drawable.ic_books)
                        .into(holder.cover)
                }
            } else {
                val request = Glide.with(holder.cover)
                    .load(model)
                    .centerCrop()
                    .placeholder(R.drawable.ic_books)
                if (fallbackModel != null) {
                    request.error(
                        Glide.with(holder.cover)
                            .load(fallbackModel)
                            .centerCrop()
                    )
                } else {
                    request.error(R.drawable.ic_books)
                }
                request.into(holder.cover)
            }
        } else if (fallbackModel != null) {
            Glide.with(holder.cover)
                .load(fallbackModel)
                .centerCrop()
                .placeholder(R.drawable.ic_books)
                .error(R.drawable.ic_books)
                .into(holder.cover)
        } else {
            holder.cover.setImageResource(R.drawable.ic_books)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.bookCover)
        val title: TextView = view.findViewById(R.id.bookTitle)
    }

    private fun resolveCoverPath(bookCoverPath: String): String {
        val trimmed = bookCoverPath.trim()
        if (trimmed.isBlank()) return genreCoverPath
        if (trimmed.startsWith("http", true)) return trimmed
        if (trimmed.startsWith("books/covers/", true) && assetExists(trimmed)) return trimmed
        return genreCoverPath
    }

    private fun assetExists(path: String): Boolean {
        assetCache[path]?.let { return it }
        val exists = runCatching { assets.open(path).close() }.isSuccess
        assetCache[path] = exists
        return exists
    }

    private fun loadAssetBitmap(path: String): Bitmap? {
        bitmapCache[path]?.let { return it }
        val bitmap = runCatching {
            assets.open(path).use { stream -> BitmapFactory.decodeStream(stream) }
        }.getOrNull()
        if (bitmap != null) {
            bitmapCache[path] = bitmap
        }
        return bitmap
    }

    private fun buildModel(path: String): String? {
        if (path.isBlank()) return null
        if (path.startsWith("http", true)) return path
        val encoded = Uri.encode(path, "/")
        return "file:///android_asset/$encoded"
    }
}
