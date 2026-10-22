package com.firstresources.learningactivity.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstresources.learningactivity.api.ApiClient
import com.firstresources.learningactivity.api.models.LoginRequest
import com.firstresources.learningactivity.api.models.Trainee
import com.firstresources.learningactivity.databinding.ActivityLoginBinding
import com.firstresources.learningactivity.ui.main.MainActivity
import com.firstresources.learningactivity.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            goToMain()
            return
        }

        binding.btnLogin.setOnClickListener {
            val nik = binding.etNik.text?.toString()?.trim() ?: ""
            if (nik.isEmpty()) {
                binding.etNik.error = "Harap masukkan NIK Anda"
                return@setOnClickListener
            }
            performLogin(nik)
        }
    }

    private fun performLogin(nik: String) {
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                if (nik == "123") {
                    val dummyTrainee = Trainee(
                        id = 999,
                        nik = "123",
                        name = "Test Peserta",
                        program = "Managerial & Leadership",
                        batch = "Batch 2026",
                        universitas = "Universitas Indonesia",
                        jurusan = "Manajemen Bisnis",
                        asalDaerah = "Jakarta",
                        email = "testpeserta@first-resources.com",
                        phone = "081234567890"
                    )
                    sessionManager.saveAuthToken("dummy-token-123")
                    sessionManager.saveUser(dummyTrainee)
                    Toast.makeText(this@LoginActivity, "Login berhasil (Akun Dummy)", Toast.LENGTH_SHORT).show()
                    goToMain()
                    return@launch
                }

                val response = ApiClient.getService(this@LoginActivity).login(LoginRequest(nik))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body.token
                    if (!token.isNullOrEmpty()) {
                        sessionManager.saveAuthToken(token)
                        body.trainee?.let { sessionManager.saveUser(it) }
                        Toast.makeText(this@LoginActivity, body.message ?: "Login berhasil!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    } else {
                        Toast.makeText(this@LoginActivity, "Token login tidak ditemukan", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "NIK tidak terdaftar atau kredensial salah", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Koneksi gagal: " + e.localizedMessage, Toast.LENGTH_LONG).show()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
