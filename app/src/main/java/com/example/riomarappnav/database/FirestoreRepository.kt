package com.example.riomarappnav.database

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private companion object {
        private const val TAG = "FirestoreRepository"
    }

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
            .addOnFailureListener { error ->
                Log.e(TAG, "Falha ao verificar documento do usuario", error)
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

        db.runTransaction { transaction ->
            val snapshot = transaction.get(trofeusRef)
            if (snapshot.exists()) {
                transaction.update(trofeusRef, "nomeUser", novoNome)
            } else {
                val novoDocumento = mapOf(
                    "userId" to userId,
                    "nomeUser" to novoNome,
                    "trofeus" to 0
                )
                transaction.set(trofeusRef, novoDocumento)
            }
        }
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
            .addOnFailureListener { error ->
                Log.e(TAG, "Falha ao buscar nome do usuario", error)
                onResult(null) // Retorna nulo em caso de erro
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
            .addOnFailureListener { error ->
                Log.e(TAG, "Falha ao buscar ranking", error)
                onResult(emptyList())
            }
    }

    fun fetchPontosDeInteresse(onResult: (List<PontoDeInteresse>) -> Unit) {
        db.collection("pontosDeInteresse")
            .limit(100)
            .get()
            .addOnSuccessListener { result ->
                val pontos = mutableListOf<PontoDeInteresse>()
                for (document in result) {
                    val localizacao = document.getGeoPoint("localizacao") ?: continue
                    val predicoesRaw = document.get("predicoes") as? List<*>
                    val predicoes = predicoesRaw
                        ?.mapNotNull { it as? String }
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?: emptyList()
                    pontos.add(PontoDeInteresse(localizacao, predicoes))
                }
                onResult(pontos)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Falha ao buscar pontos de interesse", error)
                onResult(emptyList())
            }
    }

    data class UserData(val name: String, val trophies: Int, val profileImageUrl: String)
    data class PontoDeInteresse(val localizacao: GeoPoint, val predicoes: List<String>)
}
