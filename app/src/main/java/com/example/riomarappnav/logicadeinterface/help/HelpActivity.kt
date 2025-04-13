package com.example.riomarappnav.logicadeinterface.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.riomarappnav.databinding.ActivityHelpBinding
import com.example.riomarappnav.utils.BaseActivity

class HelpActivity : BaseActivity() {

    private lateinit var binding: ActivityHelpBinding
    private var attachedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            Toast.makeText(this, "Imagem anexada com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nenhuma imagem foi selecionada.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAttachImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {
            val title = binding.etProblemTitle.text.toString()
            val description = binding.etProblemDescription.text.toString()

            if (title.isNotBlank() && description.isNotBlank()) {
                sendHelpRequest(title, description, attachedImageUri)
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendHelpRequest(title: String, description: String, imageUri: Uri?) {
        val recipient = "gabrielset20@outlook.com"
        val subject = "Formulário de Ajuda: $title"
        val body = "Título do Problema: $title\n\nDescrição:\n$description"

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (imageUri != null) "message/rfc822" else "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)

            // Anexar imagem, se houver
            imageUri?.let {
                putExtra(Intent.EXTRA_STREAM, it)
            }
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Enviar formulário via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível enviar o e-mail. Verifique seu aplicativo de e-mail.", Toast.LENGTH_SHORT).show()
        }
    }
}