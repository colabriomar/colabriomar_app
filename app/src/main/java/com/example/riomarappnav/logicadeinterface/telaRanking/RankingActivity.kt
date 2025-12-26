package com.example.riomarappnav.logicadeinterface.telaRanking

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.riomarappnav.R
import com.example.riomarappnav.database.FirestoreRepository
import com.example.riomarappnav.logicadeinterface.HomeActivity
import com.example.riomarappnav.logicadeinterface.SettingsActivity
import com.example.riomarappnav.logicadeinterface.camerapred.CameraActivity
import com.example.riomarappnav.logicadeinterface.telaMapa.MapaActivity
import com.example.riomarappnav.utils.BaseActivity
import com.google.android.material.bottomnavigation.BottomNavigationView


@Suppress("DEPRECATION")
class RankingActivity : BaseActivity() {
    private lateinit var rankingAdapter: RankingAdapter
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        val recyclerView: RecyclerView = findViewById(R.id.rvRankingList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val firestoreRepository = FirestoreRepository ()
        firestoreRepository.fetchUserData { userDataList ->
            // Ordena os dados pelos troféus em ordem decrescente
            val sortedList = userDataList.sortedByDescending { it.trophies }.take(20)

            rankingAdapter = RankingAdapter(sortedList)
            recyclerView.adapter = rankingAdapter
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.bottom_settings
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(
                        Intent(
                            applicationContext,
                            HomeActivity::class.java
                        )
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }

                R.id.bottom_search -> {
                    startActivity(
                        Intent(
                            applicationContext,
                            CameraActivity::class.java
                        )
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }

                R.id.bottom_map -> {
                    startActivity(
                        Intent(
                            applicationContext,
                            MapaActivity::class.java
                        )
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }

                R.id.bottom_settings -> return@setOnItemSelectedListener true
                R.id.bottom_profile -> {
                    startActivity(
                        Intent(
                            applicationContext,
                            SettingsActivity::class.java
                        )
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }
}
