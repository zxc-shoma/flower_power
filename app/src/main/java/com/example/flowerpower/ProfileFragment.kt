package com.example.flowerpower

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val ordersList = mutableListOf<Order>()
    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var noOrdersTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        databaseReference = FirebaseDatabase.getInstance("https://flower-power-d39c0-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("orders")

        // Инициализация элементов интерфейса
        recyclerView = view.findViewById(R.id.ordersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        orderAdapter = OrderAdapter(ordersList)
        recyclerView.adapter = orderAdapter

        profileNameTextView = view.findViewById(R.id.nameTextView)
        profileEmailTextView = view.findViewById(R.id.emailTextView)
        logoutButton = view.findViewById(R.id.logoutButton)
        noOrdersTextView = view.findViewById(R.id.noOrdersTextView)

        // Устанавливаем начальный текст
        profileNameTextView.text = "Загрузка..."
        profileEmailTextView.text = "Загрузка..."

        // Загрузка данных пользователя
        val currentUser = auth.currentUser
        loadUserData(currentUser)

        // Загрузка заказов
        loadUserOrders(currentUser?.uid)

        // Обработчик выхода из аккаунта
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), SignUpActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }

    private fun loadUserData(user: FirebaseUser?) {
        if (user != null) {
            // Получение данных из учетной записи Google
            val userName = user.displayName
            val userEmail = user.email

            profileNameTextView.text = userName ?: "Имя не указано"
            profileEmailTextView.text = userEmail ?: "Email не указан"

            // Если нужно получить дополнительные данные из Firestore
            val userId = user.uid
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firestoreName = document.getString("name")
                        val firestoreEmail = document.getString("email")

                        // Если данные из Firestore присутствуют, используем их
                        profileNameTextView.text = firestoreName ?: userName
                        profileEmailTextView.text = firestoreEmail ?: userEmail
                    }
                }
                .addOnFailureListener {
                    // Логирование или обработка ошибки
                }
        }
    }

    private fun loadUserOrders(userId: String?) {
        if (userId != null) {
            databaseReference.child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ordersList.clear()
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        order?.let { ordersList.add(it) }
                    }
                    ordersList.sortByDescending { it.date }
                    orderAdapter.notifyDataSetChanged()
                    noOrdersTextView.visibility = if (ordersList.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибки
                }
            })
        }
    }
}
