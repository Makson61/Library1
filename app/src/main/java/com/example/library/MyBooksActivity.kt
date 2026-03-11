package com.example.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MyBooksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_books)
        setupClickHandlers()
    }

    private fun setupClickHandlers() {
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.navMyBooks)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.nav_my_books), Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.nav_profile), Toast.LENGTH_SHORT).show()
        }

        val clickMap = mapOf(
            R.id.searchContainer to R.string.search_hint,
            R.id.clearSearch to R.string.cd_clear,
            R.id.bookItem1 to R.string.book_title,
            R.id.bookItem2 to R.string.book_title,
            R.id.bookItem3 to R.string.book_title,
            R.id.bookItem4 to R.string.book_title
        )

        clickMap.forEach { (id, labelRes) ->
            findViewById<View>(id)?.setOnClickListener {
                Toast.makeText(this, getString(labelRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
