package com.example.library

import android.content.res.AssetManager
import org.json.JSONArray
import kotlin.math.absoluteValue

object BooksRepository {
    private const val BOOKS_JSON_PATH = "books/books.json"
    private var cachedBooks: List<BookItem>? = null

    fun getBooksForGenre(assets: AssetManager, genre: String, coverFallback: String): List<BookItem> {
        val normalized = genre.ifBlank { "Жанр" }
        val fallback = coverFallback.trim()
        val books = loadBooks(assets)
        val filtered = books.filter { it.genre.equals(normalized, ignoreCase = true) }
        if (filtered.isNotEmpty()) {
            return filtered.map { book ->
                val shouldReplace = book.coverPath.isBlank() || book.coverPath.startsWith("genres/", true)
                if (shouldReplace && fallback.isNotBlank()) {
                    book.copy(coverPath = fallback)
                } else {
                    book
                }
            }
        }
        return buildFallbackBooks(normalized, coverFallback)
    }

    fun getAllBooks(assets: AssetManager): List<BookItem> {
        val books = loadBooks(assets)
        if (books.isNotEmpty()) return books

        val files = assets.list("genres")
            ?.filter { it.endsWith(".png", true) || it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) }
            ?: emptyList()
        return files.flatMap { file ->
            val genre = file.substringBeforeLast('.')
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
            buildFallbackBooks(genre, "genres/$file")
        }
    }

    private fun loadBooks(assets: AssetManager): List<BookItem> {
        cachedBooks?.let { return it }
        val parsed = runCatching { parseBooks(assets) }.getOrDefault(emptyList())
        cachedBooks = parsed
        return parsed
    }

    private fun parseBooks(assets: AssetManager): List<BookItem> {
        val json = assets.open(BOOKS_JSON_PATH)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        if (json.isBlank()) return emptyList()

        val array = JSONArray(json)
        val result = mutableListOf<BookItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optString("title").trim()
            if (title.isBlank()) continue
            val author = obj.optString("author").trim()
            val year = obj.optString("year").trim()
            val genre = obj.optString("genre").trim()
            val type = obj.optString("type").trim()
            val theme = obj.optString("theme").trim()
            val heroes = obj.optString("heroes").trim()
            val coverPath = obj.optString("coverPath").trim()
            val id = obj.optString("id").ifBlank {
                generateId(title, author, year, genre)
            }

            result.add(
                BookItem(
                    id = id,
                    title = title,
                    type = type.ifBlank { "-" },
                    genre = genre.ifBlank { "-" },
                    theme = theme.ifBlank { "-" },
                    author = author.ifBlank { "-" },
                    year = year.ifBlank { "-" },
                    heroes = heroes.ifBlank { "-" },
                    coverPath = coverPath
                )
            )
        }
        return result
    }

    private fun buildFallbackBooks(genre: String, coverFallback: String): List<BookItem> {
        return (1..6).map { index ->
            val title = "$genre книга $index"
            BookItem(
                id = "$genre-$index",
                title = title,
                type = "Роман",
                genre = genre,
                theme = "Тема $index",
                author = "Автор $index",
                year = (2000 + index).toString(),
                heroes = "Герой $index",
                coverPath = coverFallback
            )
        }
    }

    private fun generateId(vararg parts: String): String {
        val source = parts.filter { it.isNotBlank() }.joinToString("-")
        if (source.isBlank()) return "book-${System.nanoTime()}"
        val slug = source.lowercase()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("[^\\p{L}\\p{N}-]"), "")
            .trim('-')
        val base = if (slug.isBlank()) "book" else slug
        val hash = source.hashCode().absoluteValue
        return "$base-$hash"
    }
}
