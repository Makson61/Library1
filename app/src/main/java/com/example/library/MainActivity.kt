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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class MainActivity : AppCompatActivity() {
    private val invalidFilters = setOf("-", "Unknown", "N/A", "Не указано", "Неизвестно")

    private lateinit var homeRecycler: RecyclerView
    private lateinit var genresAdapter: HomeAdapter
    private lateinit var searchAdapter: MyBooksAdapter
    private lateinit var gridLayoutManager: StaggeredGridLayoutManager
    private lateinit var listLayoutManager: LinearLayoutManager
    private lateinit var gridDecoration: GridSpacingItemDecoration
    private var gridDecorationAdded = false

    private lateinit var homeSearchInput: EditText
    private lateinit var homeSearchClear: ImageView
    private lateinit var homeFilterButton: ImageView
    private lateinit var homeFilterGenre: TextView
    private lateinit var homeFilterType: TextView
    private lateinit var homeFilterYear: TextView
    private lateinit var homeFilterReset: TextView
    private lateinit var homeResultsInfo: TextView
    private lateinit var homeSectionTitle: TextView
    private lateinit var homeEmptyState: View
    private lateinit var homeCardsRow: LinearLayout
    private lateinit var homeCardNew: LinearLayout
    private lateinit var homeCardBest: LinearLayout

    private var allBooks: List<BookItem> = emptyList()
    private var currentQuery = ""
    private lateinit var currentGenre: String
    private lateinit var currentType: String
    private lateinit var currentYear: String
    private var genreOptions: List<String> = emptyList()
    private var typeOptions: List<String> = emptyList()
    private var yearOptions: List<String> = emptyList()

    private val filterAll: String by lazy { getString(R.string.filter_all) }

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

        allBooks = BooksRepository.getAllBooks(assets)
        setupHomeUi()
        bindBottomBar()
    }

    private fun setupHomeUi() {
        homeRecycler = findViewById(R.id.contentRecycler)
        homeSearchInput = findViewById(R.id.homeSearchInput)
        homeSearchClear = findViewById(R.id.homeSearchClear)
        homeFilterButton = findViewById(R.id.homeFilterButton)
        homeFilterGenre = findViewById(R.id.homeFilterGenre)
        homeFilterType = findViewById(R.id.homeFilterType)
        homeFilterYear = findViewById(R.id.homeFilterYear)
        homeFilterReset = findViewById(R.id.homeFilterReset)
        homeResultsInfo = findViewById(R.id.homeResultsInfo)
        homeSectionTitle = findViewById(R.id.homeSectionTitle)
        homeEmptyState = findViewById(R.id.homeEmptyState)
        homeCardsRow = findViewById(R.id.homeCardsRow)
        homeCardNew = findViewById(R.id.homeCardNew)
        homeCardBest = findViewById(R.id.homeCardBest)

        val spanCount = 3
        val spacingPx = (12 * resources.displayMetrics.density).toInt()
        gridLayoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
        listLayoutManager = LinearLayoutManager(this)
        gridDecoration = GridSpacingItemDecoration(spanCount, spacingPx)

        val genres = loadGenresFromAssets()
        genresAdapter = HomeAdapter(assets, genres, spanCount, spacingPx) { genre ->
            openGenre(genre)
        }
        searchAdapter = MyBooksAdapter(assets) { item ->
            openDetails(item)
        }

        homeRecycler.layoutManager = gridLayoutManager
        homeRecycler.adapter = genresAdapter
        homeRecycler.addItemDecoration(gridDecoration)
        gridDecorationAdded = true
        homeRecycler.setHasFixedSize(false)
        homeRecycler.doOnLayout {
            val contentWidth = it.width - it.paddingLeft - it.paddingRight
            genresAdapter.updateContainerWidth(contentWidth)
        }

        homeCardNew.setOnClickListener { openBooksList(BooksListActivity.MODE_NEW) }
        homeCardBest.setOnClickListener { openBooksList(BooksListActivity.MODE_BEST) }

        genreOptions = buildOptions(allBooks.map { it.genre })
        typeOptions = buildOptions(allBooks.map { it.type })
        yearOptions = buildYearOptions(allBooks.map { it.year })

        currentGenre = filterAll
        currentType = filterAll
        currentYear = filterAll

        homeSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim().orEmpty()
                homeSearchClear.visibility = if (currentQuery.isBlank()) View.INVISIBLE else View.VISIBLE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        homeSearchClear.setOnClickListener { homeSearchInput.setText("") }

        homeFilterGenre.setOnClickListener {
            showChoiceDialog(getString(R.string.filter_genre), genreOptions, currentGenre) {
                currentGenre = it
                applyFilters()
            }
        }
        homeFilterType.setOnClickListener {
            showChoiceDialog(getString(R.string.filter_type), typeOptions, currentType) {
                currentType = it
                applyFilters()
            }
        }
        homeFilterYear.setOnClickListener {
            showChoiceDialog(getString(R.string.filter_year), yearOptions, currentYear) {
                currentYear = it
                applyFilters()
            }
        }
        homeFilterReset.setOnClickListener { resetFilters() }
        homeFilterButton.setOnClickListener { showFilterMenu() }

        homeSearchClear.visibility = View.INVISIBLE
        applyFilters()
    }

    private fun applyFilters() {
        val queryLower = currentQuery.trim().lowercase()
        val isFiltering = queryLower.isNotEmpty()
            || currentGenre != filterAll
            || currentType != filterAll
            || currentYear != filterAll

        updateFilterChips(isFiltering)

        if (!isFiltering) {
            showGenres()
            homeSectionTitle.text = getString(R.string.section_genres)
            homeResultsInfo.visibility = View.GONE
            homeEmptyState.visibility = View.GONE
            homeCardsRow.visibility = View.VISIBLE
            return
        }

        val filtered = allBooks.filter { book ->
            if (currentGenre != filterAll && !book.genre.equals(currentGenre, true)) return@filter false
            if (currentType != filterAll && !book.type.equals(currentType, true)) return@filter false
            if (currentYear != filterAll && book.year.trim() != currentYear) return@filter false

            if (queryLower.isNotEmpty()) {
                val haystack = listOf(
                    book.title,
                    book.author,
                    book.theme,
                    book.genre,
                    book.type,
                    book.heroes
                ).joinToString(" ").lowercase()
                if (!haystack.contains(queryLower)) return@filter false
            }
            true
        }

        showResults(filtered)
        homeSectionTitle.text = getString(R.string.section_results)
        homeResultsInfo.text = getString(R.string.results_count, filtered.size)
        homeResultsInfo.visibility = View.VISIBLE
        homeCardsRow.visibility = View.GONE
        homeEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showGenres() {
        if (homeRecycler.layoutManager !is StaggeredGridLayoutManager) {
            homeRecycler.layoutManager = gridLayoutManager
        }
        if (homeRecycler.adapter !== genresAdapter) {
            homeRecycler.adapter = genresAdapter
        }
        if (!gridDecorationAdded) {
            homeRecycler.addItemDecoration(gridDecoration)
            gridDecorationAdded = true
        }
    }

    private fun showResults(items: List<BookItem>) {
        if (gridDecorationAdded) {
            homeRecycler.removeItemDecoration(gridDecoration)
            gridDecorationAdded = false
        }
        if (homeRecycler.layoutManager !is LinearLayoutManager) {
            homeRecycler.layoutManager = listLayoutManager
        }
        if (homeRecycler.adapter !== searchAdapter) {
            homeRecycler.adapter = searchAdapter
        }
        searchAdapter.submitList(items)
    }

    private fun updateFilterChips(isFiltering: Boolean) {
        homeFilterGenre.text = getString(R.string.filter_chip_genre, currentGenre)
        homeFilterType.text = getString(R.string.filter_chip_type, currentType)
        homeFilterYear.text = getString(R.string.filter_chip_year, currentYear)

        homeFilterGenre.isSelected = currentGenre != filterAll
        homeFilterType.isSelected = currentType != filterAll
        homeFilterYear.isSelected = currentYear != filterAll

        homeFilterReset.isSelected = isFiltering
        homeFilterReset.isEnabled = isFiltering
        homeFilterReset.alpha = if (isFiltering) 1f else 0.5f
    }

    private fun resetFilters() {
        currentGenre = filterAll
        currentType = filterAll
        currentYear = filterAll
        homeSearchInput.setText("")
        applyFilters()
    }

    private fun showFilterMenu() {
        val options = arrayOf(
            getString(R.string.filter_genre),
            getString(R.string.filter_type),
            getString(R.string.filter_year),
            getString(R.string.filters_reset)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.filters_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> homeFilterGenre.performClick()
                    1 -> homeFilterType.performClick()
                    2 -> homeFilterYear.performClick()
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
        return listOf(filterAll) + result
    }

    private fun buildYearOptions(values: List<String>): List<String> {
        val years = values
            .map { it.trim() }
            .mapNotNull { it.toIntOrNull() }
            .distinct()
            .sortedDescending()
            .map { it.toString() }
        return listOf(filterAll) + years
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
