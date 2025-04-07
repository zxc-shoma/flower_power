package com.example.flowerpower

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class NoInternetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_internet)
        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)

        // Установить обработчик нажатия
        openSettingsButton.setOnClickListener {
            // Создаем Intent для открытия настроек
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivity(intent)
            finish()
        }
    }
}