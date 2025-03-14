package com.example.riomarappnav.logicadeinterface

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.riomarappnav.R
import com.example.riomarappnav.ThemePreferenceManager
import com.example.riomarappnav.database.FirestoreRepository
import com.example.riomarappnav.logicadeinterface.camerapred.CameraActivity
import com.example.riomarappnav.logicadeinterface.editInfo.EditInfoActivity
import com.example.riomarappnav.logicadeinterface.help.HelpActivity
import com.example.riomarappnav.logicadeinterface.telaRanking.RankingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class SettingsActivity : AppCompatActivity() {

    private lateinit var themeManager: ThemePreferenceManager
    private lateinit var tvWelcome: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var firestoreRepository: FirestoreRepository

    // Em outra parte do código, se o nome já estiver salvo, você pode recuperá-lo:
    private fun recuperarNomeLocal(): String? {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getString("usuario_nome", null)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        themeManager = ThemePreferenceManager(this)
        firestoreRepository = FirestoreRepository()

        // Atualiza e salva o nome recuperado do Firestore
        firestoreRepository.buscarNomeUsuario { nome ->
            // Atualiza a UI
            tvWelcome.text = "Olá, ${nome ?: "usuário"}!"

            // Salva nas SharedPreferences para uso futuro
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE).edit()
            sharedPreferences.putString("usuario_nome", nome)
            sharedPreferences.apply()
        }

        val darkModeSwitch = findViewById<Switch>(R.id.switchDarkMode)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvWelcome.text = "Olá, ${recuperarNomeLocal()}!"
        ivProfilePicture = findViewById(R.id.ivProfilePicture)

        firestoreRepository.buscarImagemUsuario { profileImageUrl ->
            Glide.with(this)
                .load(profileImageUrl)
                .transform(CircleCrop())
                .into(ivProfilePicture)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.bottom_profile
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.bottom_search -> {
                    startActivity(Intent(applicationContext, CameraActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.bottom_settings -> {
                    startActivity(Intent(applicationContext, RankingActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.bottom_profile -> return@setOnItemSelectedListener true
            }
            false
        }

        val etSearchSettings = findViewById<EditText>(R.id.etSearchSettings)
        etSearchSettings.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                if (query.isEmpty()) {
                    findViewById<View>(R.id.llEditInfo).visibility = View.VISIBLE
                    findViewById<View>(R.id.llTema).visibility = View.VISIBLE
                    findViewById<View>(R.id.llPermissoes).visibility = View.VISIBLE
                    findViewById<View>(R.id.llAjuda).visibility = View.VISIBLE
                    findViewById<View>(R.id.llSobre).visibility = View.VISIBLE
                } else {
                    findViewById<View>(R.id.llEditInfo).visibility =
                        if ("editar" in query) View.VISIBLE else View.GONE
                    findViewById<View>(R.id.llTema).visibility =
                        if ("tema" in query) View.VISIBLE else View.GONE
                    findViewById<View>(R.id.llPermissoes).visibility =
                        if ("permissões" in query) View.VISIBLE else View.GONE
                    findViewById<View>(R.id.llAjuda).visibility =
                        if ("ajuda" in query) View.VISIBLE else View.GONE
                    findViewById<View>(R.id.llSobre).visibility =
                        if ("sobre" in query) View.VISIBLE else View.GONE
                }
            }
        })

        lifecycleScope.launch {
            val isDarkMode = themeManager.isDarkModeEnabled.first() ?: false
            darkModeSwitch.isChecked = isDarkMode
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                themeManager.setDarkMode(isChecked)
                etSearchSettings.setText("")
                // Reinicia a Activity sem animacoes para evitar flickering
                overridePendingTransition(0, 0)
                finish()
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }

        findViewById<View>(R.id.llEditInfo).setOnClickListener {
            startActivity(Intent(this, EditInfoActivity::class.java))
        }

        findViewById<View>(R.id.llPermissoes).setOnClickListener {
            startActivity(Intent(this, PermissionActivity::class.java))
        }

        findViewById<View>(R.id.llSobre).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        findViewById<View>(R.id.llAjuda).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
    }

}


