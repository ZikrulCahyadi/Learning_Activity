package com.firstresources.learningactivity.utils

import android.content.Context
import android.content.SharedPreferences
import com.firstresources.learningactivity.api.models.ClassInfo
import com.firstresources.learningactivity.api.models.Trainee
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "MobileTraineeSession"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
        private const val KEY_CLASS_INFO = "class_info"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUser(trainee: Trainee) {
        val json = gson.toJson(trainee)
        prefs.edit().putString(KEY_USER, json).apply()
    }

    fun getUser(): Trainee? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, Trainee::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveClassInfo(classInfo: ClassInfo?) {
        if (classInfo != null) {
            val json = gson.toJson(classInfo)
            prefs.edit().putString(KEY_CLASS_INFO, json).apply()
        }
    }

    fun getClassInfo(): ClassInfo? {
        val json = prefs.getString(KEY_CLASS_INFO, null) ?: return null
        return try {
            gson.fromJson(json, ClassInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getTrainerName(): String {
        val info = getClassInfo()
        return info?.trainerName ?: info?.trainer ?: "Nama Trainer"
    }

    fun getSubjectName(): String {
        val info = getClassInfo()
        return info?.subject ?: "High Impact Communication"
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrEmpty()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
