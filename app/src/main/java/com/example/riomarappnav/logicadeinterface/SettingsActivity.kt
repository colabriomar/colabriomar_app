package com.example.riomarappnav.logicadeinterface

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.riomarappnav.R
import com.example.riomarappnav.ThemePreferenceManager
import com.example.riomarappnav.database.FirestoreRepository
import com.example.riomarappnav.logicadeinterface.camerapred.CameraActivity
import com.example.riomarappnav.logicadeinterface.editInfo.EditInfoActivity
import com.example.riomarappnav.logicadeinterface.help.HelpActivity
import com.example.riomarappnav.logicadeinterface.telaRanking.RankingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {

    private lateinit var themeManager: ThemePreferenceManager
    private lateinit var tvWelcome: TextView
    private lateinit var firestoreRepository: FirestoreRepository

    private fun recuperarNomeLocal(): String? {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getString("usuario_nome", null)
    }

    // Função auxiliar para lidar com a depreciação do overridePendingTransition
    private fun compatOverridePendingTransition(enterAnim: Int, exitAnim: Int) {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(enterAnim, exitAnim)
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        themeManager = ThemePreferenceManager(this)
        val isDarkMode = themeManager.isDarkModeForced

        if (isDarkMode) {
            setTheme(R.style.Theme_RioMarAppNav_Dark)
        } else {
            setTheme(R.style.Theme_RioMarAppNav)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        firestoreRepository = FirestoreRepository()

        // Referências das views
        val darkModeSwitch = findViewById<Switch>(R.id.switchDarkMode)
        tvWelcome = findViewById(R.id.tvWelcome)

        // Correção: Uso de String Resource com placeholder para evitar concatenação direta
        val nomeLocal = recuperarNomeLocal() ?: "usuário"
        tvWelcome.text = getString(R.string.welcome_user, nomeLocal)

        // Removido: Lógica do Glide e findViewById(R.id.ivProfilePicture)

        // Atualiza o nome do usuário do Firestore (se houver mudança remota)
        firestoreRepository.buscarNomeUsuario { nome ->
            val nomeAtualizado = nome ?: "usuário"
            // Atualiza a UI usando o recurso de string
            tvWelcome.text = getString(R.string.welcome_user, nomeAtualizado)

            getSharedPreferences("AppPreferences", MODE_PRIVATE).edit {
                putString("usuario_nome", nome)
            }
        }

        // BottomNavigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.bottom_profile
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.bottom_search -> {
                    startActivity(Intent(applicationContext, CameraActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.bottom_settings -> {
                    startActivity(Intent(applicationContext, RankingActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.bottom_profile -> return@setOnItemSelectedListener true
            }
            false
        }

        val etSearchSettings = findViewById<EditText>(R.id.etSearchSettings)
        etSearchSettings.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                // Mantive a lógica de visibilidade original
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

        darkModeSwitch.isChecked = isDarkMode

        darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            themeManager.setDarkMode(checked)
            recreate()
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