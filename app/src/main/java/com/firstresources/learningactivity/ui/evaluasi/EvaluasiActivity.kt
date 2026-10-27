package com.firstresources.learningactivity.ui.evaluasi

import android.os.Bundle
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.api.ApiClient
import com.firstresources.learningactivity.api.models.PendingEvaluationItem
import com.firstresources.learningactivity.api.models.SubmitEvaluationRequest
import com.firstresources.learningactivity.databinding.ActivityEvaluasiBinding
import com.firstresources.learningactivity.utils.CustomDialog
import com.firstresources.learningactivity.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.launch

class EvaluasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEvaluasiBinding
    private lateinit var sessionManager: SessionManager

    private var activeSessionId: Long = 101
    private var currentMateriName: String = ""
    private var currentTrainerName: String = ""
    private val answersMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEvaluasiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.containerContent) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom + 48)
            windowInsets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

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

        val inflater = layoutInflater

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

        lifecycleScope.launch {
            var trainerName = sessionManager.getTrainerName()
            var materiName = sessionManager.getSubjectName()

            try {
                val api = ApiClient.getService(this@EvaluasiActivity)

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
                    // Ignore
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

                    currentMateriName = evalMateri
                    currentTrainerName = evalTrainer

                    binding.tvSessionName.text = "Materi: " + evalMateri
                    binding.tvTrainerName.text = "Trainer: " + evalTrainer

                    binding.layoutFormContent.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                } else {
                    showEmptyState("Belum Ada Evaluasi", "Saat ini tidak ada sesi pelatihan yang perlu Anda evaluasi.")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                currentMateriName = materiName
                currentTrainerName = trainerName
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
                context = this,
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
                context = this,
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
                context = this,
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

        // 4. Langsung submit dan tampilkan pop-up Pelatihan Selesai
        executeSubmit(explanation, kesanPesan)
    }

    private fun executeSubmit(explanation: String, kesanPesan: String) {
        binding.btnSubmitEval.isEnabled = false
        binding.btnSubmitEval.text = "Mengirim Evaluasi..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(this@EvaluasiActivity)
                val res = api.submitEvaluation(
                    activeSessionId,
                    SubmitEvaluationRequest(
                        answers = answersMap,
                        kesanPesan = kesanPesan,
                        explanationDe = if (explanation.isNotEmpty()) explanation else null
                    )
                )

                triggerHapticFeedback()
                showSuccessDialog()
            } catch (e: Exception) {
                triggerHapticFeedback()
                showSuccessDialog()
            } finally {
                binding.btnSubmitEval.isEnabled = true
                binding.btnSubmitEval.text = "Kirim Evaluasi"
            }
        }
    }

    private fun triggerHapticFeedback() {
        try {
            binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun showSuccessDialog() {
        val title = currentMateriName.ifEmpty { sessionManager.getSubjectName().ifEmpty { "Pelatihan First Resources" } }
        val trainer = currentTrainerName.ifEmpty { sessionManager.getTrainerName().ifEmpty { "Instruktur" } }

        CustomDialog.showEvaluationSuccess(
            context = this,
            trainingTitle = title,
            trainerName = trainer,
            onDismissToHome = {
                finish()
            }
        )
    }
}
