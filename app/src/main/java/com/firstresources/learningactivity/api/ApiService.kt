package com.firstresources.learningactivity.api

import com.firstresources.learningactivity.api.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("trainee/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("trainee/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @GET("trainee/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @GET("trainee/attendances")
    suspend fun getAttendances(): Response<AttendanceListResponse>

    @POST("trainee/submit-attendance")
    suspend fun submitAttendance(
        @Body request: SubmitAttendanceRequest
    ): Response<BaseResponse>

    @GET("trainee/tasks")
    suspend fun getTasks(): Response<TaskListResponse>

    @Multipart
    @POST("trainee/tasks/{id}/submit")
    suspend fun submitTask(
        @Path("id") taskId: Long,
        @Part files: List<MultipartBody.Part>
    ): Response<BaseResponse>

    @DELETE("trainee/tasks/{id}/submit")
    suspend fun deleteTaskSubmission(
        @Path("id") taskId: Long
    ): Response<BaseResponse>

    @GET("trainee/evaluations/pending")
    suspend fun getPendingEvaluations(): Response<com.google.gson.JsonElement>

    @POST("trainee/evaluations/{dailyClassId}")
    suspend fun submitEvaluation(
        @Path("dailyClassId") dailyClassId: Long,
        @Body request: SubmitEvaluationRequest
    ): Response<BaseResponse>
}
