package com.example.midterm_section2

import android.content.Context
import android.util.Log
import com.example.midterm_section2.network.GitHubApi
import com.example.midterm_section2.network.GitHubFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.Properties
import java.io.FileInputStream
private const val TAG = "PhotoRepository"

class PhotoRepository private constructor(
    context: Context?,
    private val coroutineScope: CoroutineScope = GlobalScope
) {

    private val properties: Properties = Properties()

    init {
        try {
            val file = File("local.properties")
            if (file.exists()) {
                properties.load(FileInputStream(file))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val API_KEY: String = properties.getProperty("API_KEY", "default_key")

    private val githubApi: GitHubApi
    private val token = API_KEY
    private val owner = "abisara-iu"
    private val repo = "midterm-section2-part3-photostore"
    private val branch = "main"

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = retrofit.create(GitHubApi::class.java)
    }

    companion object {
        @Volatile
        private var INSTANCE: PhotoRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = PhotoRepository(context)
                    }
                }
            }
        }

        fun get(): PhotoRepository {
            return INSTANCE ?: throw IllegalStateException("PhotoRepository must be initialized by calling initialize() first")
        }
    }

    suspend fun saveImage(base64Image: String, filename: String) {
        withContext(Dispatchers.IO) {
            uploadImageToGitHub(base64Image, filename)
        }
    }


    private suspend fun uploadImageToGitHub(base64Image: String, filename: String) {
        val path = "$filename"
        val file = GitHubFile(
            message = "Add $filename",
            content = base64Image
        )

        try {
            val response = githubApi.uploadFile(token, owner, repo, path, file)
            if (response.isSuccessful) {
                Log.d(TAG, "File uploaded successfully: ${response.body()?.content?.path}")
            } else {
                Log.e(TAG, "Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getImageUrl(fileName: String): String {
        return "https://raw.githubusercontent.com/${owner}/${repo}/${branch}/${fileName}"
    }

}


