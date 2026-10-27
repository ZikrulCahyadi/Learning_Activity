package com.firstresources.learningactivity.utils

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.databinding.DialogCustomConfirmationBinding

object CustomDialog {

    enum class Type {
        DANGER, SUCCESS, WARNING, INFO
    }

    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Ya, Lanjutkan",
        negativeText: String = "Batal",
        iconRes: Int? = null,
        type: Type = Type.DANGER,
        isCancelable: Boolean = true,
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DialogCustomConfirmationBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(isCancelable)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message
        binding.btnDialogPositive.text = positiveText
        binding.btnDialogNegative.text = negativeText

        when (type) {
            Type.DANGER -> {
                binding.layoutIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                binding.ivDialogIcon.setImageResource(iconRes ?: R.drawable.ic_logout)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                binding.btnDialogPositive.setTextColor(Color.WHITE)
            }
            Type.WARNING -> {
                binding.layoutIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                binding.ivDialogIcon.setImageResource(iconRes ?: R.drawable.ic_alert_circle)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                binding.btnDialogPositive.setTextColor(Color.WHITE)
            }
            Type.SUCCESS -> {
                binding.layoutIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                binding.ivDialogIcon.setImageResource(iconRes ?: R.drawable.ic_check_circle)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#16A34A"))
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#047857"))
                binding.btnDialogPositive.setTextColor(Color.WHITE)
            }
            Type.INFO -> {
                binding.layoutIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
                binding.ivDialogIcon.setImageResource(iconRes ?: R.drawable.ic_assignment)
                binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#0284C7"))
                binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#047857"))
                binding.btnDialogPositive.setTextColor(Color.WHITE)
            }
        }

        binding.btnDialogNegative.setOnClickListener {
            dialog.dismiss()
            onNegative?.invoke()
        }

        binding.btnDialogPositive.setOnClickListener {
            dialog.dismiss()
            onPositive.invoke()
        }

        dialog.show()

        val displayMetrics = context.resources.displayMetrics
        val maxPx = (340 * displayMetrics.density).toInt()
        val dialogWidth = (displayMetrics.widthPixels * 0.88).toInt().coerceAtMost(maxPx)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        return dialog
    }

    fun showSuccess(
        context: Context,
        title: String = "Berhasil!",
        message: String,
        buttonText: String = "Tutup",
        iconRes: Int? = null,
        isCancelable: Boolean = true,
        onDismiss: (() -> Unit)? = null
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DialogCustomConfirmationBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(isCancelable)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message

        binding.layoutIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
        binding.ivDialogIcon.setImageResource(iconRes ?: R.drawable.ic_check_circle)
        binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#16A34A"))

        // Single button mode
        binding.btnDialogNegative.visibility = View.GONE
        binding.btnDialogPositive.text = buttonText
        binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#047857"))
        binding.btnDialogPositive.setTextColor(Color.WHITE)

        val params = binding.btnDialogPositive.layoutParams as LinearLayout.LayoutParams
        params.marginStart = 0
        binding.btnDialogPositive.layoutParams = params

        binding.btnDialogPositive.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()

        val displayMetrics = context.resources.displayMetrics
        val maxPx = (340 * displayMetrics.density).toInt()
        val dialogWidth = (displayMetrics.widthPixels * 0.88).toInt().coerceAtMost(maxPx)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        return dialog
    }

    fun showError(
        context: Context,
        title: String = "Gagal",
        message: String,
        buttonText: String = "Tutup",
        iconRes: Int? = null,
        isCancelable: Boolean = true,
        onDismiss: (() -> Unit)? = null
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DialogCustomConfirmationBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(isCancelable)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message

        binding.layoutIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
        binding.ivDialogIcon.setImageResource(iconRes ?: R.drawable.ic_alert_circle)
        binding.ivDialogIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))

        // Single button mode
        binding.btnDialogNegative.visibility = View.GONE
        binding.btnDialogPositive.text = buttonText
        binding.btnDialogPositive.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        binding.btnDialogPositive.setTextColor(Color.WHITE)

        val params = binding.btnDialogPositive.layoutParams as LinearLayout.LayoutParams
        params.marginStart = 0
        binding.btnDialogPositive.layoutParams = params

        binding.btnDialogPositive.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()

        val displayMetrics = context.resources.displayMetrics
        val maxPx = (340 * displayMetrics.density).toInt()
        val dialogWidth = (displayMetrics.widthPixels * 0.88).toInt().coerceAtMost(maxPx)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        return dialog
    }
    fun showEvaluationSuccess(
        context: Context,
        trainingTitle: String,
        trainerName: String,
        onDismissToHome: () -> Unit
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = com.firstresources.learningactivity.databinding.DialogCustomEvaluationSuccessBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.tvSuccessTitle.text = "Pelatihan Selesai!"
        binding.tvBadgeSubtitle.text = "PELATIHAN SELESAI"
        binding.tvSuccessMessage.text = "Selamat! Anda telah menyelesaikan seluruh rangkaian kegiatan pelatihan. Penilaian evaluasi Anda telah berhasil dikirimkan."

        binding.tvInfoTrainingTitle.text = if (trainingTitle.isNotBlank()) trainingTitle else "Pelatihan First Resources"
        binding.tvInfoTrainerName.text = if (trainerName.isNotBlank()) trainerName else "Instruktur"

        binding.btnBackToHome.setOnClickListener {
            dialog.dismiss()
            onDismissToHome.invoke()
        }

        dialog.show()

        val displayMetrics = context.resources.displayMetrics
        val maxPx = (360 * displayMetrics.density).toInt()
        val dialogWidth = (displayMetrics.widthPixels * 0.90).toInt().coerceAtMost(maxPx)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        return dialog
    }
}