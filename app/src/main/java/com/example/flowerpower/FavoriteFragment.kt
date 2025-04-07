package com.example.flowerpower

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import kotlin.random.Random

class FavoriteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private lateinit var totalCostValue: TextView
    private lateinit var checkoutButton: View
    private lateinit var clearCartButton: View
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adressTextView: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Элементы, контролирующие видимость при пустой корзине
    private lateinit var deliveryOptionsContainer: View
    private lateinit var emptyCartMessage: TextView
    private lateinit var deliveryAddressContainer: View
    private lateinit var totalCostContainer: View

    // Дополнительная стоимость (500 при доставке, 0 при самовывозе)
    private var currentExtraCost: Int = 0

    companion object {
        const val REQUEST_CODE_MAP = 1001
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite, container, false)

        // Инициализация элементов
        recyclerView = view.findViewById(R.id.cartRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        cartAdapter = CartAdapter(CartManager.cartItems) { updateTotalCost(currentExtraCost) }
        recyclerView.adapter = cartAdapter

        totalCostValue = view.findViewById(R.id.totalCostValue)
        adressTextView = view.findViewById(R.id.adress)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        checkoutButton = view.findViewById(R.id.checkoutButton)
        clearCartButton = view.findViewById(R.id.clearCartButton)

        // Элементы для контроля видимости при пустой корзине
        deliveryOptionsContainer = view.findViewById(R.id.deliveryOptionsContainer)
        deliveryAddressContainer = view.findViewById(R.id.deliveryAddressContainer)
        totalCostContainer = view.findViewById(R.id.totalCostContainer)
        emptyCartMessage = view.findViewById(R.id.emptyCartMessage)

        // Обработчики кнопок
        checkoutButton.setOnClickListener { onCheckoutClicked() }
        clearCartButton.setOnClickListener { showClearCartConfirmationDialog() }

        // Получаем ссылки на радио-кнопки
        val deliveryButton: RadioButton = view.findViewById(R.id.deliveryButton)
        val pickupButton: RadioButton = view.findViewById(R.id.pickupButton)

        // По умолчанию выбран самовывоз
        pickupButton.isChecked = true

        // При выборе доставки – устанавливаем доп. стоимость 500
        deliveryButton.setOnClickListener {
            requestLocation()
            currentExtraCost = 500
            updateTotalCost(currentExtraCost)
            // Запускаем MapsFragment для выбора адреса
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, MapsFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
        // При выборе самовывоза – сбрасываем доп. стоимость
        pickupButton.setOnClickListener {
            currentExtraCost = 0
            adressTextView.text = "ул. Революционная 109"
            updateTotalCost(currentExtraCost)
        }

        // Инициализация Firebase
        databaseReference =
            FirebaseDatabase.getInstance("https://flower-power-d39c0-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("orders")
        auth = FirebaseAuth.getInstance()

        // Проверяем, пуста ли корзина, и скрываем элементы, если да
        if (CartManager.cartItems.isEmpty()) {
            emptyCartMessage.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            deliveryOptionsContainer.visibility = View.GONE
            deliveryAddressContainer.visibility = View.GONE
            adressTextView.visibility = View.GONE
            totalCostValue.visibility = View.GONE
            totalCostContainer.visibility = View.GONE
            checkoutButton.visibility = View.GONE
            clearCartButton.visibility = View.GONE
        } else {
            emptyCartMessage.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            deliveryOptionsContainer.visibility = View.VISIBLE
            adressTextView.visibility = View.VISIBLE
            totalCostValue.visibility = View.VISIBLE
            totalCostContainer.visibility = View.VISIBLE
            checkoutButton.visibility = View.VISIBLE
            clearCartButton.visibility = View.VISIBLE
        }

        updateTotalCost(currentExtraCost)

        return view
    }

    // Метод обновления общей стоимости с учетом дополнительной суммы
    private fun updateTotalCost(extraCost: Int = 0) {
        val totalCost = CartManager.getTotalCost() + extraCost
        totalCostValue.text = "$totalCost ₽"
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_MAP
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val address = getAddressFromLocation(it.latitude, it.longitude)
                adressTextView.text = address ?: "Не удалось определить адрес"
            } ?: run {
                adressTextView.text = "Местоположение недоступно"
            }
        }
    }

    private fun getAddressFromLocation(lat: Double, lon: Double): String? {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        return addresses?.firstOrNull()?.getAddressLine(0)
    }

    override fun onResume() {
        super.onResume()
        cartAdapter.notifyDataSetChanged()
        updateTotalCost(currentExtraCost)
    }

    // При нажатии на "Оформить заказ" открывается OrderActivity с передачей параметров
    private fun onCheckoutClicked() {
        if (CartManager.cartItems.isEmpty()) {
            showErrorDialog("Не удалось оформить заказ. Проверьте параметры и попробуйте снова.")
        } else {
            val intent = Intent(requireContext(), OrderActivity::class.java)
            // Передаем тип доставки: "delivery" если currentExtraCost == 500, иначе "pickup"
            intent.putExtra("deliveryType", if (currentExtraCost == 500) "delivery" else "pickup")
            // Передаем дополнительную стоимость
            intent.putExtra("extraCost", currentExtraCost)
            // Передаем базовую стоимость корзины
            intent.putExtra("baseTotal", CartManager.getTotalCost())
            // Передаем адрес (если доставка, выбранный адрес, если самовывоз – фиксированный)
            intent.putExtra("selectedAddress", adressTextView.text.toString())
            startActivity(intent)
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showPaymentDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_payment, null)
        val cardNumberEditText: EditText = dialogView.findViewById(R.id.cardNumberEditText)
        val expiryDateEditText: EditText = dialogView.findViewById(R.id.expiryDateEditText)
        val cvcEditText: EditText = dialogView.findViewById(R.id.cvcEditText)
        val cashOnDeliveryCheckBox: CheckBox = dialogView.findViewById(R.id.cashOnDeliveryCheckBox)
        val cardNum: TextView = dialogView.findViewById(R.id.cardNumText)
        val yearNum: TextView = dialogView.findViewById(R.id.yearNum)
        val CvvNum: TextView = dialogView.findViewById(R.id.CvvNum)

        cardNumberEditText.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                s?.let {
                    val formatted = it.toString().replace(" ", "").chunked(4).joinToString(" ")
                    cardNumberEditText.setText(formatted)
                    cardNumberEditText.setSelection(formatted.length)
                }
                isEditing = false
            }
        })

        expiryDateEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().length == 2 && !s.toString().contains("/")) {
                    expiryDateEditText.setText("$s/")
                    expiryDateEditText.setSelection(expiryDateEditText.text.length)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cashOnDeliveryCheckBox.setOnCheckedChangeListener { _, isChecked ->
            cardNumberEditText.visibility = if (isChecked) View.GONE else View.VISIBLE
            expiryDateEditText.visibility = if (isChecked) View.GONE else View.VISIBLE
            cvcEditText.visibility = if (isChecked) View.GONE else View.VISIBLE
            cardNum.visibility = if (isChecked) View.GONE else View.VISIBLE
            CvvNum.visibility = if (isChecked) View.GONE else View.VISIBLE
            yearNum.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Выберите способ оплаты")
            .setView(dialogView)
            .setPositiveButton("Оформить") { _, _ ->
                val isCashSelected = cashOnDeliveryCheckBox.isChecked
                val cardNumber = cardNumberEditText.text.toString().replace(" ", "")
                val expiryDate = expiryDateEditText.text.toString()
                val cvc = cvcEditText.text.toString()

                if (!isCashSelected && cardNumber.isEmpty() && expiryDate.isEmpty() && cvc.isEmpty()) {
                    showErrorDialog("Пожалуйста, выберите способ оплаты или введите данные карты.")
                    return@setPositiveButton
                }

                if (isCashSelected) {
                    val orderCode = generateOrderCode()
                    saveOrderToDatabase(orderCode, "Оплата при получении")
                    showOrderConfirmationDialog(orderCode)
                    clearCart()
                } else if (validateCardDetails(cardNumber, expiryDate, cvc)) {
                    val orderCode = generateOrderCode()
                    saveOrderToDatabase(orderCode, "Банковская карта")
                    showOrderConfirmationDialog(orderCode)
                    clearCart()
                } else {
                    showErrorDialog("Неверные данные карты. Пожалуйста, проверьте и попробуйте снова.")
                }
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun validateCardDetails(cardNumber: String, expiryDate: String, cvc: String): Boolean {
        val (month, year) = expiryDate.split("/")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
        if (cardNumber.length != 16 || cvc.length != 3 || month.length != 2 || year.length != 2) {
            return false
        }
        val monthInt = month.toIntOrNull() ?: return false
        val yearInt = year.toIntOrNull() ?: return false
        if (monthInt !in 1..12 || yearInt > 36) return false
        return yearInt > currentYear
    }

    private fun showClearCartConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Очистить корзину")
            .setMessage("Вы точно хотите очистить корзину?")
            .setPositiveButton("Да") { _, _ -> clearCart() }
            .setNegativeButton("Нет") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun generateOrderCode(): String {
        return (100 + Random.nextInt(900)).toString()
    }

    private fun saveOrderToDatabase(orderCode: String, paymentMethod: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val finalCost = CartManager.getTotalCost() + currentExtraCost
            val deliveryAddress = adressTextView.text.toString()
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

    private fun showOrderConfirmationDialog(orderCode: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Заказ оформлен")
            .setMessage("Ваш заказ успешно оформлен. Код заказа: $orderCode. Для уточнения деталей заказа с вами свяжется наш менеджер.")
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showErrorDialog(message: String = "Не удалось оформить. Проверьте параметры и попробуйте снова.") {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun clearCart() {
        CartManager.cartItems.clear()
        cartAdapter.notifyDataSetChanged()
        updateTotalCost(currentExtraCost)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener("addressRequestKey", viewLifecycleOwner) { _, bundle ->
            val selectedAddress = bundle.getString("selectedAddress")
            adressTextView.text = selectedAddress ?: "Адрес не выбран"
        }
    }
}
