package com.example.appmoneego

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.appmoneego.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Hubungkan bottom nav dengan nav controller
        binding.bottomNavigationView.setupWithNavController(navController)

        // Atur visibility bottomNav per halaman
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Splash → sembunyikan bottomNav
                R.id.splashFragment -> {
                    binding.bottomNavigationView.visibility = View.GONE
                }
                // Halaman lain → tampilkan bottomNav
                else -> {
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }
    }
}