package com.example.flowerpower

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private enum class Season {
        WINTER, SPRING, SUMMER, AUTUMN
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageResource(getSeasonalDrawable())

        // Получаем кнопку "Далее"
        val buttonNext: Button = findViewById(R.id.button1)

        // Обработка нажатия на кнопку "Далее"
        buttonNext.setOnClickListener {
            // Проверка наличия интернета при нажатии на кнопку
            if (!isInternetAvailable()) {
                // Если нет интернета, отправляем на экран с уведомлением
                navigateToNoInternetScreen()
            } else {
                // Если интернет есть, проверяем, авторизован ли пользователь
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Если пользователь авторизован, отправляем на главный экран
                    navigateToHomeScreen()
                } else {
                    // Если не авторизован, показываем экран регистрации
                    showSignUpScreen()
                }
            }
        }
    }

    // Метод для проверки наличия интернета
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // Проверяем, что активное подключение существует и поддерживает интернет
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // Переход на главный экран (MainActivity)
    private fun navigateToHomeScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()  // Закрываем WelcomeActivity, чтобы не было возврата
    }

    // Переход на экран с уведомлением об отсутствии интернета
    private fun navigateToNoInternetScreen() {
        val intent = Intent(this, NoInternetActivity::class.java)
        startActivity(intent)
        finish()  // Закрываем WelcomeActivity, чтобы не было возврата
    }

    // Переход на экран регистрации (SignUpActivity)
    private fun showSignUpScreen() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
        finish()  // Закрываем WelcomeActivity, чтобы не было возврата
    }

    private fun getCurrentSeason(): Season {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.MONTH)) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> Season.WINTER
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Season.SPRING
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Season.SUMMER
            else -> Season.AUTUMN
        }
    }


    private fun getSeasonalDrawable(): Int {
        return when (getCurrentSeason()) {
            Season.WINTER -> R.drawable.welcome_winter
            Season.SPRING -> R.drawable.welcome_spring
            Season.SUMMER -> R.drawable.welcome_summer
            Season.AUTUMN -> R.drawable.welcome_autumn
        }
    }
}
