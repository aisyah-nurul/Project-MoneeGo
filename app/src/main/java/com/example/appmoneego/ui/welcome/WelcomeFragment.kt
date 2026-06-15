package com.example.appmoneego.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmoneego.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_welcome, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Sembunyikan Bottom Navigation di Welcome Screen ───────────────────
        // Welcome Screen adalah fullscreen onboarding, tidak butuh nav bar.
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            ?.visibility = View.GONE

        // ── Tombol Mulai Sekarang ─────────────────────────────────────────────
        view.findViewById<Button>(R.id.btnMulai).setOnClickListener {

            // Tandai bahwa user sudah melewati Welcome Screen.
            // Setelah ini, SplashFragment akan langsung ke Dashboard.
            requireContext()
                .getSharedPreferences("moneego_prefs", 0)
                .edit()
                .putBoolean("is_first_launch", false)
                .apply()

            findNavController()
                .navigate(R.id.action_welcomeFragment_to_dashboardFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ── Tampilkan kembali Bottom Navigation saat keluar dari Welcome ───────
        // Ini dijalankan ketika fragment di-destroy (navigasi ke Dashboard).
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            ?.visibility = View.VISIBLE
    }
}