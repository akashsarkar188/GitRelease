package com.akashsarkar188.gitrelease

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.akashsarkar188.gitrelease.databinding.ActivityMainBinding
import com.akashsarkar188.gitrelease.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }
}