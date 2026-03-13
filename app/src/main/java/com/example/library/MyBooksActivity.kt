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
import android.widget.TextView
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
    private val invalidFilters = setOf("-", "Unknown", "N/A", "Не указано", "Неизвестно")

    private lateinit var statusAll: String
    private lateinit var statusDefault: String
    private lateinit var statusOptions: List<String>

    private lateinit var adapter: MyBooksAdapter
    private var allBooks: List<BookItem> = emptyList()
    private var currentStatus = ""
    private var currentGenre = ""
    private var currentYear = ""
    private var currentQuery = ""

    private var genreOptions: List<String> = emptyList()
    private var yearOptions: List<String> = emptyList()

    private lateinit var searchInput: EditText
    private lateinit var searchClear: ImageView
    private lateinit var filterButton: ImageView
    private lateinit var filterStatus: TextView
    private lateinit var filterGenre: TextView
    private lateinit var filterYear: TextView
    private lateinit var filterReset: TextView
    private lateinit var resultsInfo: TextView
    private lateinit var emptyState: View

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

        statusAll = getString(R.string.filter_all)
        statusDefault = getString(R.string.status_default)
        statusOptions = listOf(
            statusAll,
            getString(R.string.status_will_read),
            getString(R.string.status_reading),
            getString(R.string.status_done),
            getString(R.string.status_postponed),
            getString(R.string.status_dropped)
        )

        currentStatus = statusAll
        currentGenre = statusAll
        currentYear = statusAll

        allBooks = BooksRepository.getAllBooks(assets)

        val recyclerView = findViewById<RecyclerView>(R.id.myBooksRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyBooksAdapter(assets) { openDetails(it) }
        recyclerView.adapter = adapter

        bindSearch()
        bindFilters()
        bindBottomBar()

        refreshFilterOptions()
        applyFilters()
    }

    override fun onResume() {
        super.onResume()
        refreshFilterOptions()
        applyFilters()
    }

    private fun bindSearch() {
        searchInput = findViewById(R.id.myBooksSearch)
        searchClear = findViewById(R.id.myBooksClear)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim().orEmpty()
                searchClear.visibility = if (currentQuery.isBlank()) View.INVISIBLE else View.VISIBLE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        searchClear.setOnClickListener { searchInput.setText("") }
        searchClear.visibility = View.INVISIBLE
    }

    private fun bindFilters() {
        filterButton = findViewById(R.id.myBooksFilter)
        filterStatus = findViewById(R.id.myBooksFilterStatus)
        filterGenre = findViewById(R.id.myBooksFilterGenre)
        filterYear = findViewById(R.id.myBooksFilterYear)
        filterReset = findViewById(R.id.myBooksFilterReset)
        resultsInfo = findViewById(R.id.myBooksResultsInfo)
        emptyState = findViewById(R.id.myBooksEmptyState)

        filterStatus.setOnClickListener {
            showChoiceDialog(getString(R.string.filter_status), statusOptions, currentStatus) {
                currentStatus = it
                applyFilters()
            }
        }
        filterGenre.setOnClickListener {
            showChoiceDialog(getString(R.string.filter_genre), genreOptions, currentGenre) {
                currentGenre = it
                applyFilters()
            }
        }
        filterYear.setOnClickListener {
            showChoiceDialog(getString(R.string.filter_year), yearOptions, currentYear) {
                currentYear = it
                applyFilters()
            }
        }
        filterReset.setOnClickListener { resetFilters() }
        filterButton.setOnClickListener { showFilterMenu() }
    }

    private fun refreshFilterOptions() {
        val base = loadMyBooks()
        genreOptions = buildOptions(base.map { it.genre })
        yearOptions = buildYearOptions(base.map { it.year })

        if (!genreOptions.contains(currentGenre)) currentGenre = statusAll
        if (!yearOptions.contains(currentYear)) currentYear = statusAll
    }

    private fun applyFilters() {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val queryLower = currentQuery.trim().lowercase()
        val base = loadMyBooks()

        val filtered = base.filter { book ->
            val status = prefs.getString("status_${book.id}", statusDefault) ?: statusDefault
            if (currentStatus != statusAll && status != currentStatus) return@filter false
            if (currentGenre != statusAll && !book.genre.equals(currentGenre, true)) return@filter false
            if (currentYear != statusAll && book.year.trim() != currentYear) return@filter false

            if (queryLower.isNotEmpty()) {
                val haystack = listOf(
                    book.title,
                    book.author,
                    book.theme,
                    book.genre,
                    book.heroes
                ).joinToString(" ").lowercase()
                if (!haystack.contains(queryLower)) return@filter false
            }
            true
        }

        adapter.submitList(filtered)

        val isFiltering = queryLower.isNotEmpty()
            || currentStatus != statusAll
            || currentGenre != statusAll
            || currentYear != statusAll

        updateFilterChips(isFiltering)

        resultsInfo.text = getString(R.string.results_count, filtered.size)
        resultsInfo.visibility = if (base.isNotEmpty() || isFiltering) View.VISIBLE else View.GONE
        emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateFilterChips(isFiltering: Boolean) {
        filterStatus.text = getString(R.string.filter_chip_status, currentStatus)
        filterGenre.text = getString(R.string.filter_chip_genre, currentGenre)
        filterYear.text = getString(R.string.filter_chip_year, currentYear)

        filterStatus.isSelected = currentStatus != statusAll
        filterGenre.isSelected = currentGenre != statusAll
        filterYear.isSelected = currentYear != statusAll

        filterReset.isSelected = isFiltering
        filterReset.isEnabled = isFiltering
        filterReset.alpha = if (isFiltering) 1f else 0.5f
    }

    private fun resetFilters() {
        currentStatus = statusAll
        currentGenre = statusAll
        currentYear = statusAll
        searchInput.setText("")
        applyFilters()
    }

    private fun showFilterMenu() {
        val options = arrayOf(
            getString(R.string.filter_status),
            getString(R.string.filter_genre),
            getString(R.string.filter_year),
            getString(R.string.filters_reset)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.filters_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> filterStatus.performClick()
                    1 -> filterGenre.performClick()
                    2 -> filterYear.performClick()
                    3 -> resetFilters()
                }
            }
            .show()
    }

    private fun showChoiceDialog(
        title: String,
        options: List<String>,
        current: String,
        onSelected: (String) -> Unit
    ) {
        if (options.isEmpty()) return
        val currentIndex = options.indexOfFirst { it.equals(current, true) }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options.toTypedArray(), currentIndex) { dialog, which ->
                onSelected(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun buildOptions(values: List<String>): List<String> {
        val result = values
            .map { it.trim() }
            .filter { it.isNotBlank() && !invalidFilters.contains(it) }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        return listOf(statusAll) + result
    }

    private fun buildYearOptions(values: List<String>): List<String> {
        val years = values
            .map { it.trim() }
            .mapNotNull { it.toIntOrNull() }
            .distinct()
            .sortedDescending()
            .map { it.toString() }
        return listOf(statusAll) + years
    }

    private fun loadMyBooks(): List<BookItem> {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return allBooks.filter { book ->
            val status = prefs.getString("status_${book.id}", statusDefault) ?: statusDefault
            status != statusDefault
        }
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
