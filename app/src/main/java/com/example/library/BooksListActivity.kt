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

        val allBooks = BooksRepository.getAllBooks(assets)
        val filtered = allBooks.filter { isQualityBook(it) }
        val items = if (mode == MODE_BEST) filtered.reversed() else filtered

        val recyclerView = findViewById<RecyclerView>(R.id.listRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = BooksListAdapter(items) { openDetails(it) }

        bindBottomBar()
    }

    private fun openDetails(item: BookItem) {
        val intent = Intent(this, BookDetailActivity::class.java)
        intent.putExtra(BookDetailActivity.EXTRA_ID, item.id)
        intent.putExtra(BookDetailActivity.EXTRA_TITLE, item.title)
        intent.putExtra(BookDetailActivity.EXTRA_TYPE, item.type)
        intent.putExtra(BookDetailActivity.EXTRA_GENRE, item.genre)
        intent.putExtra(BookDetailActivity.EXTRA_THEME, item.theme)
        intent.putExtra(BookDetailActivity.EXTRA_AUTHOR, item.author)
        intent.putExtra(BookDetailActivity.EXTRA_YEAR, item.year)
        intent.putExtra(BookDetailActivity.EXTRA_HEROES, item.heroes)
        intent.putExtra(BookDetailActivity.EXTRA_COVER, item.coverPath)
        startActivity(intent)
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

    private fun isQualityBook(book: BookItem): Boolean {
        return hasCover(book.coverPath)
            && isNormalTitle(book.title)
            && isFilled(book.type)
            && isFilled(book.genre)
            && isFilled(book.theme)
            && isFilled(book.author)
            && isFilled(book.year)
            && isFilled(book.heroes)
    }

    private fun hasCover(coverPath: String): Boolean {
        if (coverPath.isBlank()) return false
        return try {
            assets.open(coverPath).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isNormalTitle(title: String): Boolean {
        val trimmed = title.trim()
        if (trimmed.length < 2) return false
        if (!CYRILLIC_REGEX.containsMatchIn(trimmed)) return false
        if (LATIN_REGEX.containsMatchIn(trimmed)) return false
        return true
    }

    private fun isFilled(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return false
        return !PLACEHOLDERS.contains(trimmed)
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

        private val CYRILLIC_REGEX = Regex("\\p{IsCyrillic}")
        private val LATIN_REGEX = Regex("[A-Za-z]")
        private val PLACEHOLDERS = setOf(
            "-",
            "\u041D\u0435 \u0443\u043A\u0430\u0437\u0430\u043D\u043E",
            "\u041D\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043D\u043E",
            "Unknown",
            "N/A"
        )
    }
}
