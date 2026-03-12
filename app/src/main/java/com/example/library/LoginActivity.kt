package com.example.library

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Calendar

class LoginActivity : AppCompatActivity() {
    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        hideNavigationBar()

        if (isLoggedIn()) {
            goToMain()
            return
        }

        val inputName = findViewById<EditText>(R.id.inputName)
        val inputBirth = findViewById<EditText>(R.id.inputBirth)
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val buttonAction = findViewById<Button>(R.id.buttonAction)
        val toggleText = findViewById<TextView>(R.id.toggleText)

        inputBirth.setOnClickListener { showDatePicker(inputBirth) }

        buttonAction.setOnClickListener {
            val name = inputName.text.toString().trim()
            val birth = inputBirth.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString()

            if (isRegisterMode) {
                if (name.isBlank() || birth.isBlank() || email.isBlank() || password.isBlank()) {
                    Toast.makeText(this, "Неверно", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!email.contains("@")) {
                    Toast.makeText(this, "Неверно", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                saveUser(name, birth, email, password)
                setLoggedIn(true)
                goToMain()
            } else {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(this, "Неверно", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val savedEmail = getPrefs().getString("user_email", null)
                val savedPassword = getPrefs().getString("user_password", null)
                if (savedEmail == null || savedPassword == null) {
                    Toast.makeText(this, "Неверно", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (email != savedEmail || password != savedPassword) {
                    Toast.makeText(this, "Неверно", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                setLoggedIn(true)
                goToMain()
            }
        }

        toggleText.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateModeUI(isRegisterMode, inputName, inputBirth, buttonAction, toggleText)
        }

        updateModeUI(isRegisterMode, inputName, inputBirth, buttonAction, toggleText)
    }

    private fun updateModeUI(
        isRegister: Boolean,
        inputName: EditText,
        inputBirth: EditText,
        buttonAction: Button,
        toggleText: TextView
    ) {
        inputName.visibility = if (isRegister) View.VISIBLE else View.GONE
        inputBirth.visibility = if (isRegister) View.VISIBLE else View.GONE
        buttonAction.text = if (isRegister) "Создать" else "Войти"
        toggleText.text = if (isRegister) "Уже есть аккаунт? Войти" else "Еще нет аккаунта? Создать"
    }

    private fun showDatePicker(target: EditText) {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val mm = (month + 1).toString().padStart(2, '0')
                val dd = dayOfMonth.toString().padStart(2, '0')
                target.setText("$dd.$mm.$year")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun saveUser(name: String, birth: String, email: String, password: String) {
        getPrefs().edit()
            .putString("user_name", name)
            .putString("user_birth", birth)
            .putString("user_email", email)
            .putString("user_password", password)
            .apply()
    }

    private fun setLoggedIn(value: Boolean) {
        getPrefs().edit().putBoolean("logged_in", value).apply()
    }

    private fun isLoggedIn(): Boolean = getPrefs().getBoolean("logged_in", false)

    private fun getPrefs() = getSharedPreferences("auth_prefs", MODE_PRIVATE)

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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

        val root = findViewById<View>(R.id.loginRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }
    }
}
