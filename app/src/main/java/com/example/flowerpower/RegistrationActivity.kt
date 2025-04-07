package com.example.flowerpower

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameEditText: EditText = findViewById(R.id.username)
        val emailEditText: EditText = findViewById(R.id.email)
        val passwordEditText: EditText = findViewById(R.id.password)
        val confirmPasswordEditText: EditText = findViewById(R.id.confirmPassword)
        val registerButton: Button = findViewById(R.id.registerButton)
        val textView: TextView = findViewById(R.id.textViewAlredy)

        registerButton.setOnClickListener {
            val userName = nameEditText.text.toString().trim()
            val userEmail = emailEditText.text.toString().trim()
            val userPassword = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (userName.isEmpty()) {
                nameEditText.error = "Введите ваше имя"
                return@setOnClickListener
            }
            if (userEmail.isEmpty()) {
                emailEditText.error = "Введите вашу электронную почту"
                return@setOnClickListener
            }
            if (userPassword.isEmpty() || userPassword.length < 6) {
                passwordEditText.error = "Пароль должен быть не менее 6 символов"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                confirmPasswordEditText.error = "Подтвердите пароль"
                return@setOnClickListener
            }
            if (userPassword != confirmPassword) {
                confirmPasswordEditText.error = "Пароли не совпадают"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid

                        if (userId != null) {
                            val userData = hashMapOf(
                                "name" to userName,
                                "email" to userEmail
                            )

                            db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { emailTask ->
                                            if (emailTask.isSuccessful) {
                                                Toast.makeText(
                                                    this,
                                                    "Письмо с подтверждением отправлено. Проверьте вашу почту.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                auth.signOut()
                                                navigateToLogin()
                                            } else {
                                                Toast.makeText(
                                                    this,
                                                    "Ошибка отправки письма: ${emailTask.exception?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Ошибка сохранения данных: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    } else {
                        val errorMessage = task.exception?.message ?: "Неизвестная ошибка"
                        Toast.makeText(
                            this,
                            "Ошибка регистрации: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        textView.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
        finish()
    }
}