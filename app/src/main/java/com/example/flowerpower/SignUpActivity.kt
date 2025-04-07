package com.example.flowerpower

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Инициализация Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Конфигурация Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Элементы интерфейса
        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val signUpButton: Button = findViewById(R.id.signInButton)
        val textView: TextView = findViewById(R.id.textViewAuth)
        val googleSignInButton: com.google.android.gms.common.SignInButton =
            findViewById(R.id.googleSignInButton)
        val forgotPasswordText: TextView = findViewById(R.id.forgotPasswordText)
        googleSignInButton.setSize(com.google.android.gms.common.SignInButton.SIZE_ICON_ONLY)



        // Кнопка входа через Email/Password
        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty()) {
                emailEditText.error = "Введите email"
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                passwordEditText.error = "Пароль должен быть не менее 6 символов"
                return@setOnClickListener
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Вход выполнен успешно", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Неверные данные", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Кнопка входа через Google
        googleSignInButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        // Переход на страницу регистрации
        textView.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }

        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }

    }
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Восстановление пароля")

        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailInput = view.findViewById<EditText>(R.id.emailEditText)

        builder.setView(view)
        builder.setPositiveButton("Отправить") { _, _ ->
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Отмена", null)

        builder.create().show()
    }

    private fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Письмо для сброса пароля отправлено на $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка: ${task.exception?.message ?: "Не удалось отправить письмо"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Вход через Google выполнен успешно", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Ошибка авторизации через Google", Toast.LENGTH_LONG).show()
                }
            }
    }
}
