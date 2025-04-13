package com.example.riomarappnav.logicadeinterface.editInfo

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.riomarappnav.database.FirestoreRepository
import com.example.riomarappnav.databinding.ActivityEditInfoBinding
import com.example.riomarappnav.utils.BaseActivity

class EditInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityEditInfoBinding
    private lateinit var firestoreRepository: FirestoreRepository

    // Registrar o seletor de imagem
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Exibe a imagem selecionada
            binding.ivEditProfilePicture.setImageURI(uri)
            // Inicia o upload da imagem com redimensionamento
            firestoreRepository.uploadProfileImage(uri, this) { success ->
                if (success) {
                    Toast.makeText(this, "Imagem enviada com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Falha ao enviar imagem.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Nenhuma imagem foi selecionada.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o repositório do Firebase
        firestoreRepository = FirestoreRepository()

        // Botão para selecionar nova imagem
        binding.btnChangePicture.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

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
        sharedPref.edit().putString("userName", name).apply()
    }
}
