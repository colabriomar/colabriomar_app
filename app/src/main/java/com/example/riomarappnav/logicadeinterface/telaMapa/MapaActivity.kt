package com.example.riomarappnav.logicadeinterface.telaMapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.riomarappnav.R
import com.example.riomarappnav.database.FirestoreRepository
import com.example.riomarappnav.logicadeinterface.HomeActivity
import com.example.riomarappnav.logicadeinterface.SettingsActivity
import com.example.riomarappnav.logicadeinterface.camerapred.CameraActivity
import com.example.riomarappnav.logicadeinterface.telaRanking.RankingActivity
import com.example.riomarappnav.utils.BaseActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapaActivity : BaseActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationClient: FusedLocationProviderClient
    private val firestoreRepository = FirestoreRepository()
    private val defaultCenter = GeoPoint(-3.130369, -60.023794)
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                centerOnCurrentLocation()
            } else {
                centerOnFallback()
            }
        }

    private fun compatOverridePendingTransition(enterAnim: Int, exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(enterAnim, exitAnim)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_mapa)

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        centerOnFallback()
        requestLocationIfNeeded()

        firestoreRepository.fetchPontosDeInteresse { pontos ->
            plotarPontos(pontos)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.bottom_map
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_search -> {
                    startActivity(Intent(applicationContext, CameraActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_map -> true
                R.id.bottom_settings -> {
                    startActivity(Intent(applicationContext, RankingActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_profile -> {
                    startActivity(Intent(applicationContext, SettingsActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun plotarPontos(pontos: List<FirestoreRepository.PontoDeInteresse>) {
        val pontosLimitados = pontos.take(100)
        if (pontosLimitados.isEmpty()) {
            return
        }

        mapView.overlays.removeAll { it is Marker }
        for (ponto in pontosLimitados) {
            val latLng = GeoPoint(ponto.localizacao.latitude, ponto.localizacao.longitude)
            adicionarMarker(ponto, latLng)
        }
        mapView.invalidate()
    }

    private fun adicionarMarker(ponto: FirestoreRepository.PontoDeInteresse, latLng: GeoPoint) {
        val materiais = ponto.predicoes.distinct().joinToString(", ")
        val snippet = if (materiais.isBlank()) {
            "Materiais: sem dados"
        } else {
            "Materiais: $materiais"
        }
        val marker = Marker(mapView)
        marker.position = latLng
        marker.title = "Ponto"
        marker.snippet = snippet
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
    }

    private fun requestLocationIfNeeded() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            centerOnCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun centerOnCurrentLocation() {
        locationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    centerOnFallback()
                    return@addOnSuccessListener
                }
                val current = GeoPoint(location.latitude, location.longitude)
                mapView.controller.setCenter(current)
                mapView.controller.setZoom(13.5)
            }
            .addOnFailureListener {
                centerOnFallback()
            }
    }

    private fun centerOnFallback() {
        mapView.controller.setCenter(defaultCenter)
        mapView.controller.setZoom(11.0)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }
}
