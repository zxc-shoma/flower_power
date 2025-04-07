package com.example.flowerpower

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

class OrderActivity : AppCompatActivity() {

    private lateinit var headerTypeTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var paymentRadioGroup: RadioGroup
    private lateinit var cardInputContainer: LinearLayout
    private lateinit var orderItemsRecyclerView: RecyclerView
    private lateinit var totalPriceTextView: TextView
    private lateinit var promoCodeEditText: TextInputEditText
    private lateinit var applyPromoButton: Button
    private lateinit var confirmOrderButton: Button

    private var baseTotal: Int = 0
    private var extraCost: Int = 0
    private var currentTotal: Int = 0
    private var isPromoApplied: Boolean = false

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        // Инициализация UI элементов
        headerTypeTextView = findViewById(R.id.headerTypeTextView)
        addressTextView = findViewById(R.id.addressTextView)
        paymentRadioGroup = findViewById(R.id.paymentRadioGroup)
        cardInputContainer = findViewById(R.id.cardInputContainer)
        orderItemsRecyclerView = findViewById(R.id.orderItemsRecyclerView)
        totalPriceTextView = findViewById(R.id.totalPriceTextView)
        promoCodeEditText = findViewById(R.id.promoCodeEditText)
        applyPromoButton = findViewById(R.id.applyPromoButton)
        confirmOrderButton = findViewById(R.id.confirmOrderButton)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance("https://flower-power-d39c0-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("orders")

        // Получение параметров из Intent
        val deliveryType = intent.getStringExtra("deliveryType") ?: "pickup"
        extraCost = intent.getIntExtra("extraCost", 0)
        baseTotal = intent.getIntExtra("baseTotal", 0)
        val selectedAddress = intent.getStringExtra("selectedAddress") ?: ""

        // Установка заголовка и адреса в зависимости от типа доставки
        if (deliveryType == "delivery") {
            headerTypeTextView.text = "Доставка"
            addressTextView.text = if (selectedAddress.isEmpty()) "Адрес не выбран" else selectedAddress
        } else {
            headerTypeTextView.text = "Самовывоз"
            addressTextView.text = "ул. Революционная, 109"
        }

        // Настройка RecyclerView для отображения товаров из корзины
        orderItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        val orderAdapter = CartAdapter(CartManager.cartItems) { }
        orderItemsRecyclerView.adapter = orderAdapter

        // Вычисление и отображение итоговой стоимости
        currentTotal = baseTotal + extraCost
        totalPriceTextView.text = "Итоговая стоимость: $currentTotal ₽"

        // Обработчик применения промокода
        applyPromoButton.setOnClickListener {
            val promoCode = promoCodeEditText.text.toString().trim()
            if (promoCode.equals("sale20", ignoreCase = true)) {
                if (!isPromoApplied) {
                    currentTotal = ((baseTotal + extraCost) * 0.8).toInt()
                    totalPriceTextView.text = "Итоговая стоимость: $currentTotal ₽ (со скидкой)"
                    isPromoApplied = true
                } else {
                    Toast.makeText(this, "Промокод уже применён", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Неверный промокод", Toast.LENGTH_SHORT).show()
            }
        }

        // Слушатель изменения способа оплаты: если выбрана оплата картой, показываем поля ввода
        paymentRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.cardPaymentRadio) {
                cardInputContainer.visibility = LinearLayout.VISIBLE
            } else {
                cardInputContainer.visibility = LinearLayout.GONE
            }
        }

        // Инициализация полей ввода для данных карты
        val cardNumberEditText = findViewById<EditText>(R.id.cardNumberEditText)
        val expiryDateEditText = findViewById<EditText>(R.id.expiryDateEditText)
        val cvcEditText = findViewById<EditText>(R.id.cvcEditText)

        // Форматирование номера карты: разделяем по 4 цифры, оставляем только цифры и ограничиваем длину до 16
        cardNumberEditText.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                s?.let {
                    // Убираем пробелы и оставляем только цифры
                    val digits = it.toString().replace(" ", "").filter { ch -> ch.isDigit() }
                    // Ограничиваем длину до 16 символов
                    val limited = if (digits.length > 16) digits.substring(0, 16) else digits
                    // Добавляем пробелы каждые 4 цифры
                    val formatted = limited.chunked(4).joinToString(" ")
                    cardNumberEditText.setText(formatted)
                    cardNumberEditText.setSelection(formatted.length)
                }
                isEditing = false
            }
        })

        // Форматирование срока действия: автоматически вставляем "/" после ввода двух цифр, оставляем только допустимые символы
        expiryDateEditText.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                s?.let {
                    // Фильтруем символы, оставляем цифры и слэш
                    var text = it.toString().filter { ch -> ch.isDigit() || ch == '/' }
                    // Если введено 2 цифры и отсутствует "/", добавляем его
                    if (text.length == 2 && !text.contains("/")) {
                        text += "/"
                    }
                    // Ограничиваем длину ввода до 5 символов (MM/YY)
                    if (text.length > 5) {
                        text = text.substring(0, 5)
                    }
                    expiryDateEditText.setText(text)
                    expiryDateEditText.setSelection(text.length)
                }
                isEditing = false
            }
        })

        // Ограничение ввода для CVV: оставляем только цифры, длина не более 3 символов
        cvcEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val digits = it.toString().filter { ch -> ch.isDigit() }
                    val limited = if (digits.length > 3) digits.substring(0, 3) else digits
                    if (limited != it.toString()) {
                        cvcEditText.setText(limited)
                        cvcEditText.setSelection(limited.length)
                    }
                }
            }
        })

        // Обработчик кнопки подтверждения заказа
        confirmOrderButton.setOnClickListener {
            val selectedPaymentMethod = when (paymentRadioGroup.checkedRadioButtonId) {
                R.id.cardPaymentRadio -> "Банковская карта"
                R.id.cashPaymentRadio -> "Оплата при получении"
                else -> "Оплата при получении"
            }

            // Если оплата картой, проверяем данные карты
            if (selectedPaymentMethod == "Банковская карта") {
                val cardNumber = cardNumberEditText.text.toString().replace(" ", "")
                val expiryDate = expiryDateEditText.text.toString()
                val cvc = cvcEditText.text.toString()

                if (cardNumber.isEmpty() || expiryDate.isEmpty() || cvc.isEmpty()) {
                    Toast.makeText(this, "Пожалуйста, введите данные карты", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!validateCardDetails(cardNumber, expiryDate, cvc)) {
                    Toast.makeText(this, "Неверные данные карты. Проверьте и попробуйте снова.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Если все проверки пройдены, сохраняем заказ
            val orderCode = generateOrderCode()
            saveOrderToDatabase(orderCode, selectedPaymentMethod)
            // Отображаем AlertDialog с кодом заказа
            AlertDialog.Builder(this)
                .setTitle("Заказ оформлен")
                .setMessage("Ваш заказ успешно оформлен.\nКод заказа: $orderCode")
                .setPositiveButton("ОК") { dialog, _ ->
                    dialog.dismiss()
                    CartManager.cartItems.clear()
                    finish()
                }
                .show()
        }
    }

    private fun validateCardDetails(cardNumber: String, expiryDate: String, cvc: String): Boolean {
        val parts = expiryDate.split("/")
        if (parts.size != 2) return false
        val month = parts[0]
        val year = parts[1]
        val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
        if (cardNumber.length != 16 || cvc.length != 3 || month.length != 2 || year.length != 2) return false
        val monthInt = month.toIntOrNull() ?: return false
        val yearInt = year.toIntOrNull() ?: return false
        if (monthInt !in 1..12 || yearInt > 36) return false
        return yearInt > currentYear
    }

    private fun generateOrderCode(): String {
        return (100 + Random.nextInt(900)).toString()
    }

    private fun saveOrderToDatabase(orderCode: String, paymentMethod: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Рассчитываем базовую сумму заказа
            val rawCost = CartManager.getTotalCost() + extraCost
            // Если промокод применён, сохраняем сумму со скидкой
            val finalCost = if (isPromoApplied) (rawCost * 0.8).toInt() else rawCost
            val deliveryAddress = addressTextView.text.toString()
            val order = Order(
                orderCode = orderCode,
                items = CartManager.cartItems,
                totalCost = finalCost,
                paymentMethod = paymentMethod,
                deliveryAddress = deliveryAddress,
                date = Date()
            )
            databaseReference.child(userId).child(orderCode).setValue(order)
        }
    }
}
