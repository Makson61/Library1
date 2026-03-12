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

class GenreBooksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        if (!isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_genre_books)
        hideNavigationBar()

        val genre = intent.getStringExtra(EXTRA_GENRE) ?: "Жанр"
        val image = intent.getStringExtra(EXTRA_IMAGE) ?: ""
        findViewById<TextView>(R.id.genreTitle).text = genre

        val items = BooksRepository.getBooksForGenre(genre, image)
        val recyclerView = findViewById<RecyclerView>(R.id.genreRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GenreBooksAdapter(items) { openDetails(it) }

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
        findViewById<View>(R.id.genreNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.genreNavMyBooks).setOnClickListener {
            startActivity(Intent(this, MyBooksActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.genreNavProfile).setOnClickListener {
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

        val root = findViewById<View>(R.id.genreRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_GENRE = "extra_genre"
        const val EXTRA_IMAGE = "extra_image"
    }
}
