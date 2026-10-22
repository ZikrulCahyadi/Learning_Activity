package com.firstresources.learningactivity.ui.profil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.api.ApiClient
import com.firstresources.learningactivity.api.models.Trainee
import com.firstresources.learningactivity.databinding.FragmentProfilBinding
import com.firstresources.learningactivity.ui.login.LoginActivity
import com.firstresources.learningactivity.utils.CustomDialog
import com.firstresources.learningactivity.utils.SessionManager
import com.firstresources.learningactivity.utils.UpdateManager
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        val user = sessionManager.getUser()
        if (user != null) {
            displayUserData(user)
        } else {
            // Default initial view
            val defaultMateri = sessionManager.getSubjectName().ifEmpty { "High Impact Communication" }
            binding.tvProfileMateri.text = defaultMateri
        }

        fetchRemoteProfile()

        binding.tvAppVersion.setOnClickListener {
            UpdateManager.checkForUpdates(requireContext(), showNoUpdateToast = true)
        }

        binding.btnLogout.setOnClickListener {
            CustomDialog.showConfirmation(
                context = requireContext(),
                title = "Keluar Akun",
                message = "Apakah Anda yakin ingin keluar dari akun ini?",
                positiveText = "Keluar",
                negativeText = "Batal",
                iconRes = R.drawable.ic_logout,
                type = CustomDialog.Type.DANGER,
                onPositive = {
                    sessionManager.logout()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            )
        }
    }

    private fun displayUserData(user: Trainee) {
        binding.tvProfileName.text = user.name ?: "Test Peserta"
        binding.tvProfileCategory.text = user.program ?: "Managerial & Leadership"
        
        // Materi Saat Ini: match dashboard
        val currentMateri = user.materi ?: sessionManager.getSubjectName().ifEmpty { "High Impact Communication" }
        binding.tvProfileMateri.text = currentMateri

        binding.tvProfileTalentTo.text = user.talentTo ?: "-"
        binding.tvProfileUnit.text = user.unit ?: "-"
        binding.tvProfileRegion.text = user.region ?: user.asalDaerah ?: "Riau"
    }

    private fun fetchRemoteProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getService(requireContext())

                // Also refresh dashboard class info if possible
                try {
                    val dashRes = api.getDashboard()
                    if (dashRes.isSuccessful && dashRes.body()?.data?.classInfo != null) {
                        sessionManager.saveClassInfo(dashRes.body()!!.data!!.classInfo)
                    }
                } catch (e: Exception) {
                    // Ignore
                }

                val res = api.getProfile()
                if (res.isSuccessful && res.body()?.data != null) {
                    val user = res.body()!!.data!!
                    sessionManager.saveUser(user)
                    displayUserData(user)
                } else {
                    val cachedUser = sessionManager.getUser()
                    if (cachedUser != null) {
                        displayUserData(cachedUser)
                    }
                }
            } catch (e: Exception) {
                val cachedUser = sessionManager.getUser()
                if (cachedUser != null) {
                    displayUserData(cachedUser)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
