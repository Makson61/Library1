package com.example.library

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MyBooksActivity : AppCompatActivity() {
    private val statusAll = "Все"
    private val statusDefault = "Добавить в список"
    private val statusOptions = listOf(
        statusAll,
        "Буду читать",
        "Читаю",
        "Прочитано",
        "Отложено",
        "Брошено"
    )

    private lateinit var adapter: MyBooksAdapter
    private var allBooks: List<BookItem> = emptyList()
    private var currentFilter = statusAll
    private var currentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        if (!isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_my_books)
        hideNavigationBar()

        allBooks = BooksRepository.getAllBooks(assets)

        val recyclerView = findViewById<RecyclerView>(R.id.myBooksRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyBooksAdapter { openDetails(it) }
        recyclerView.adapter = adapter

        bindSearch()
        bindFilter()
        bindBottomBar()

        applyFilters()
    }

    override fun onResume() {
        super.onResume()
        applyFilters()
    }

    private fun bindSearch() {
        val search = findViewById<EditText>(R.id.myBooksSearch)
        val clear = findViewById<ImageView>(R.id.myBooksClear)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim().orEmpty()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        clear.setOnClickListener {
            search.setText("")
        }
    }

    private fun bindFilter() {
        val filter = findViewById<ImageView>(R.id.myBooksFilter)
        filter.setOnClickListener {
            AlertDialog.Builder(this)
                .setItems(statusOptions.toTypedArray()) { _, which ->
                    currentFilter = statusOptions[which]
                    applyFilters()
                }
                .show()
        }
    }

    private fun applyFilters() {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val queryLower = currentQuery.lowercase()
        val filtered = allBooks.filter { book ->
            val status = prefs.getString("status_${book.id}", statusDefault) ?: statusDefault
            if (status == statusDefault) return@filter false
            if (currentFilter != statusAll && status != currentFilter) return@filter false
            if (queryLower.isNotEmpty() && !book.title.lowercase().contains(queryLower)) return@filter false
            true
        }
        adapter.submitList(filtered)
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
        findViewById<View>(R.id.myBooksNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.myBooksNavProfile).setOnClickListener {
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

        val root = findViewById<View>(R.id.myBooksRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
    }
}
