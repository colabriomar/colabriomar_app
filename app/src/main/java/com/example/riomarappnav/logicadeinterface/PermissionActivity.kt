package com.example.riomarappnav.logicadeinterface

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.riomarappnav.R
import com.example.riomarappnav.utils.BaseActivity

@SuppressLint("UseSwitchCompatOrMaterialCode")
class PermissionActivity : BaseActivity() {

    private lateinit var switchLocationPermission: Switch
    private lateinit var switchCameraPermission: Switch
    private lateinit var ivLocation: ImageView
    private lateinit var tvLocationDesc: TextView
    private lateinit var ivCamera: ImageView
    private lateinit var tvCameraDesc: TextView

    // Launcher para solicitar a permissão de Câmera
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "Permissão de câmera concedida!", Toast.LENGTH_SHORT).show()
            switchCameraPermission.isChecked = true
        } else {
            Toast.makeText(this, "Permissão de câmera não concedida.", Toast.LENGTH_LONG).show()
            switchCameraPermission.isChecked = false
        }
    }

    // Launcher para solicitar a permissão de Localização (solicitando ambas: fine e coarse)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            Toast.makeText(this, "Permissão de localização concedida!", Toast.LENGTH_SHORT).show()
            switchLocationPermission.isChecked = true
        } else {
            Toast.makeText(this, "Permissão de localização não concedida.", Toast.LENGTH_LONG).show()
            switchLocationPermission.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.permission)

        // Inicializa os componentes da interface
        switchLocationPermission = findViewById(R.id.switchLocationPermission)
        switchCameraPermission = findViewById(R.id.switchCameraPermission)
        ivLocation = findViewById(R.id.ivLocation)
        tvLocationDesc = findViewById(R.id.tvLocationDesc)
        ivCamera = findViewById(R.id.ivCamera)
        tvCameraDesc = findViewById(R.id.tvCameraDesc)

        // Verifica se as permissões já foram concedidas
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasLocationPermission = (ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)

        // Atualiza o estado dos switches com base nas permissões atuais
        switchCameraPermission.isChecked = hasCameraPermission
        switchLocationPermission.isChecked = hasLocationPermission

        // Configura o listener do switch da câmera
        switchCameraPermission.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                Toast.makeText(this, "Permissão de câmera desabilitada.", Toast.LENGTH_SHORT).show()
            }
        }

        // Configura o listener do switch de localização
        switchLocationPermission.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                Toast.makeText(this, "Permissão de localização desabilitada.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
