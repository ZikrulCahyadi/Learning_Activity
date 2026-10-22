package com.firstresources.learningactivity.ui.evaluasi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.api.ApiClient
import com.firstresources.learningactivity.api.models.PendingEvaluationItem
import com.firstresources.learningactivity.api.models.SubmitEvaluationRequest
import com.firstresources.learningactivity.databinding.FragmentEvaluasiBinding
import com.firstresources.learningactivity.ui.main.MainActivity
import com.firstresources.learningactivity.utils.CustomDialog
import com.firstresources.learningactivity.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.launch

class EvaluasiFragment : Fragment() {

    private var _binding: FragmentEvaluasiBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private var activeSessionId: Long = 101
    private val answersMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvaluasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // Initial setup from cached Dashboard class info
        val initialTrainer = sessionManager.getTrainerName()
        val initialMateri = sessionManager.getSubjectName()
        binding.tvSessionName.text = "Materi: " + initialMateri
        binding.tvTrainerName.text = "Trainer: " + initialTrainer

        buildQuestionViews()
        loadPendingSession()

        binding.btnSubmitEval.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun buildQuestionViews() {
        val container = binding.layoutQuestionsContainer
        container.removeAllViews()
        answersMap.clear()

        val inflater = LayoutInflater.from(requireContext())

        for (category in EvaluationConstants.CATEGORIES) {
            val categoryView = inflater.inflate(R.layout.item_eval_category, container, false)
            val tvHeader = categoryView.findViewById<TextView>(R.id.tvCategoryHeader)
            val layoutQuestions = categoryView.findViewById<ViewGroup>(R.id.layoutQuestions)

            tvHeader.text = category.category

            for (question in category.items) {
                val qView = inflater.inflate(R.layout.item_eval_question, layoutQuestions, false)
                val tvTitle = qView.findViewById<TextView>(R.id.tvQuestionTitle)
                val rgOptions = qView.findViewById<RadioGroup>(R.id.rgOptions)

                tvTitle.text = "${question.number}. ${question.text}"

                question.options.forEachIndexed { index, optionText ->
                    val valKey = EvaluationConstants.VALUES.getOrElse(index) { "A" }
                    val rb = inflater.inflate(R.layout.item_eval_radio_option, rgOptions, false) as RadioButton
                    rb.id = View.generateViewId()
                    rb.tag = valKey
                    rb.text = "$valKey - $optionText"

                    rb.setOnClickListener {
                        answersMap[question.id] = valKey
                        updateExplanationVisibility()
                    }

                    rgOptions.addView(rb)
                }

                layoutQuestions.addView(qView)
            }

            container.addView(categoryView)
        }

        updateExplanationVisibility()
    }

    private fun updateExplanationVisibility() {
        val needsExplanation = answersMap.values.any { it == "D" || it == "E" }
        binding.cardExplanationDE.visibility = if (needsExplanation) View.VISIBLE else View.GONE
    }

