package com.example.library

import android.os.Bundle
import android.view.View
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.core.view.doOnLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color
import android.os.Build
import androidx.core.view.ViewCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        if (!isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_learn_world)
        hideNavigationBar()
        setupContentList()
        bindBottomBar()
    }

    private fun setupContentList() {
        val recyclerView = findViewById<RecyclerView>(R.id.contentRecycler)
        val spanCount = 3
        val spacingPx = (12 * resources.displayMetrics.density).toInt()

        val layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacingPx))
        recyclerView.setHasFixedSize(false)

        val genres = loadGenresFromAssets()
        val adapter = HomeAdapter(
            assets,
            genres,
            spanCount,
            spacingPx,
            { action ->
                when (action) {
                    HomeAdapter.ACTION_NEW -> openBooksList(BooksListActivity.MODE_NEW)
                    HomeAdapter.ACTION_BEST -> openBooksList(BooksListActivity.MODE_BEST)
                }
            },
            { genre ->
                openGenre(genre)
            }
        )
        recyclerView.adapter = adapter

        recyclerView.doOnLayout {
            val contentWidth = it.width - it.paddingLeft - it.paddingRight
            adapter.updateContainerWidth(contentWidth)
        }
    }

    private fun loadGenresFromAssets(): List<GenreItem> {
        val files = assets.list("genres")
            ?.filter { it.endsWith(".png", true) || it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) }
            ?: emptyList()

        if (files.isEmpty()) {
            return emptyList()
        }

        return files.mapIndexed { index, file ->
            val name = file.substringBeforeLast('.')
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
            GenreItem(
                id = index,
                name = name,
                imagePath = "genres/$file"
            )
        }
    }

    private fun bindBottomBar() {
        findViewById<View>(R.id.navMyBooks).setOnClickListener {
            startActivity(Intent(this, MyBooksActivity::class.java))
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun openBooksList(mode: String) {
        val intent = Intent(this, BooksListActivity::class.java)
        intent.putExtra(BooksListActivity.EXTRA_MODE, mode)
        startActivity(intent)
    }

    private fun openGenre(genre: GenreItem) {
        val intent = Intent(this, GenreBooksActivity::class.java)
        intent.putExtra(GenreBooksActivity.EXTRA_GENRE, genre.name)
        intent.putExtra(GenreBooksActivity.EXTRA_IMAGE, genre.imagePath)
        startActivity(intent)
    }

    private fun hideNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())

        val root = findViewById<View>(R.id.rootLayout)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
    }

    private fun isLoggedIn(): Boolean = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        .getBoolean("logged_in", false)

}




