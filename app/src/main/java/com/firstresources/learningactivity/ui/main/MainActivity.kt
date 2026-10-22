package com.firstresources.learningactivity.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.databinding.ActivityMainBinding
import com.firstresources.learningactivity.ui.absen.AbsenFragment
import com.firstresources.learningactivity.ui.dashboard.DashboardFragment
import com.firstresources.learningactivity.ui.evaluasi.EvaluasiFragment
import com.firstresources.learningactivity.ui.profil.ProfilFragment
import com.firstresources.learningactivity.ui.tugas.TugasFragment
import com.firstresources.learningactivity.utils.UpdateManager

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        UpdateManager.checkForUpdates(this)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.navigation_absen -> {
                    loadFragment(AbsenFragment())
                    true
                }
                R.id.navigation_tugas -> {
                    loadFragment(TugasFragment())
                    true
                }
                R.id.navigation_evaluasi -> {
                    loadFragment(EvaluasiFragment())
                    true
                }
                R.id.navigation_profil -> {
                    loadFragment(ProfilFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun selectNavigationTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
