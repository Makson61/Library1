package com.example.library

import android.content.res.AssetManager

object BooksRepository {
    fun getBooksForGenre(genre: String, coverPath: String): List<BookItem> {
        val normalized = genre.ifBlank { "Жанр" }
        return (1..6).map { index ->
            val title = "$normalized книга $index"
            BookItem(
                id = "$normalized-$index",
                title = title,
                type = "Роман",
                genre = normalized,
                theme = "Тема $index",
                author = "Автор $index",
                year = (2000 + index).toString(),
                heroes = "Герой $index",
                coverPath = coverPath
            )
        }
    }

    fun getAllBooks(assets: AssetManager): List<BookItem> {
        val files = assets.list("genres")
            ?.filter { it.endsWith(".png", true) || it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) }
            ?: emptyList()
        return files.flatMap { file ->
            val genre = file.substringBeforeLast('.')
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
            getBooksForGenre(genre, "genres/$file")
        }
    }
}
