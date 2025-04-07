
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.flowerpower.CartManager
import com.example.flowerpower.Flower
import com.example.flowerpower.FlowerAdapter
import com.example.flowerpower.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var flowerAdapter: FlowerAdapter
    private lateinit var flowersList: MutableList<Flower>
    private lateinit var originalFlowersList: MutableList<Flower>
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var seasonSpinner: Spinner
    private lateinit var priceSortSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchView = view.findViewById(R.id.searchView)
        seasonSpinner = view.findViewById(R.id.seasonSpinner)
        priceSortSpinner = view.findViewById(R.id.priceSortSpinner)

        flowersList = mutableListOf()
        originalFlowersList = mutableListOf()
        flowerAdapter = FlowerAdapter(flowersList, { flower ->
            CartManager.addToCart(flower)
            Toast.makeText(requireContext(), "${flower.name} добавлен в корзину", Toast.LENGTH_SHORT).show()
        }, { flower ->
            showFlowerDescriptionDialog(flower)
        })
        recyclerView.adapter = flowerAdapter

        databaseReference = FirebaseDatabase.getInstance("https://flower-power-d39c0-default-rtdb.europe-west1.firebasedatabase.app").getReference("flowers")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                originalFlowersList.clear()
                for (flowerSnapshot in snapshot.children) {
                    val flower = flowerSnapshot.getValue(Flower::class.java)
                    flower?.let { originalFlowersList.add(it) }
                }
                filterAndSortFlowers()
                Log.d("HomeFragment", "Data loaded: ${originalFlowersList.size} items")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Database error: ${error.message}")
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterAndSortFlowers()
                return true
            }
        })

        val seasonAdapter = ArrayAdapter(
            requireContext(),
            R.layout.custom_spinner_item,
            resources.getStringArray(R.array.seasons)
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.custom_spinner_item)
            seasonSpinner.adapter = adapter
        }
        seasonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterAndSortFlowers()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val priceSortAdapter = ArrayAdapter(
            requireContext(),
            R.layout.custom_spinner_item,
            resources.getStringArray(R.array.price_sort_options)
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.custom_spinner_item)
            priceSortSpinner.adapter = adapter
        }
        priceSortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterAndSortFlowers()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterAndSortFlowers() {
        val query = searchView.query.toString().lowercase()
        val selectedSeason = seasonSpinner.selectedItem.toString()
        val selectedPriceSort = priceSortSpinner.selectedItem.toString()

        flowersList = originalFlowersList.filter { flower ->
            val matchesQuery = flower.name.lowercase().contains(query)
            val matchesSeason = selectedSeason == "Сезон" || flower.season == selectedSeason
            matchesQuery && matchesSeason
        }.toMutableList()

        when (selectedPriceSort) {
            "Сортировка" -> flowersList
            "по возрастанию" -> flowersList.sortBy { it.price }
            "по убыванию" -> flowersList.sortByDescending { it.price }
        }

        flowerAdapter.updateList(flowersList)
    }

    private fun showFlowerDescriptionDialog(flower: Flower) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_flower_description, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val flowerImageView: ImageView = dialogView.findViewById(R.id.flowerImageView)
        val flowerNameTextView: TextView = dialogView.findViewById(R.id.flowerNameTextView)
        val flowerDescriptionTextView: TextView = dialogView.findViewById(R.id.flowerDescriptionTextView)
        val closeButton: Button = dialogView.findViewById(R.id.closeButton)

        // Загружаем данные
        flowerNameTextView.text = flower.name
        flowerDescriptionTextView.text = flower.description

        // Загрузка изображения с помощью Glide (убедись, что библиотека Glide добавлена)
        Glide.with(requireContext())
            .load(flower.photoUrl)
            .placeholder(R.drawable.placeholder_image) // заглушка
            .error(R.drawable.error_image) // если ошибка загрузки
            .into(flowerImageView)

        // Закрытие диалога
        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

}
