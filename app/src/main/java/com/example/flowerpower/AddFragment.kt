package com.example.flowerpower

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Random

class AddFragment : Fragment() {

    

    private lateinit var occasionSpinner: Spinner
    private lateinit var budgetEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var addToCartButton: Button
    private lateinit var generatedFlowersRecyclerView: RecyclerView
    private lateinit var generatedFlowersAdapter: CartAdapter
    private val generatedFlowers = mutableListOf<Flower>()
    private val originalFlowersList = mutableListOf<Flower>()
    private lateinit var databaseReference: DatabaseReference
    private var dataLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        occasionSpinner = view.findViewById(R.id.occasionSpinner)
        budgetEditText = view.findViewById(R.id.budgetEditText)
        generateButton = view.findViewById(R.id.generateButton)
        addToCartButton = view.findViewById(R.id.addToCartButton)
        generatedFlowersRecyclerView = view.findViewById(R.id.generatedFlowersRecyclerView)







        // Инициализация спиннера случаев
        val occasions = resources.getStringArray(R.array.occasions)
        val occasionAdapter = ArrayAdapter(
            requireContext(),
            R.layout.custom_spinner_item,
            occasions
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.custom_spinner_item)
            occasionSpinner.adapter = adapter
        }
        occasionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Инициализация RecyclerView
        generatedFlowersAdapter = CartAdapter(generatedFlowers) { }
        generatedFlowersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        generatedFlowersRecyclerView.adapter = generatedFlowersAdapter

        // Инициализация Firebase
        databaseReference = FirebaseDatabase.getInstance("https://flower-power-d39c0-default-rtdb.europe-west1.firebasedatabase.app").getReference("flowers")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                originalFlowersList.clear()
                for (flowerSnapshot in snapshot.children) {
                    val flower = flowerSnapshot.getValue(Flower::class.java)
                    flower?.let { originalFlowersList.add(it) }
                }
                dataLoaded = true
                Log.d("AddFragment", "Data loaded: ${originalFlowersList.size} items")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("AddFragment", "loadPost:onCancelled", error.toException())
            }
        })

        generateButton.setOnClickListener {
            if (dataLoaded) {
                generateFlowerCombination()
            } else {
                showErrorDialog("Данные еще не загружены. Пожалуйста, подождите.")
            }
        }

        addToCartButton.setOnClickListener {
            addToCart()
        }

        return view
    }

    private fun generateFlowerCombination() {

        val selectedPosition = occasionSpinner.selectedItemPosition
        if (selectedPosition == 0) { // Проверка на выбор заголовка
            showErrorDialog("Пожалуйста, выберите повод из списка")
            return
        }
        // Получаем выбранный случай из выпадающего списка (Spinner)
        val selectedOccasion = occasionSpinner.selectedItem.toString()


        // Получаем бюджет из текстового поля, преобразуем его в число или устанавливаем 0, если поле пустое
        val budget = budgetEditText.text.toString().toIntOrNull() ?: 0

        // Фильтруем список цветов, оставляя только те, которые подходят под выбранный случай и бюджет
        val filteredFlowers = originalFlowersList.filter { flower ->
            flower.occasions.contains(selectedOccasion) && flower.price <= budget
        }

        // Если ни один цветок не подходит, выводим сообщение об ошибке
        if (filteredFlowers.isEmpty()) {
            showErrorDialog("Для выбранных параметров нет доступных цветов.")
            return
        }

        // Очищаем список сгенерированных цветов перед новой генерацией
        generatedFlowers.clear()
        var totalPrice = 0 // Текущая сумма стоимости цветов в букете
        val random = Random() // Для случайного выбора цветов

        // Перемешиваем отфильтрованные цветы для разнообразия
        val shuffledFlowers = filteredFlowers.shuffled()

        // Этап 1: Создаем базовый букет, добавляя цветы, пока бюджет позволяет
        for (flower in shuffledFlowers) {
            // Если добавление текущего цветка превысит бюджет, завершаем цикл
            if (totalPrice + flower.price > budget) break

            // Рассчитываем, сколько экземпляров текущего цветка можно добавить в пределах оставшегося бюджета
            val maxCount = (budget - totalPrice) / flower.price
            val count = random.nextInt(maxCount.coerceAtLeast(1)) + 1 // Случайное количество от 1 до maxCount

            // Создаем копию объекта Flower с указанным количеством экземпляров
            val newFlower = flower.copy(quantity = count)
            // Добавляем цветок в букет
            generatedFlowers.add(newFlower)
            // Увеличиваем общую стоимость букета
            totalPrice += flower.price * count
        }

        // Этап 2: Дополняем букет, чтобы максимально использовать бюджет
        while (totalPrice < budget) {
            // Фильтруем цветы, которые можно добавить, не превышая оставшийся бюджет и учитывая совместимость
            val affordableFlowers = shuffledFlowers.filter { it.price + totalPrice <= budget && isCompatible(it, generatedFlowers) }
            if (affordableFlowers.isEmpty()) break // Если таких цветов нет, завершаем цикл

            // Выбираем случайный доступный цветок
            val randomFlower = affordableFlowers[random.nextInt(affordableFlowers.size)]
            // Рассчитываем максимальное количество, которое можно добавить
            val maxCount = (budget - totalPrice) / randomFlower.price
            val count = random.nextInt(maxCount.coerceAtLeast(1)) + 1

            // Проверяем, есть ли уже этот цветок в букете
            val existingFlower = generatedFlowers.find { it.name == randomFlower.name }
            if (existingFlower != null) {
                // Если цветок уже есть, увеличиваем его количество
                val possibleAddCount = (budget - totalPrice) / randomFlower.price
                val addCount = count.coerceAtMost(possibleAddCount)
                existingFlower.quantity += addCount
                totalPrice += randomFlower.price * addCount
            } else {
                // Если цветка еще нет в букете, добавляем его
                val newFlower = randomFlower.copy(quantity = count)
                generatedFlowers.add(newFlower)
                totalPrice += randomFlower.price * count
            }
        }

        // Этап 3: Корректируем количество цветов, чтобы общее число было нечетным
        var totalFlowersCount = generatedFlowers.sumOf { it.quantity }
        if (totalFlowersCount % 2 == 0 && totalFlowersCount > 0) {
            for (flower in generatedFlowers) {
                // Уменьшаем количество у цветка, если это не нарушает бюджет
                if (flower.quantity > 1 && totalPrice - flower.price >= 0) {
                    flower.quantity -= 1
                    totalPrice -= flower.price
                    totalFlowersCount -= 1
                    break
                }
            }
        }

        // Если по какой-то причине не удалось собрать букет или вышли за бюджет, выводим сообщение
        if (totalPrice > budget || generatedFlowers.isEmpty()) {
            showErrorDialog("Не удалось собрать букет в пределах бюджета.")
            return
        }

        // Обновляем адаптер RecyclerView, чтобы отобразить сгенерированный букет
        generatedFlowersAdapter.notifyDataSetChanged()
    }

    private fun isCompatible(flower: Flower, generatedFlowers: List<Flower>): Boolean {
        // Проверяем совместимость цветка с уже добавленными в букет
        for (generatedFlower in generatedFlowers) {
            if (!flower.compatible.contains(generatedFlower.name)) {
                return false
            }
        }
        return true
    }

    private fun showErrorDialog(message: String = "Не удалось собрать букет. Проверьте параметры и попробуйте снова.") {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun addToCart() {
        for (flower in generatedFlowers) {
            CartManager.addToCart(flower)
        }
        generatedFlowers.clear()
        generatedFlowersAdapter.notifyDataSetChanged()
    }



}
