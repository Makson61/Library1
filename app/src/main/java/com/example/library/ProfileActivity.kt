package com.example.library

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide

class ProfileActivity : AppCompatActivity() {
    private val pickAvatar = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            saveAvatar(uri.toString())
            loadAvatar(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        hideNavigationBar()

        bindProfile()
        bindNav()
    }

    private fun bindProfile() {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val name = prefs.getString("user_name", "Имя пользователя") ?: "Имя пользователя"
        val email = prefs.getString("user_email", "") ?: ""
        val password = prefs.getString("user_password", "") ?: ""
        val about = prefs.getString("user_about", "") ?: ""
        val avatar = prefs.getString("user_avatar_uri", null)

        findViewById<TextView>(R.id.profileName).text = name
        findViewById<TextView>(R.id.profileEmail).text = "Email: $email"
        findViewById<TextView>(R.id.profilePassword).text = "Пароль: $password"

        val aboutField = findViewById<EditText>(R.id.profileAbout)
        aboutField.setText(about)
        aboutField.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                prefs.edit().putString("user_about", aboutField.text.toString()).apply()
            }
        }

        if (avatar != null) {
            loadAvatar(avatar)
        }

        val pickTargets = listOf(
            findViewById<View>(R.id.profileAvatar),
            findViewById<View>(R.id.profilePickButton),
            findViewById<View>(R.id.profilePickText)
        )
        pickTargets.forEach { it.setOnClickListener { pickAvatar.launch(arrayOf("image/*")) } }

        findViewById<View>(R.id.profileLogout).setOnClickListener {
            prefs.edit().putBoolean("logged_in", false).apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadAvatar(uriString: String) {
        val imageView = findViewById<ImageView>(R.id.profileAvatar)
        Glide.with(imageView)
            .load(Uri.parse(uriString))
            .centerCrop()
            .into(imageView)
    }

    private fun saveAvatar(uri: String) {
        getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .edit()
            .putString("user_avatar_uri", uri)
            .apply()
    }

    private fun bindNav() {
        findViewById<View>(R.id.profileNavHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.profileNavMyBooks).setOnClickListener {
            startActivity(Intent(this, MyBooksActivity::class.java))
            finish()
        }
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

        val root = findViewById<View>(R.id.profileRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
    }
}
