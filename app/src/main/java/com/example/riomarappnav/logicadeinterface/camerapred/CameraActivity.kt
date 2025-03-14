package com.example.riomarappnav.logicadeinterface.camerapred

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
import com.example.riomarappnav.logicadeinterface.SettingsActivity
import com.example.riomarappnav.logicadeinterface.telaRanking.RankingActivity
import com.example.riomarappnav.modelYolov8n.BoundingBox
import com.example.riomarappnav.modelYolov8n.Constants.LABELS_PATH
import com.example.riomarappnav.modelYolov8n.Constants.MODEL_PATH
import com.example.riomarappnav.modelYolov8n.Detector
import com.example.riomarappnav.modelYolov8n.OverlayView
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
class CameraActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var previewView: PreviewView
    private lateinit var overlay: OverlayView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: Detector
    private lateinit var cameraButton: ImageButton
    private lateinit var top10predicoesDeClasseCompleta: List<String>

    // Variáveis de localização
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var localizacaoAtual: String
    private lateinit var longi: Number
    private lateinit var lati: Number

    //a localizacao parece funcionar corretamente no celular, entao ve somente se nao estao sendo passados numeros nulos pro firebase
    //refatorar essa bomba ai pae

    @SuppressLint("MissingPermission", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        top10predicoesDeClasseCompleta = listOf(" ")

        // Inicializa o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(
                    Manifest.permission.ACCESS_FINE_LOCATION, false
                ) || permissions.getOrDefault(
                    Manifest.permission.ACCESS_COARSE_LOCATION, false
                ) -> {
                    Toast.makeText(this, "acesso a localizacao permitido", Toast.LENGTH_LONG).show()

                    if (isLocationEnabled()) {
                        val result = fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            CancellationTokenSource().token
                        )
                        result.addOnCompleteListener {
                            if (it.result != null) {
                                val location = "Latitude: ${it.result.latitude}\nLongitude: ${it.result.longitude}"
                                localizacaoAtual = location
                                lati = it.result.latitude
                                longi = it.result.longitude
                            } else {
                                Toast.makeText(this, "Falha ao obter a localização", Toast.LENGTH_LONG).show()
                                // Atribuindo valores padrão
                                lati = 0.0
                                longi = 0.0
                            }
                        }
                    } else {
                        Toast.makeText(this, "Por favor, ligue a localizacao.", Toast.LENGTH_LONG).show()
                        createLocationRequest()
                    }
                }
                else -> {
                    Toast.makeText(this, "acesso a localizacao negado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Inicializa os componentes da interface
        previewView = binding.previewView
        overlay = binding.overlay
        cameraButton = findViewById(R.id.capture_button)

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
        // Configuração do BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.bottom_search
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_search -> true
                R.id.bottom_settings -> {
                    startActivity(Intent(applicationContext, RankingActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_profile -> {
                    startActivity(Intent(applicationContext, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }
        // Inicializa a câmera
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        cameraButton.setOnClickListener {
            botaoTirarFotoQueDisparaOEnvioDoFormProFirebase()
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
                Log.e("CameraActivity", "Erro ao iniciar a câmera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap() // Função de extensão para converter ImageProxy para Bitmap
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            imageProxy.imageInfo.rotationDegrees.toMatrix(), true
        )
        try{
            detector.detect(rotatedBitmap)
        } catch (e: Exception) {
            Log.e("CameraActivity", "Erro ao processar a imagem: ${e.message}")
        }
        imageProxy.close()
    }

    // Extensão para converter graus em Matrix para rotação da imagem
    private fun Int.toMatrix(): Matrix {
        return Matrix().apply { postRotate(this@toMatrix.toFloat()) }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            overlay.clear()
        }
        Log.d("CameraActivity", "Nenhuma detecção realizada.")
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            overlay.setResults(boundingBoxes)
        }
        Log.d("CameraActivity", "Detecções: $boundingBoxes, Tempo: $inferenceTime ms")
        val classNames: List<String> = boundingBoxes.map { it.clsName }
        Log.d("CameraActivity", "Class Names: $classNames")
        classNames.firstOrNull()?.let { Log.d("CameraActivity", "First Class Name: $it") }
        passaNumeroDeDeteccoesDeClasses(classNames)
    }

    private fun passaNumeroDeDeteccoesDeClasses(listaDeClasses: List<String>) {
        Log.d("CameraActivity", "Processando formulário com classes: $listaDeClasses")
        setTop10predicoesDeClasse(listaDeClasses)
    }

    private fun setTop10predicoesDeClasse(top10predicoesDeClasse: List<String>) {
        top10predicoesDeClasseCompleta = top10predicoesDeClasse
    }

    private fun getTop10predicoesDeClasses(): List<String> {
        return top10predicoesDeClasseCompleta.take(10)
    }

    private fun botaoTirarFotoQueDisparaOEnvioDoFormProFirebase() {
        if (!::lati.isInitialized || !::longi.isInitialized) {
            Toast.makeText(this, "Localização não disponível", Toast.LENGTH_LONG).show()
            return
        }
        Log.d("CameraActivity", "Botão pressionado para processar detecções")
        val listaDeClassesPreditas: List<String> = getTop10predicoesDeClasses()
        Toast.makeText(this, "Enviando local!", Toast.LENGTH_LONG).show()
        // Casting para o padrão do Firebase de localização
        val geopoint = GeoPoint(lati.toDouble(), longi.toDouble())
        val firestoreRepository = FirestoreRepository()
        firestoreRepository.salvarPontoDeInteresse(geopoint, listaDeClassesPreditas)
        premiarUsuario()
    }

    private fun premiarUsuario() {
        val firestoreRepository = FirestoreRepository()
        val trophyGenerator = TrophyGenerator(firestoreRepository)
        val nomeDoUsuario = firestoreRepository.buscarNomeUsuario { nomeUsuario ->
            if (nomeUsuario != null) {
                println("Nome do usuário: $nomeUsuario")
            } else {
                println("Não foi possível obter o nome do usuário.")
            }
        }
        trophyGenerator.gerenciarTrofeus(nomeDoUsuario.toString(), getTop10predicoesDeClasses()) { sucesso ->
            if (sucesso) {
                Log.d("TrophyGenerator", "Troféus gerenciados com sucesso!")
            } else {
                Log.e("TrophyGenerator", "Falha ao gerenciar os troféus.")
            }
        }
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
