package com.example.riomarappnav.logicadeinterface.editInfo

import android.os.Bundle
import android.widget.Toast
import com.example.riomarappnav.database.FirestoreRepository
import com.example.riomarappnav.databinding.ActivityEditInfoBinding
import com.example.riomarappnav.utils.BaseActivity
import androidx.core.content.edit

class EditInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityEditInfoBinding
    private lateinit var firestoreRepository: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o repositório do Firebase
        firestoreRepository = FirestoreRepository()

        binding.btnSave.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateUserName(newName)
            } else {
                Toast.makeText(this, "O nome não pode estar vazio!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método para atualizar o nome do usuário no Firebase
    private fun updateUserName(name: String) {
        firestoreRepository.atualizarNomeUsuario(name) { sucesso ->
            if (sucesso) {
                saveUserInfoLocally(name)
                Toast.makeText(this, "Nome salvo com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Erro ao salvar nome.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Salva o nome localmente (caso seja necessário)
    private fun saveUserInfoLocally(name: String) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPref.edit { putString("userName", name) }
    }
}
