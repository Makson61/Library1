package com.example.library

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide

class HomeAdapter(
    private val assets: AssetManager,
    private val items: List<GenreItem>,
    private val spanCount: Int,
    private val spacingPx: Int,
    private val onHeaderAction: (Int) -> Unit,
    private val onGenreClick: (GenreItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1

        const val ACTION_SEARCH = 1
        const val ACTION_CLEAR = 2
        const val ACTION_NEW = 3
        const val ACTION_BEST = 4
    }

    private val sizeCache = mutableMapOf<String, Pair<Int, Int>>()
    private var containerWidth = 0

    fun updateContainerWidth(width: Int) {
        if (width != containerWidth) {
            containerWidth = width
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_home_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_genre_tile, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val params = holder.itemView.layoutParams
            if (params is StaggeredGridLayoutManager.LayoutParams) {
                params.isFullSpan = true
            }
            holder.bind(onHeaderAction)
            return
        }

        val item = items[position - 1]
        val itemHolder = holder as ItemViewHolder

        if (containerWidth > 0) {
            val columnWidth = (containerWidth - spacingPx * (spanCount - 1)) / spanCount
            val (w, h) = getAssetSize(item.imagePath)
            if (w > 0 && h > 0) {
                val targetHeight = (columnWidth * (h / w.toFloat())).toInt()
                itemHolder.image.layoutParams = itemHolder.image.layoutParams.apply {
                    height = targetHeight
                }
            }
        }

        itemHolder.name.text = item.name
        itemHolder.itemView.setOnClickListener { onGenreClick(item) }

        val uri = "file:///android_asset/${item.imagePath}"
        Glide.with(itemHolder.image)
            .load(uri)
            .centerCrop()
            .into(itemHolder.image)
    }

    override fun getItemCount(): Int = items.size + 1

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

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val searchContainer: LinearLayout = view.findViewById(R.id.searchContainer)
        private val clearSearch: ImageView = view.findViewById(R.id.clearSearch)
        private val cardNew: LinearLayout = view.findViewById(R.id.cardNew)
        private val cardBest: LinearLayout = view.findViewById(R.id.cardBest)

        fun bind(onHeaderAction: (Int) -> Unit) {
            searchContainer.setOnClickListener { onHeaderAction(ACTION_SEARCH) }
            clearSearch.setOnClickListener { onHeaderAction(ACTION_CLEAR) }
            cardNew.setOnClickListener { onHeaderAction(ACTION_NEW) }
            cardBest.setOnClickListener { onHeaderAction(ACTION_BEST) }
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.genreImage)
        val name: TextView = view.findViewById(R.id.genreName)
    }
}
