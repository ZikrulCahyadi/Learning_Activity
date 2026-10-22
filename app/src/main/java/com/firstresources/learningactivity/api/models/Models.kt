package com.firstresources.learningactivity.api.models

import com.google.gson.annotations.SerializedName

data class Trainee(
    val id: Long? = null,
    val nik: String? = null,
    val name: String? = null,
    val program: String? = null,
    val batch: String? = null,
    val universitas: String? = null,
    val jurusan: String? = null,
    @SerializedName("asal_daerah") val asalDaerah: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val materi: String? = null,
    @SerializedName("talent_to") val talentTo: String? = null,
    val unit: String? = null,
    val region: String? = null
)

data class LoginRequest(
    val nik: String
)

data class LoginResponse(
    val status: String? = null,
    val message: String? = null,
    val token: String? = null,
    val trainee: Trainee? = null
)

data class ClassInfo(
    val id: Long? = null,
    val subject: String? = null,
    val trainer: String? = null,
    @SerializedName("trainer_name") val trainerName: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null,
    val room: String? = null,
    val date: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("is_passed") val isPassed: Boolean? = null,
    @SerializedName("lms_url") val lmsUrl: String? = null,
    @SerializedName("mess_url") val messUrl: String? = null
)

data class DashboardData(
    val todayAttendance: Boolean = false,
    val pendingTasks: Int = 0,
    val pendingEvaluations: Int = 0,
    val trainee: Trainee? = null,
    val todayClass: String? = null,
    val classInfo: ClassInfo? = null
)

data class DashboardResponse(
    val status: String? = null,
    val data: DashboardData? = null
)

data class ProfileResponse(
    val status: String? = null,
    val data: Trainee? = null
)

data class AttendanceItem(
    val id: Long? = null,
    val session: String? = null,
    val date: String? = null,
    val status: String? = null,
    @SerializedName("time_in") val timeIn: String? = null,
    val notes: String? = null
)

data class AttendanceListResponse(
    val status: String? = null,
    val data: List<AttendanceItem>? = null
)

data class SubmitAttendanceRequest(
    @SerializedName("qr_code") val qrCode: String
)

data class BaseResponse(
    val status: String? = null,
    val message: String? = null
)

data class TaskItem(
    val id: Long,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,
    val status: String? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("file_path") val filePath: String? = null
)

data class TaskListResponse(
    val status: String? = null,
    val data: List<TaskItem>? = null
)

data class PendingEvaluationItem(
    val id: Long,
    val date: String? = null,
    val materi: String? = null,
    val title: String? = null,
    val trainer: String? = null,
    @SerializedName("trainer_name") val trainerName: String? = null,
    val status: String? = null
) {
    val displayMateri: String get() = title ?: materi ?: "Pelatihan FR"
    val displayTrainer: String get() = trainerName ?: trainer ?: "Instruktur"
}

data class EvaluationListResponse(
    val status: String? = null,
    val data: List<PendingEvaluationItem>? = null
)

data class SubmitEvaluationRequest(
    val answers: Map<String, String>,
    @SerializedName("kesan_pesan") val kesanPesan: String? = null,
    @SerializedName("explanation_de") val explanationDe: String? = null
)