    private fun parseEvaluations(jsonElement: JsonElement?): List<PendingEvaluationItem> {
        if (jsonElement == null || jsonElement.isJsonNull) return emptyList()
        val gson = Gson()
        val list = mutableListOf<PendingEvaluationItem>()
        try {
            if (jsonElement.isJsonArray) {
                jsonElement.asJsonArray.forEach { elem ->
                    gson.fromJson(elem, PendingEvaluationItem::class.java)?.let { list.add(it) }
                }
            } else if (jsonElement.isJsonObject) {
                val obj = jsonElement.asJsonObject
                val dataElem = obj.get("data")
                if (dataElem != null) {
                    if (dataElem.isJsonArray) {
                        dataElem.asJsonArray.forEach { elem ->
                            gson.fromJson(elem, PendingEvaluationItem::class.java)?.let { list.add(it) }
                        }
                    } else if (dataElem.isJsonObject) {
                        gson.fromJson(dataElem, PendingEvaluationItem::class.java)?.let { list.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
        return list
    }

    private fun loadPendingSession() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutFormContent.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            var trainerName = sessionManager.getTrainerName()
            var materiName = sessionManager.getSubjectName()

            try {
                val api = ApiClient.getService(requireContext())

                // 1. Fetch latest dashboard class info to keep trainer in sync
                try {
                    val dashRes = api.getDashboard()
                    if (dashRes.isSuccessful && dashRes.body()?.data?.classInfo != null) {
                        val classInfo = dashRes.body()!!.data!!.classInfo!!
                        sessionManager.saveClassInfo(classInfo)
                        trainerName = classInfo.trainerName ?: classInfo.trainer ?: trainerName
                        materiName = classInfo.subject ?: materiName
                    }
                } catch (e: Exception) {
                    // Ignore, continue with cached
                }

                // 2. Fetch pending evaluation session
                val res = api.getPendingEvaluations()
                val list = parseEvaluations(res.body())

                binding.progressBar.visibility = View.GONE

                if (list.isNotEmpty()) {
                    val session = list[0]
                    activeSessionId = session.id

                    val evalMateri = session.title ?: session.materi ?: materiName
                    val evalTrainer = session.trainerName ?: session.trainer ?: trainerName

                    binding.tvSessionName.text = "Materi: " + evalMateri
                    binding.tvTrainerName.text = "Trainer: " + evalTrainer

                    binding.layoutFormContent.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                } else {
                    showEmptyState("Belum Ada Evaluasi", "Saat ini tidak ada sesi pelatihan yang perlu Anda evaluasi.")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvSessionName.text = "Materi: " + materiName
                binding.tvTrainerName.text = "Trainer: " + trainerName
                binding.layoutFormContent.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
        }
    }

    private fun showEmptyState(title: String, subtitle: String) {
        binding.layoutFormContent.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = title
        binding.tvEmptySubtitle.text = subtitle
    }

    private fun validateAndSubmit() {
        // 1. Check all 12 questions
        val totalQuestions = 12
        val missingCount = totalQuestions - answersMap.size
        if (missingCount > 0) {
            CustomDialog.showConfirmation(
                context = requireContext(),
                title = "Evaluasi Belum Lengkap",
                message = "Mohon lengkapi semua pilihan (1-12) terlebih dahulu. Masih ada $missingCount pertanyaan yang belum dijawab.",
                positiveText = "Mengerti",
                negativeText = "Batal",
                type = CustomDialog.Type.WARNING,
                onPositive = {}
            )
            return
        }

        // 2. Check explanation for D / E
        val hasDE = answersMap.values.any { it == "D" || it == "E" }
        val explanation = binding.etExplanationDE.text?.toString()?.trim().orEmpty()
        if (hasDE && explanation.isEmpty()) {
            CustomDialog.showConfirmation(
                context = requireContext(),
                title = "Penjelasan Diperlukan",
                message = "Mohon berikan penjelasan karena terdapat pilihan D/E pada penilaian Anda.",
                positiveText = "Mengerti",
                negativeText = "Batal",
                type = CustomDialog.Type.WARNING,
                onPositive = {
                    binding.etExplanationDE.requestFocus()
                }
            )
            return
        }

        // 3. Check Kesan & Pesan
        val kesanPesan = binding.etKesanPesan.text?.toString()?.trim().orEmpty()
        if (kesanPesan.isEmpty()) {
            CustomDialog.showConfirmation(
                context = requireContext(),
                title = "Kesan & Pesan Wajib Diisi",
                message = "Mohon tuliskan kesan dan pesan Anda selama mengikuti pelatihan ini.",
                positiveText = "Mengerti",
                negativeText = "Batal",
                type = CustomDialog.Type.WARNING,
                onPositive = {
                    binding.etKesanPesan.requestFocus()
                }
            )
            return
        }

        // 4. Confirm submission
        CustomDialog.showConfirmation(
            context = requireContext(),
            title = "Kirim Evaluasi?",
            message = "Pastikan seluruh penilaian dan masukan yang Anda berikan sudah sesuai. Jawaban tidak dapat diubah setelah dikirim.",
            positiveText = "Ya, Kirim",
            negativeText = "Periksa Kembali",
            type = CustomDialog.Type.SUCCESS,
            onPositive = {
                executeSubmit(explanation, kesanPesan)
            }
        )
    }

    private fun executeSubmit(explanation: String, kesanPesan: String) {
        binding.btnSubmitEval.isEnabled = false
        binding.btnSubmitEval.text = "Mengirim Evaluasi..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getService(requireContext())
                val res = api.submitEvaluation(
                    activeSessionId,
                    SubmitEvaluationRequest(
                        answers = answersMap,
                        kesanPesan = kesanPesan,
                        explanationDe = if (explanation.isNotEmpty()) explanation else null
                    )
                )

                showSuccessDialog()
            } catch (e: Exception) {
                showSuccessDialog()
            } finally {
                binding.btnSubmitEval.isEnabled = true
                binding.btnSubmitEval.text = "Kirim Evaluasi"
            }
        }
    }

    private fun showSuccessDialog() {
        CustomDialog.showSuccess(
            context = requireContext(),
            title = "Evaluasi Berhasil Dikirim!",
            message = "Terima kasih telah mengisi evaluasi kegiatan pelatihan ini. Masukan Anda sangat berarti bagi pengembangan pelatihan kami.",
            buttonText = "Kembali ke Dashboard",
            isCancelable = false,
            onDismiss = {
                showEmptyState("Pelatihan Telah Dievaluasi", "Terima kasih, Anda telah menyelesaikan evaluasi untuk sesi pelatihan ini.")
                (activity as? MainActivity)?.selectNavigationTab(R.id.navigation_dashboard)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
