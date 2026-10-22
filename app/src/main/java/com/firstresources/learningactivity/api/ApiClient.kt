package com.firstresources.learningactivity.api

import android.content.Context
import com.firstresources.learningactivity.api.models.PendingEvaluationItem
import com.firstresources.learningactivity.utils.SessionManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://fr-academy.my.id/api/"

    private var apiService: ApiService? = null

    class FlexibleEvaluationDeserializer : JsonDeserializer<List<PendingEvaluationItem>> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): List<PendingEvaluationItem> {
            val list = mutableListOf<PendingEvaluationItem>()
            try {
                if (json.isJsonArray) {
                    json.asJsonArray.forEach { elem ->
                        val item = context.deserialize<PendingEvaluationItem>(elem, PendingEvaluationItem::class.java)
                        if (item != null) list.add(item)
                    }
                } else if (json.isJsonObject) {
                    val obj = json.asJsonObject
                    val dataElem = obj.get("data")
                    if (dataElem != null && dataElem.isJsonArray) {
                        dataElem.asJsonArray.forEach { elem ->
                            val item = context.deserialize<PendingEvaluationItem>(elem, PendingEvaluationItem::class.java)
                            if (item != null) list.add(item)
                        }
                    }
                }
            } catch (e: Exception) {
                // Return whatever collected or empty
            }
            return list
        }
    }

    fun getService(context: Context): ApiService {
        if (apiService == null) {
            val sessionManager = SessionManager(context)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Mock & Resilience Interceptor (Matches MobileTrainee api.js behavior)
            val mockInterceptor = Interceptor { chain ->
                val request = chain.request()
                val token = sessionManager.getAuthToken()
                val isDummy = token == "dummy-token-123"

                if (isDummy) {
                    val url = request.url.toString()
                    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val mockJson = when {
                        url.contains("trainee/dashboard") -> """
                            {
                                "status": "success",
                                "data": {
                                    "todayAttendance": false,
                                    "pendingTasks": 1,
                                    "pendingEvaluations": 1,
                                    "trainee": {
                                        "id": 999,
                                        "nik": "123",
                                        "name": "Test Peserta",
                                        "program": "Managerial & Leadership",
                                        "batch": "Batch 2026",
                                        "universitas": "Universitas Indonesia",
                                        "jurusan": "Manajemen Bisnis",
                                        "asal_daerah": "Jakarta",
                                        "email": "testpeserta@first-resources.com",
                                        "phone": "081234567890"
                                    },
                                    "todayClass": "High Impact Communication",
                                    "classInfo": {
                                        "id": 1,
                                        "subject": "High Impact Communication",
                                        "trainer": "Nama Trainer",
                                        "trainer_name": "Nama Trainer",
                                        "start_time": "08:00",
                                        "end_time": "16:00",
                                        "room": "Ruang Leadership A",
                                        "date": "14 Okt 2026",
                                        "lms_url": "https://e-learning.first-resources.com/",
                                        "mess_url": "https://mess-fr-academy.my.id/"
                                    }
                                }
                            }
                        """.trimIndent()

                        url.contains("trainee/profile") -> """
                            {
                                "status": "success",
                                "data": {
                                    "id": 999,
                                    "nik": "123",
                                    "name": "Test Peserta",
                                    "program": "Managerial & Leadership",
                                    "batch": "Batch 2026",
                                    "universitas": "Universitas Indonesia",
                                    "jurusan": "Manajemen Bisnis",
                                    "asal_daerah": "Jakarta",
                                    "email": "testpeserta@first-resources.com",
                                    "phone": "081234567890"
                                }
                            }
                        """.trimIndent()

                        url.contains("trainee/tasks") && request.method == "GET" -> """
                            {
                                "status": "success",
                                "data": [
                                    {
                                        "id": 201,
                                        "title": "Resume Materi High Impact Communication",
                                        "description": "Buat resume materi High Impact Communication mengenai teknik komunikasi efektif, persuasif, dan assertive leadership minimal 2 halaman dalam format PDF.",
                                        "deadline": "2026-10-15 23:59:00",
                                        "status": "pending",
                                        "submitted_at": null,
                                        "file_path": null
                                    }
                                ]
                            }
                        """.trimIndent()

                        url.contains("trainee/evaluations/pending") -> """
                            {
                                "status": "success",
                                "data": [
                                    {
                                        "id": 101,
                                        "date": "2026-10-14",
                                        "materi": "High Impact Communication",
                                        "trainer": "Nama Trainer",
                                        "status": "pending"
                                    }
                                ]
                            }
                        """.trimIndent()

                        url.contains("trainee/attendances") && request.method == "GET" -> """
                            {
                                "status": "success",
                                "data": [
                                    {
                                        "id": 301,
                                        "session": "Sesi Pelatihan",
                                        "date": "2026-10-11",
                                        "status": "Hadir",
                                        "time_in": "07:55:00",
                                        "notes": "Tepat waktu"
                                    }
                                ]
                            }
                        """.trimIndent()

                        url.contains("trainee/submit-attendance") -> """
                            {
                                "status": "success",
                                "message": "Absensi berhasil dicatat (Akun Dummy)."
                            }
                        """.trimIndent()

                        url.contains("trainee/tasks") && request.method == "POST" -> """
                            {
                                "status": "success",
                                "message": "Tugas berhasil dikumpulkan (Akun Dummy)."
                            }
                        """.trimIndent()

                        url.contains("trainee/tasks") && request.method == "DELETE" -> """
                            {
                                "status": "success",
                                "message": "Pengumpulan tugas berhasil dibatalkan (Akun Dummy)."
                            }
                        """.trimIndent()

                        url.contains("trainee/evaluations") && request.method == "POST" -> """
                            {
                                "status": "success",
                                "message": "Evaluasi berhasil dikirim (Akun Dummy)."
                            }
                        """.trimIndent()

                        else -> null
                    }

                    if (mockJson != null) {
                        return@Interceptor Response.Builder()
                            .code(200)
                            .message("OK")
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .body(mockJson.toResponseBody(jsonMediaType))
                            .addHeader("content-type", "application/json; charset=utf-8")
                            .build()
                    }
                }

                // Execute real request to server
                val response = chain.proceed(request)

                // Resilience check: if server returns HTML redirect (like suspended cPanel), convert to clean JSON
                val contentType = response.header("content-type") ?: ""
                if (contentType.contains("text/html") || response.code in 300..399) {
                    val errorJson = """
                        {
                            "status": "error",
                            "message": "Layanan server pelatihan sedang dalam pemeliharaan (Maintenance)."
                        }
                    """.trimIndent()
                    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    return@Interceptor response.newBuilder()
                        .code(503)
                        .message("Service Unavailable")
                        .body(errorJson.toResponseBody(jsonMediaType))
                        .header("content-type", "application/json; charset=utf-8")
                        .build()
                }

                return@Interceptor response
            }

            val authInterceptor = Interceptor { chain ->
                val originalRequest = chain.request()
                val token = sessionManager.getAuthToken()

                val requestBuilder = originalRequest.newBuilder()
                    .header("Accept", "application/json")

                if (!token.isNullOrEmpty()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val listType = object : TypeToken<List<PendingEvaluationItem>>() {}.type
            val gson = GsonBuilder()
                .registerTypeAdapter(listType, FlexibleEvaluationDeserializer())
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}