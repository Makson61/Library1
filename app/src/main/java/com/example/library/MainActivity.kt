package com.example.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn_world)
        setupClickHandlers()
    }

    private fun setupClickHandlers() {
        findViewById<View>(R.id.navMyBooks)?.setOnClickListener {
            startActivity(Intent(this, MyBooksActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        val clickMap = mapOf(
            R.id.iconSearch to R.string.cd_search,
            R.id.iconMenu to R.string.cd_menu,
            R.id.searchContainer to R.string.search_hint,
            R.id.clearSearch to R.string.cd_clear,
            R.id.cardNew to R.string.new_items,
            R.id.cardBest to R.string.best_items,
            R.id.genreTile1 to R.string.genre_classic,
            R.id.genreTile2 to R.string.genre_fantasy,
            R.id.genreTile3 to R.string.genre,
            R.id.genreTile4 to R.string.genre,
            R.id.genreTile5 to R.string.genre,
            R.id.genreTile6 to R.string.genre,
            R.id.genreTile7 to R.string.genre,
            R.id.genreTile8 to R.string.genre,
            R.id.genreTile9 to R.string.genre,
            R.id.navHome to R.string.nav_home,
            R.id.navProfile to R.string.nav_profile
        )

        clickMap.forEach { (id, labelRes) ->
            findViewById<View>(id)?.setOnClickListener {
                Toast.makeText(this, getString(labelRes), Toast.LENGTH_SHORT).show()
            }
        }
    }
}




