package com.example.riomarappnav.logicadeinterface.camerapred

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.riomarappnav.R
import com.example.riomarappnav.database.FirestoreRepository
import com.example.riomarappnav.databinding.ActivityCameraBinding
import com.example.riomarappnav.logicadeinterface.HomeActivity
import com.example.riomarappnav.logicadeinterface.PermissionActivity
import com.example.riomarappnav.logicadeinterface.SettingsActivity
import com.example.riomarappnav.logicadeinterface.telaMapa.MapaActivity
import com.example.riomarappnav.logicadeinterface.telaRanking.RankingActivity
import com.example.riomarappnav.modelYolov8n.BoundingBox
import com.example.riomarappnav.modelYolov8n.Constants.LABELS_PATH
import com.example.riomarappnav.modelYolov8n.Constants.MODEL_PATH
import com.example.riomarappnav.modelYolov8n.Detector
import com.example.riomarappnav.modelYolov8n.OverlayView
import com.example.riomarappnav.utils.BaseActivity
import com.example.riomarappnav.utils.TrophyGenerator
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.GeoPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Suppress("DEPRECATION")
class CameraActivity : BaseActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var previewView: PreviewView
    private lateinit var overlay: OverlayView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: Detector
    private lateinit var cameraButton: ImageButton
    private lateinit var top10predicoesDeClasseCompleta: List<String>
    private val firestoreRepository = FirestoreRepository()
    private lateinit var statusOverlay: View
    private lateinit var statusTitle: TextView
    private lateinit var statusMessage: TextView
    private lateinit var statusProgress: ProgressBar
    private val uiHandler = Handler(Looper.getMainLooper())
    private var instructionStepRunnable: Runnable? = null
    private var instructionHideRunnable: Runnable? = null
    private var statusHideRunnable: Runnable? = null
    private var captureTimeoutRunnable: Runnable? = null

    // VariÃ¡veis de localizaÃ§Ã£o
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var longi: Double? = null
    private var lati: Double? = null

    // Flag para indicar se a Activity estÃ¡ ativa (para evitar processamento em background)
    private var isActive: Boolean = true
    private var isCaptureInProgress: Boolean = false
    private var isSending: Boolean = false
    private val captureCooldownMs = 3000L
    private var lastCaptureAt = 0L
    private val captureTimeoutMs = 10_000L
    private var pendingDetections: List<String> = emptyList()
    private val analysisIntervalMs = 150L
    private var lastAnalysisAt = 0L

    @SuppressLint("MissingPermission", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        top10predicoesDeClasseCompleta = emptyList()

        // Inicializa o cliente de localizaÃ§Ã£o
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Inicializa os componentes da interface
        previewView = binding.previewView
        overlay = binding.overlay
        cameraButton = findViewById(R.id.capture_button)
        statusOverlay = binding.statusOverlay
        statusTitle = binding.statusTitle
        statusMessage = binding.statusMessage
        statusProgress = binding.statusProgress

        // Inicializa o detector do modelo Yolov8 (incluindo bounding boxes)
        try {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
            detector.setup()
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erro ao inicializar o detector: ${e.message}")
            Toast.makeText(this, "Falha ao inicializar o detector", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // ConfiguraÃ§Ã£o do BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.bottom_search
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_search -> true
                R.id.bottom_map -> {
                    startActivity(Intent(applicationContext, MapaActivity::class.java))
                    compatOverridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
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
        // Inicializa a cÃ¢mera e o executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
        showInstructionSequence()

        cameraButton.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - lastCaptureAt
            if (elapsed < captureCooldownMs) {
                val remainingSeconds = ((captureCooldownMs - elapsed) / 1000L) + 1L
                Toast.makeText(
                    this,
                    "Aguarde ${remainingSeconds}s para registrar novamente.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (isCaptureInProgress || isSending) {
                return@setOnClickListener
            }
            lastCaptureAt = now
            cameraButton.isEnabled = false
            startCaptureFlow()
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

    // Reinicializa recursos quando a Activity voltar ao primeiro plano
    override fun onResume() {
        super.onResume()
        if (!hasRequiredPermissions()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }
        // Reativa o processamento de imagens
        isActive = true
        // Se o executor foi encerrado, reinitialize-o
        if (cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        // Reinicia a cÃ¢mera
        startCamera()
    }

    // Pausa o processamento quando a Activity nǜo estiver em primeiro plano
    override fun onPause() {
        super.onPause()
        // Evita processamento desnecessǭrio em background
        isActive = false
        isCaptureInProgress = false
        isSending = false
        cancelCaptureTimeout()
        cancelInstructionSequence()
        statusHideRunnable?.let { uiHandler.removeCallbacks(it) }
        hideStatus()
        // Cancela tarefas pendentes
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdownNow()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImage(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraActivity", "Erro ao iniciar a camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (!isActive || !isCaptureInProgress) {
            imageProxy.close()
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalysisAt < analysisIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalysisAt = now
        val bitmap = imageProxy.toBitmap()
        try {
            detector.detect(bitmap, imageProxy.imageInfo.rotationDegrees)
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erro ao processar a imagem: ${e.message}")
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
        isCaptureInProgress = false
        isSending = false
        cancelCaptureTimeout()
        cancelInstructionSequence()
        statusHideRunnable?.let { uiHandler.removeCallbacks(it) }
        if (::detector.isInitialized) {
            detector.clear()
        }
        if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
            cameraExecutor.shutdownNow()
        }
    }


    override fun onEmptyDetect() {
        if (!isCaptureInProgress) {
            return
        }
        runOnUiThread {
            overlay.clear()
        }
        Log.d("CameraActivity", "Nenhuma detecÃ§Ã£o realizada.")
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        if (!isCaptureInProgress) {
            return
        }
        runOnUiThread {
            overlay.setResults(boundingBoxes)
        }
        Log.d("CameraActivity", "DetecÃ§Ãµes: $boundingBoxes, Tempo: $inferenceTime ms")
        val classNames: List<String> = boundingBoxes.map { it.clsName }
        Log.d("CameraActivity", "Class Names: $classNames")
        classNames.firstOrNull()?.let { Log.d("CameraActivity", "First Class Name: $it") }
        passaNumeroDeDeteccoesDeClasses(classNames)
        attemptFinalizeCapture()
    }

    private fun passaNumeroDeDeteccoesDeClasses(listaDeClasses: List<String>) {
        Log.d("CameraActivity", "Processando formulÃ¡rio com classes: $listaDeClasses")
        setTop10predicoesDeClasse(listaDeClasses)
    }

    private fun setTop10predicoesDeClasse(top10predicoesDeClasse: List<String>) {
        val normalizadas = top10predicoesDeClasse
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        top10predicoesDeClasseCompleta = normalizadas
        pendingDetections = normalizadas.take(10)
    }

    private fun getTop10predicoesDeClasses(): List<String> {
        return pendingDetections
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startCaptureFlow() {
        cancelInstructionSequence()
        pendingDetections = emptyList()
        top10predicoesDeClasseCompleta = emptyList()
        lati = null
        longi = null
        overlay.clear()
        isCaptureInProgress = true
        isSending = false
        lastAnalysisAt = 0L
        "Processando registro".showLoading("Aguarde enquanto analisamos e enviamos.")
        if (!isLocationEnabled()) {
            createLocationRequest()
        }
        requestCurrentLocationForCapture()
        scheduleCaptureTimeout()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestCurrentLocationForCapture() {
        if (!hasLocationPermission()) {
            return
        }
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                lati = location.latitude
                longi = location.longitude
                attemptFinalizeCapture()
            }
        }.addOnFailureListener {
            Log.e("CameraActivity", "Falha ao obter localizacao.")
        }
    }

    private fun attemptFinalizeCapture() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { attemptFinalizeCapture() }
            return
        }
        if (!isCaptureInProgress || isSending) {
            return
        }
        if (pendingDetections.isEmpty()) {
            return
        }
        val lat = lati
        val lon = longi
        if (lat == null || lon == null) {
            return
        }
        enviarRegistro(lat, lon)
    }

    private fun enviarRegistro(latitude: Double, longitude: Double) {
        isCaptureInProgress = false
        isSending = true
        cancelCaptureTimeout()
        val geopoint = GeoPoint(latitude, longitude)
        val listaDeClassesPreditas = getTop10predicoesDeClasses()
        val task = firestoreRepository.salvarPontoDeInteresse(geopoint, listaDeClassesPreditas)
        if (task == null) {
            handleCaptureError("Nao foi possivel registrar. Usuario nao autenticado.")
            return
        }
        task.addOnSuccessListener {
            premiarUsuario()
            "Registro enviado!".showSuccess("Obrigado por contribuir.")
            finishCaptureFlow()
        }.addOnFailureListener {
            handleCaptureError("Nao foi possivel registrar. Tente novamente.")
        }
    }

    private fun handleCaptureTimeout() {
        if (!isCaptureInProgress) {
            return
        }
        handleCaptureError("Nao foi possivel registrar. Verifique a localizacao e tente novamente.")
    }

    private fun handleCaptureError(message: String) {
        isCaptureInProgress = false
        isSending = false
        cancelCaptureTimeout()
        "Falha no registro".showError(message)
        finishCaptureFlow()
    }

    private fun finishCaptureFlow() {
        isCaptureInProgress = false
        isSending = false
        pendingDetections = emptyList()
        cameraButton.postDelayed({ cameraButton.isEnabled = true }, captureCooldownMs)
    }

    private fun showInstructionSequence() {
        showStatus("Aproxime-se do objeto", "Posicione o objeto e aguarde.", false)
        instructionStepRunnable = Runnable {
            showStatus("Pronto para registrar", "Toque no botao para enviar.", false)
        }
        instructionHideRunnable = Runnable { hideStatus() }
        uiHandler.postDelayed(instructionStepRunnable!!, 2500L)
        uiHandler.postDelayed(instructionHideRunnable!!, 5000L)
    }

    private fun String.showLoading(message: String) {
        showStatus(this, message, true)
    }

    private fun String.showError(message: String) {
        showStatus(this, message, false)
        scheduleStatusHide(2500L)
    }

    private fun String.showSuccess(message: String) {
        showStatus(this, message, false)
        scheduleStatusHide(2000L)
    }

    private fun showStatus(title: String, message: String, showProgress: Boolean) {
        statusHideRunnable?.let { uiHandler.removeCallbacks(it) }
        statusTitle.text = title
        statusMessage.text = message
        statusProgress.visibility = if (showProgress) View.VISIBLE else View.GONE
        statusOverlay.visibility = View.VISIBLE
    }

    private fun hideStatus() {
        if (!::statusOverlay.isInitialized || !::statusProgress.isInitialized) {
            return
        }
        statusOverlay.visibility = View.GONE
        statusProgress.visibility = View.GONE
    }

    private fun scheduleStatusHide(delayMs: Long) {
        statusHideRunnable?.let { uiHandler.removeCallbacks(it) }
        statusHideRunnable = Runnable { hideStatus() }
        uiHandler.postDelayed(statusHideRunnable!!, delayMs)
    }

    private fun cancelInstructionSequence() {
        instructionStepRunnable?.let { uiHandler.removeCallbacks(it) }
        instructionHideRunnable?.let { uiHandler.removeCallbacks(it) }
        instructionStepRunnable = null
        instructionHideRunnable = null
    }

    private fun scheduleCaptureTimeout() {
        cancelCaptureTimeout()
        captureTimeoutRunnable = Runnable { handleCaptureTimeout() }
        uiHandler.postDelayed(captureTimeoutRunnable!!, captureTimeoutMs)
    }

    private fun cancelCaptureTimeout() {
        captureTimeoutRunnable?.let { uiHandler.removeCallbacks(it) }
        captureTimeoutRunnable = null
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return hasCamera && hasLocationPermission()
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    private fun premiarUsuario() {
        val firestoreRepository = FirestoreRepository()
        val trophyGenerator = TrophyGenerator(firestoreRepository)
        firestoreRepository.buscarNomeUsuario { nomeUsuario ->
            if (nomeUsuario != null) {
                println("Nome do usuÃ¡rio: $nomeUsuario")
            } else {
                println("NÃ£o foi possÃ­vel obter o nome do usuÃ¡rio.")
            }
        }
        val nomeLocal = getNomeLocal() ?: "usuario"
        trophyGenerator.gerenciarTrofeus(nomeLocal, getTop10predicoesDeClasses()) { sucesso ->
            if (sucesso) {
                Log.d("TrophyGenerator", "TrofÃ©us gerenciados com sucesso!")
            } else {
                Log.e("TrophyGenerator", "Falha ao gerenciar os trofÃ©us.")
            }
        }
    }

    private fun getNomeLocal(): String? {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getString("usuario_nome", null)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ServiceCast")
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return try {
            locationManager.isLocationEnabled
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).setMinUpdateIntervalMillis(5000).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            Toast.makeText(this, "acesso a localizacao permitido", Toast.LENGTH_LONG).show()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this, 100)
                } catch (sendEx: Exception) {
                    sendEx.printStackTrace()
                }
            }
        }
    }
}





