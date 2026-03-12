package com.example.library

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide

class BookDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        if (!isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_book_detail)
        hideNavigationBar()

        val id = intent.getStringExtra(EXTRA_ID) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Название книги"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "-"
        val genre = intent.getStringExtra(EXTRA_GENRE) ?: "-"
        val theme = intent.getStringExtra(EXTRA_THEME) ?: "-"
        val author = intent.getStringExtra(EXTRA_AUTHOR) ?: "-"
        val year = intent.getStringExtra(EXTRA_YEAR) ?: "-"
        val heroes = intent.getStringExtra(EXTRA_HEROES) ?: "-"
        val cover = intent.getStringExtra(EXTRA_COVER) ?: ""

        findViewById<TextView>(R.id.detailTitle).text = title
        findViewById<TextView>(R.id.detailType).text = "Тип: $type"
        findViewById<TextView>(R.id.detailGenre).text = "Жанр: $genre"
        findViewById<TextView>(R.id.detailTheme).text = "Тема: $theme"
        findViewById<TextView>(R.id.detailAuthor).text = "Автор: $author"
        findViewById<TextView>(R.id.detailYear).text = "Выпуск: $year"
        findViewById<TextView>(R.id.detailHeroes).text = "Главные герои: $heroes"

        val coverView = findViewById<ImageView>(R.id.detailCover)
        if (cover.isNotBlank()) {
            Glide.with(coverView)
                .load("file:///android_asset/$cover")
                .centerCrop()
                .into(coverView)
        }

        val spinner = findViewById<Spinner>(R.id.detailStatus)
        val options = listOf(
            "Добавить в список",
            "Буду читать",
            "Читаю",
            "Прочитано",
            "Отложено",
            "Брошено"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.adapter = adapter

        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val saved = prefs.getString("status_$id", options.first()) ?: options.first()
        val index = options.indexOf(saved).coerceAtLeast(0)
        spinner.setSelection(index)
        spinner.onItemSelectedListener = SimpleItemSelectedListener { selected ->
            prefs.edit().putString("status_$id", selected).apply()
        }

        bindBottomBar()
    }

    private fun bindBottomBar() {
        findViewById<View>(R.id.detailNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.detailNavMyBooks).setOnClickListener {
            startActivity(Intent(this, MyBooksActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.detailNavProfile).setOnClickListener {
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

        val root = findViewById<View>(R.id.detailRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_GENRE = "extra_genre"
        const val EXTRA_THEME = "extra_theme"
        const val EXTRA_AUTHOR = "extra_author"
        const val EXTRA_YEAR = "extra_year"
        const val EXTRA_HEROES = "extra_heroes"
        const val EXTRA_COVER = "extra_cover"
    }
}
