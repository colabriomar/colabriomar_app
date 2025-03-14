package com.example.riomarappnav.database

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

@Suppress("DEPRECATION")
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Obtém o ID do usuário autenticado
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Salva um ponto de interesse no Firestore.
     * @param localizacao Localização (GeoPoint) do ponto de interesse.
     * @param predicoes Lista de previsões relacionadas ao ponto de interesse.
     * @return Task<Void> para monitorar o sucesso ou falha da operação.
     */
    fun salvarPontoDeInteresse(localizacao: GeoPoint, predicoes: List<String>): Task<Void>? {
        val userId = getCurrentUserId() ?: return null

        val pontoDeInteresse = mapOf(
            "userid" to userId,
            "localizacao" to localizacao,
            "predicoes" to predicoes
        )

        return db.collection("pontosDeInteresse")
            .document() // Gera um novo documento com ID automático
            .set(pontoDeInteresse)
    }

    /**
     * Atualiza ou cria os troféus do usuário.
     * @param trofeus Número total de troféus do usuário.
     * @return Task<Void> para monitorar o sucesso ou falha da operação.
     */
    fun salvarTrofeusUsuario(trofeus: Int, nomeUser: String): Task<Void>? {
        val userId = getCurrentUserId() ?: return null
        val trofeuUsuario = mapOf(
            "userId" to userId,
            "nomeUser" to nomeUser,
            "trofeus" to trofeus
        )
        return db.collection("trofeusUsuario")
            .document(userId) // Usa o userId como ID do documento
            .set(trofeuUsuario)
    }

    fun verificarDocumentoUsuario(onResult: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onResult(false) // Retorna falso se não houver usuário autenticado
            return
        }

        val trofeusRef = db.collection("trofeusUsuario").document(userId)

        trofeusRef.get()
            .addOnSuccessListener { document ->
                onResult(document.exists()) // Retorna true se o documento existir
            }
            .addOnFailureListener {
                onResult(false) // Retorna falso em caso de falha ao verificar
            }
    }

    /**
     * Atualiza apenas os troféus do usuário.
     * @param novosTrofeus Número de troféus a adicionar.
     * @param onResult Callback que retorna sucesso ou falha da operação.
     */
    fun incrementarTrofeus(novosTrofeus: Int, onResult: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onResult(false)

        val trofeusRef = db.collection("trofeusUsuario").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(trofeusRef)
            val trofeusAtuais = snapshot.getLong("trofeus")?.toInt() ?: 0
            val trofeusAtualizados = trofeusAtuais + novosTrofeus

            // Atualiza apenas o campo "trofeus"
            transaction.update(trofeusRef, "trofeus", trofeusAtualizados)
        }.addOnSuccessListener {
            onResult(true) // Operação bem-sucedida
        }.addOnFailureListener {
            onResult(false) // Falha na operação
        }
    }

    /**
     * Atualiza o nome do usuário na coleção trofeusUsuario.
     * @param novoNome Novo nome do usuário.
     * @param onResult Callback que retorna sucesso ou falha da operação.
     */
    fun atualizarNomeUsuario(novoNome: String, onResult: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onResult(false) // Falha se o usuário não estiver autenticado
            return
        }

        val trofeusRef = db.collection("trofeusUsuario").document(userId)

        trofeusRef.update("nomeUser", novoNome)
            .addOnSuccessListener {
                onResult(true) // Sucesso na atualização
            }
            .addOnFailureListener {
                onResult(false) // Falha na atualização
            }
    }

    /**
     * Consulta o nome do usuário na tabela trofeusUsuario.
     * @param onResult Callback que retorna o nome do usuário ou nulo em caso de falha.
     */
    fun buscarNomeUsuario(onResult: (String?) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onResult(null) // Retorna nulo se o usuário não estiver autenticado
            return
        }
        val trofeusRef = db.collection("trofeusUsuario").document(userId)
        trofeusRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nomeUsuario = document.getString("nomeUser")
                    onResult(nomeUsuario) // Retorna o nome do usuário
                } else {
                    onResult(null) // Retorna nulo se o documento não existir
                }
            }
            .addOnFailureListener {
                onResult(null) // Retorna nulo em caso de erro
            }
    }

    /**
     * Atualiza a imagem de perfil do usuário no Firestore.
     * @param imageUrl URL da imagem de perfil.
     * @param onResult Callback que retorna sucesso (true) ou falha (false) da operação.
     */
    private fun atualizarImagemUsuario(imageUrl: String, onResult: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onResult(false)
            return
        }

        val userRef = db.collection("trofeusUsuario").document(userId)
        val updates = mapOf("profileImageUrl" to imageUrl)
        userRef.update(updates)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Busca a URL da imagem de perfil do usuário no Firestore.
     * Se o usuário não tiver uma imagem cadastrada, retorna uma URL padrão.
     * @param onResult Callback que retorna a URL da imagem.
     */
    fun buscarImagemUsuario(onResult: (String) -> Unit) {
        val defaultImageUrl = "https://as2.ftcdn.net/v2/jpg/05/60/44/93/1000_F_560449312_I6FTJ1HWythaLutWDMuHvJy2IoCv20iJ.jpg"
        val userId = getCurrentUserId()
        if (userId == null) {
            onResult(defaultImageUrl)
            return
        }

        val userRef = db.collection("trofeusUsuario").document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                val url = document.getString("profileImageUrl")
                if (!url.isNullOrEmpty()) {
                    onResult(url)
                } else {
                    onResult(defaultImageUrl)
                }
            }
            .addOnFailureListener {
                onResult(defaultImageUrl)
            }
    }

    fun uploadProfileImage(uri: Uri, context: Context, onResult: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onResult(false)
            return
        }

        try {
            // Obtém o bitmap da imagem a partir do URI
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            // Redimensiona a imagem para 200x200 pixels
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            // Comprime a imagem em JPEG (qualidade 80%)
            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageData = baos.toByteArray()

            // Define a referência de armazenamento no Firebase Storage
            val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId.jpg")
            storageRef.putBytes(imageData)
                .addOnSuccessListener {
                    // Após o upload, recupera a URL de download
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        atualizarImagemUsuario(downloadUri.toString()) { success ->
                            onResult(success)
                        }
                    }
                }
                .addOnFailureListener {
                    onResult(false)
                }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun fetchUserData(onResult: (List<UserData>) -> Unit) {
        val defaultImageUrl = "https://as2.ftcdn.net/v2/jpg/05/60/44/93/1000_F_560449312_I6FTJ1HWythaLutWDMuHvJy2IoCv20iJ.jpg"
        db.collection("trofeusUsuario")
            .orderBy("trofeus", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { result ->
                val userList = mutableListOf<UserData>()
                for (document in result) {
                    val name = document.getString("nomeUser") ?: "Unknown"
                    val trophies = document.getLong("trofeus")?.toInt() ?: 0
                    val profileImageUrl = document.getString("profileImageUrl") ?: defaultImageUrl
                    userList.add(UserData(name, trophies, profileImageUrl))
                }
                onResult(userList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
    data class UserData(val name: String, val trophies: Int, val profileImageUrl: String)
}
