package com.example.riomarappnav.logicadeinterface

import android.os.Bundle
import com.example.riomarappnav.databinding.ActivityAboutBinding
import com.example.riomarappnav.utils.BaseActivity

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}