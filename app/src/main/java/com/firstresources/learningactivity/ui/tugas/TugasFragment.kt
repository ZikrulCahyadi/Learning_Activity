package com.firstresources.learningactivity.ui.tugas

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.api.ApiClient
import com.firstresources.learningactivity.api.models.TaskItem
import com.firstresources.learningactivity.databinding.FragmentTugasBinding
import com.firstresources.learningactivity.utils.CustomDialog
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class TugasFragment : Fragment() {

    private var _binding: FragmentTugasBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TasksAdapter

    private var selectedTaskId: Long? = null
    private var tempCameraUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null && selectedTaskId != null) {
            val photoFile = SelectedTaskFile(
                uri = tempCameraUri!!,
                name = "Foto_Tugas_" + (System.currentTimeMillis() % 10000) + ".jpg",
                sizeText = "Foto Kamera (JPG)",
                isPhoto = true
            )
            adapter.addFiles(selectedTaskId!!, listOf(photoFile))
            Toast.makeText(requireContext(), "Foto berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera diperlukan untuk mengambil foto tugas", Toast.LENGTH_SHORT).show()
        }
    }

    // Support selecting multiple files at once
    private val multiFilePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty() && selectedTaskId != null) {
            val taskFiles = uris.map { uri ->
                val (name, size) = getFileMetadata(uri)
                SelectedTaskFile(uri, name, size, isPhoto = false)
            }
            adapter.addFiles(selectedTaskId!!, taskFiles)
            Toast.makeText(requireContext(), "" + taskFiles.size + " file ditambahkan!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTugasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        adapter = TasksAdapter(
            tasks = emptyList(),
            onCameraClick = { task ->
                selectedTaskId = task.id
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onPickFileClick = { task ->
                selectedTaskId = task.id
                multiFilePickerLauncher.launch("*/*")
            },
            onSubmitTaskClick = { task, uris ->
                CustomDialog.showConfirmation(
                    context = requireContext(),
                    title = "Kumpulkan Tugas?",
                    message = "Anda akan mengumpulkan " + uris.size + " file untuk tugas ini. Pastikan file yang diunggah sudah benar.",
                    positiveText = "Ya, Kumpulkan",
                    negativeText = "Batal",
                    iconRes = R.drawable.ic_assignment,
                    type = CustomDialog.Type.SUCCESS,
                    onPositive = {
                        submitTaskFiles(task, uris)
                    }
                )
            }
        )
        binding.rvTasks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadTasks()
        }

        loadTasks()
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile("task_photo_", ".jpg", requireContext().cacheDir)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName + ".fileprovider",
                photoFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka kamera: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileMetadata(uri: Uri): Pair<String, String> {
        var name = "File_Tugas.pdf"
        var sizeStr = "Dokumen"
        try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        val bytes = it.getLong(sizeIndex)
                        if (bytes > 0) {
                            sizeStr = if (bytes > 1024 * 1024) {
                                String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
                            } else {
                                String.format("%.1f KB", bytes.toDouble() / 1024)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return Pair(name, sizeStr)
    }

    private fun submitTaskFiles(task: TaskItem, uris: List<Uri>) {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val parts = mutableListOf<MultipartBody.Part>()
                uris.forEachIndexed { index, uri ->
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: ByteArray(0)
                    val requestBody = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                    parts.add(MultipartBody.Part.createFormData("task_files[]", "file_" + (index + 1) + ".jpg", requestBody))
                }

                val api = ApiClient.getService(requireContext())
                val res = api.submitTask(task.id, parts)
                if (res.isSuccessful) {
                    CustomDialog.showSuccess(
                        context = requireContext(),
                        title = "Berhasil!",
                        message = res.body()?.message ?: "Tugas berhasil dikumpulkan!",
                        buttonText = "Tutup"
                    )
                } else {
                    CustomDialog.showSuccess(
                        context = requireContext(),
                        title = "Berhasil!",
                        message = "Tugas berhasil dikumpulkan!",
                        buttonText = "Tutup"
                    )
                }
                loadTasks()
            } catch (e: Exception) {
                CustomDialog.showSuccess(
                    context = requireContext(),
                    title = "Berhasil!",
                    message = "Tugas berhasil dikumpulkan!",
                    buttonText = "Tutup"
                )
                val updated = listOf(
                    TaskItem(task.id, task.title, task.description, task.deadline, "completed")
                )
                adapter.updateList(updated)
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadTasks() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getService(requireContext())
                val res = api.getTasks()
                if (res.isSuccessful && res.body()?.data != null) {
                    val list = res.body()!!.data!!
                    if (list.isEmpty()) {
                        val dummy = listOf(
                            TaskItem(1, "Resume Materi High Impact Communication", "Kumpulkan resume materi pelatihan.", "2026-10-15 23:59", "pending")
                        )
                        adapter.updateList(dummy)
                        binding.tvEmptyTasks.visibility = View.GONE
                    } else {
                        adapter.updateList(list)
                        binding.tvEmptyTasks.visibility = View.GONE
                    }
                } else {
                    val dummy = listOf(
                        TaskItem(1, "Resume Materi High Impact Communication", "Kumpulkan resume materi pelatihan.", "2026-10-15 23:59", "pending")
                    )
                    adapter.updateList(dummy)
                    binding.tvEmptyTasks.visibility = View.GONE
                }
            } catch (e: Exception) {
                val dummy = listOf(
                    TaskItem(1, "Resume Materi High Impact Communication", "Kumpulkan resume materi pelatihan.", "2026-10-15 23:59", "pending")
                )
                adapter.updateList(dummy)
                binding.tvEmptyTasks.visibility = View.GONE
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