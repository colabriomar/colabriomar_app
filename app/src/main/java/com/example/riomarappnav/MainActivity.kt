package com.example.riomarappnav

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.startActivity
import com.example.riomarappnav.login.RegisterActivity
import com.example.riomarappnav.logicadeinterface.HomeActivity
import com.example.riomarappnav.utils.BaseActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    companion object{
        lateinit var auth: FirebaseAuth
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //instancia global do firebase
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
        if (auth.currentUser != null) {
            // Aguarda 3 segundos e abre a tela inicial
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }, 3000)
        }
    }
}
