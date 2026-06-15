package com.example.appmoneego.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmoneego.R

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            // Cek apakah ini pertama kali buka app
            val prefs = requireContext()
                .getSharedPreferences("moneego_prefs", 0)
            val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

            if (isFirstLaunch) {
                // User baru → ke Welcome
                findNavController()
                    .navigate(R.id.action_splash_to_welcome)
            } else {
                // User lama → langsung Dashboard
                findNavController()
                    .navigate(R.id.action_splash_to_dashboard)
            }
        }, 2000)
    }
}