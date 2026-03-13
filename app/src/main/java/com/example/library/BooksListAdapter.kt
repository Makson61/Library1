package com.example.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BooksListAdapter(
    private val items: List<BookItem>,
    private val onClick: (BookItem) -> Unit
) : RecyclerView.Adapter<BooksListAdapter.BookViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_row, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        val cover = item.coverPath
        if (cover.isNotBlank()) {
            val model = if (cover.startsWith("http", true)) cover else "file:///android_asset/$cover"
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
}
