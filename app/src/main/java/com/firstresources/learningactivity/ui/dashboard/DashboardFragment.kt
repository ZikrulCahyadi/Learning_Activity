package com.firstresources.learningactivity.ui.dashboard

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.api.ApiClient
import com.firstresources.learningactivity.api.models.ClassInfo
import com.firstresources.learningactivity.databinding.FragmentDashboardBinding
import com.firstresources.learningactivity.ui.main.MainActivity
import com.firstresources.learningactivity.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var currentClassInfo: ClassInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#0D3829"))

        val user = sessionManager.getUser()
        if (user != null) {
            binding.tvPesertaName.text = user.name ?: "Test Peserta"
            binding.tvProgramBadge.text = user.program ?: "High Impact Communication"
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }

        binding.btnHeaderProfile.setOnClickListener {
            (activity as? MainActivity)?.selectNavigationTab(R.id.navigation_profil)
        }

        binding.btnScanQrAction.setOnClickListener {
            (activity as? MainActivity)?.selectNavigationTab(R.id.navigation_absen)
        }

        binding.btnViewTasks.setOnClickListener {
            (activity as? MainActivity)?.selectNavigationTab(R.id.navigation_tugas)
        }

        binding.btnStartEvalAction.setOnClickListener {
            (activity as? MainActivity)?.selectNavigationTab(R.id.navigation_evaluasi)
        }

        binding.btnLms.setOnClickListener {
            try {
                val url = currentClassInfo?.lmsUrl ?: "https://e-learning.first-resources.com/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }

        binding.btnMess.setOnClickListener {
            try {
                val url = currentClassInfo?.messUrl ?: "https://mess-fr-academy.my.id/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }

        loadDashboardData()
    }

    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getService(requireContext())

                var tasksCount = 0
                var evalsCount = 0

                val dashRes = api.getDashboard()
                if (dashRes.isSuccessful && dashRes.body()?.data != null) {
                    val data = dashRes.body()!!.data!!
                    currentClassInfo = data.classInfo
                    sessionManager.saveClassInfo(data.classInfo)

                    data.trainee?.let { t ->
                        binding.tvPesertaName.text = t.name ?: "Test Peserta"
                        sessionManager.saveUser(t)
                    }

                    if (data.todayAttendance) {
                        binding.tvAttendanceStatus.text = "Sudah Absen"
                        binding.tvAttendanceStatus.setTextColor(Color.parseColor("#047857"))
                        binding.btnScanQrAction.visibility = View.GONE
                    } else {
                        binding.tvAttendanceStatus.text = "Belum Absen"
                        binding.tvAttendanceStatus.setTextColor(Color.parseColor("#0F4A38"))
                        binding.btnScanQrAction.visibility = View.VISIBLE
                    }

                    tasksCount = data.pendingTasks
                    evalsCount = data.pendingEvaluations

                    val classInfo = data.classInfo
                    if (classInfo != null && classInfo.id != null) {
                        binding.tvScheduleHeaderTitle.text = "Jadwal Training Aktif"
                        binding.tvScheduleSubtitle.text = classInfo.subject ?: "Detail jadwal kelas Anda saat ini."
                        binding.tvProgramBadge.text = classInfo.subject ?: data.trainee?.program ?: "High Impact Communication"

                        binding.tvScheduleDate.text = classInfo.date ?: "14 Okt 2026"
                        binding.tvScheduleTime.text = (classInfo.startTime ?: "08:00") + " - " + (classInfo.endTime ?: "16:00")
                        binding.tvScheduleRoom.text = (classInfo.room ?: "Ruang Leadership A").replace("FR Academy / ", "")
                        binding.tvScheduleTrainer.text = classInfo.trainerName ?: classInfo.trainer ?: "Nama Trainer"
                        binding.layoutScheduleDetails.visibility = View.VISIBLE
                    } else {
                        binding.tvScheduleHeaderTitle.text = "Jadwal Training Aktif"
                        binding.tvScheduleSubtitle.text = "Detail jadwal kelas Anda saat ini."
                        binding.layoutScheduleDetails.visibility = View.VISIBLE
                        if (!data.todayClass.isNullOrEmpty()) {
                            binding.tvProgramBadge.text = data.todayClass
                        }
                    }
                } else {
                    // Fallback when server is offline or unreachable
                    val user = sessionManager.getUser()
                    if (user != null) {
                        binding.tvPesertaName.text = user.name ?: "Test Peserta"
                        binding.tvProgramBadge.text = user.program ?: "High Impact Communication"
                    }
                    tasksCount = 1
                    evalsCount = 1
                }

                // Directly cross-check with Tasks API to display accurate active pending tasks count
                try {
                    val tasksRes = api.getTasks()
                    if (tasksRes.isSuccessful && tasksRes.body()?.data != null) {
                        val activePending = tasksRes.body()!!.data!!.count { task ->
                            !task.status.equals("completed", ignoreCase = true) && !task.status.equals("submitted", ignoreCase = true)
                        }
                        tasksCount = activePending
                    }
                } catch (e: Exception) {
                    if (tasksCount == 0) tasksCount = 1
                }

                // Directly cross-check with Evaluations API to display accurate pending evaluations count
                try {
                    val evalRes = api.getPendingEvaluations()
                    val evalJson = evalRes.body()
                    if (evalRes.isSuccessful && evalJson != null) {
                        val count = when {
                            evalJson.isJsonArray -> evalJson.asJsonArray.size()
                            evalJson.isJsonObject && evalJson.asJsonObject.has("data") && evalJson.asJsonObject.get("data").isJsonArray -> {
                                evalJson.asJsonObject.get("data").asJsonArray.size()
                            }
                            else -> evalsCount
                        }
                        evalsCount = count
                    }
                } catch (e: Exception) {
                    if (evalsCount == 0) evalsCount = 1
                }

                binding.tvPendingTasksCount.text = "" + tasksCount
                binding.tvPendingEvalsCount.text = "" + evalsCount

            } catch (e: Exception) {
                val user = sessionManager.getUser()
                if (user != null) {
                    binding.tvPesertaName.text = user.name ?: "Test Peserta"
                    binding.tvProgramBadge.text = user.program ?: "High Impact Communication"
                }
                binding.tvPendingTasksCount.text = "1"
                binding.tvPendingEvalsCount.text = "1"
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}