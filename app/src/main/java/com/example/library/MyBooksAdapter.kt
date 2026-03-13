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

class MyBooksAdapter(
    private val assets: AssetManager,
    private val onClick: (BookItem) -> Unit
) : RecyclerView.Adapter<MyBooksAdapter.BookViewHolder>() {

    private val items = mutableListOf<BookItem>()
    private val assetCache = mutableMapOf<String, Boolean>()
    private val bitmapCache = mutableMapOf<String, Bitmap>()
    private val genreCoverCache = mutableMapOf<String, String>()

    fun submitList(newItems: List<BookItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_row, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        val cover = item.coverPath.trim()
        val resolvedCover = resolveCoverPath(cover, item.genre)
        Glide.with(holder.cover).clear(holder.cover)
        if (resolvedCover.isNotBlank() && isGenreCover(resolvedCover)) {
            val bitmap = loadAssetBitmap(resolvedCover)
            if (bitmap != null) {
                holder.cover.setImageBitmap(bitmap)
            } else {
                holder.cover.setImageResource(R.drawable.ic_books)
            }
        } else if (resolvedCover.isNotBlank()) {
            val model = buildModel(resolvedCover)
            Glide.with(holder.cover)
                .load(model)
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

    private fun resolveCoverPath(bookCoverPath: String, genre: String): String {
        if (bookCoverPath.isBlank()) return getGenreCoverPath(genre).orEmpty()
        if (bookCoverPath.startsWith("http", true)) return bookCoverPath
        if (bookCoverPath.startsWith("books/covers/", true) && assetExists(bookCoverPath)) {
            return bookCoverPath
        }
        return getGenreCoverPath(genre).orEmpty()
    }

    private fun isGenreCover(path: String): Boolean {
        return path.startsWith("genres/", true)
    }

    private fun getGenreCoverPath(genre: String): String? {
        val key = genre.trim()
        if (key.isBlank()) return null
        genreCoverCache[key]?.let { return it }
        val files = assets.list("genres") ?: return null
        val match = files.firstOrNull { file ->
            val name = file.substringBeforeLast('.').replace('_', ' ')
            name.equals(key, ignoreCase = true)
        }
        val result = match?.let { "genres/$it" }
        if (result != null) {
            genreCoverCache[key] = result
        }
        return result
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
