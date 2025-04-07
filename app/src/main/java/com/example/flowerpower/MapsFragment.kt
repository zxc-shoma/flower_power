package com.example.flowerpower

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentAddress: String = "Адрес не найден" // Переменная для сохранения адреса

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)

        // Получаем фрагмент карты
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Инициализируем fusedLocationClient для получения местоположения пользователя
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val saveButton = view.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            // При нажатии на кнопку отправляем сохранённый адрес через Fragment Result API
            val resultBundle = Bundle().apply {
                putString("selectedAddress", currentAddress)
            }
            parentFragmentManager.setFragmentResult("addressRequestKey", resultBundle)
            parentFragmentManager.popBackStack()  // Возврат к предыдущему фрагменту
        }
        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Проверяем разрешение
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            setMapToDefaultLocation()
            return
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        // Проверяем наличие разрешения
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Разрешения нет — выходим или запрашиваем его
            return
        }

        // Создаем запрос на обновление местоположения
        val locationRequest = LocationRequest.create().apply {
            interval = 50000          // Интервал обновления – каждые 5 секунд
            fastestInterval = 3000       // Минимальный интервал обновления
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val newLatLng = LatLng(location.latitude, location.longitude)

                // Обновляем камеру карты
                val cameraPosition = CameraPosition.Builder()
                    .target(newLatLng)
                    .zoom(16f)
                    .tilt(30f)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                // Очищаем карту и добавляем новый маркер
                map.clear()
                val markerOptions = MarkerOptions()
                    .position(newLatLng)
                    .title("Мое местоположение")
                    .draggable(true)
                map.addMarker(markerOptions)

                // Обновляем текущий адрес с помощью Geocoder
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                currentAddress = addresses?.firstOrNull()?.getAddressLine(0) ?: "Адрес не найден"

                // Устанавливаем слушатель для перетаскивания маркера
                map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                    override fun onMarkerDragStart(marker: com.google.android.gms.maps.model.Marker) {}
                    override fun onMarkerDrag(marker: com.google.android.gms.maps.model.Marker) {}
                    override fun onMarkerDragEnd(marker: com.google.android.gms.maps.model.Marker) {
                        val latLng = marker.position
                        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                        currentAddress = addresses?.firstOrNull()?.getAddressLine(0) ?: "Адрес не найден"
                    }
                })
            }
        }

        // Оборачиваем вызов в try-catch для явной обработки SecurityException
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Логируем или обрабатываем ошибку, если разрешение вдруг исчезнет
            e.printStackTrace()
        }
    }


    // Метод для установки точки по умолчанию (если нет разрешения или местоположение не найдено)
    private fun setMapToDefaultLocation() {
        val defaultLatLng = LatLng(54.735152, 55.958736) // Пример: Уфа
        val cameraPosition = CameraPosition.Builder()
            .target(defaultLatLng)
            .zoom(16f)
            .tilt(30f)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        val markerOptions = MarkerOptions()
            .position(defaultLatLng)
            .title("Уфа")
            .draggable(true)
        map.addMarker(markerOptions)
    }

    override fun onPause() {
        super.onPause()
        // Останавливаем обновления местоположения, чтобы не расходовать ресурсы
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
