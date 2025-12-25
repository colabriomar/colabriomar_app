package com.example.riomarappnav

import android.content.Intent
import android.os.Bundle
import com.example.riomarappnav.logicadeinterface.HomeActivity
import com.example.riomarappnav.login.RegisterActivity
import com.example.riomarappnav.utils.BaseActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    companion object {
        lateinit var auth: FirebaseAuth
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // instancia global do firebase
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, RegisterActivity::class.java))
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }
}
