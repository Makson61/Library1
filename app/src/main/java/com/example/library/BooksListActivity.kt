package com.example.library

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BooksListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        if (!isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_books_list)
        hideNavigationBar()

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NEW
        val title = if (mode == MODE_BEST) getString(R.string.best_items) else getString(R.string.new_items)
        findViewById<TextView>(R.id.listTitle).text = title

        val allItems = loadItemsFromAssets()
        val items = if (mode == MODE_BEST) allItems.reversed() else allItems

        val recyclerView = findViewById<RecyclerView>(R.id.listRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = BooksListAdapter(items)

        bindBottomBar()
    }

    private fun loadItemsFromAssets(): List<GenreItem> {
        val files = assets.list("genres")
            ?.filter { it.endsWith(".png", true) || it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) }
            ?: emptyList()
        return files.mapIndexed { index, file ->
            val name = file.substringBeforeLast('.')
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
            GenreItem(index, name, "genres/$file")
        }
    }

    private fun bindBottomBar() {
        findViewById<View>(R.id.listNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.listNavMyBooks).setOnClickListener {
            startActivity(Intent(this, MyBooksActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.listNavProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun isLoggedIn(): Boolean = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        .getBoolean("logged_in", false)

    private fun hideNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())

        val root = findViewById<View>(R.id.listRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_NEW = "new"
        const val MODE_BEST = "best"
    }
}
