package com.example.midterm_section2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.midterm_section2.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

private const val TAG = "ProfileFragment"

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        loadUserProfile()

        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                binding.ivProfilePicture.setImageURI(uri)
            } else {
                Log.d(TAG, "No media selected")
            }
        }

        binding.ivProfilePicture.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnUpdateProfilePicture.setOnClickListener {
            if (selectedImageUri != null) {
                uploadProfilePicture()
            } else {
                Toast.makeText(requireContext(), "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            requireActivity().finish()
            findNavController().navigate(R.id.navigateToLogin)
        }

        return binding.root
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val profileUrl = doc.getString("profileImageUrl")
                if (!profileUrl.isNullOrEmpty()) {
                    binding.ivProfilePicture.load(profileUrl)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to load profile image: ${it.message}")
            }
    }

    private fun uploadProfilePicture() {
        val userId = auth.currentUser?.uid ?: return
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnUpdateProfilePicture.isEnabled = false

        uploadImageToGitHub(userId, selectedImageUri!!) { imageUrl ->
            if (imageUrl != null) {
                saveProfilePictureUrlToFirestore(userId, imageUrl)
            } else {
                runOnUiThread {
                    Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                }
                binding.btnUpdateProfilePicture.isEnabled = true
            }
        }
    }

    private fun uploadImageToGitHub(userId: String, imageUri: Uri, callback: (String?) -> Unit) {
        val properties: Properties = Properties()

        val file = File("local.properties")
        if (file.exists()) {
              properties.load(FileInputStream(file))
        }

        val API_KEY: String = properties.getProperty("API_KEY", "default_key")

        val repoOwner = "abisara"  // <-- Replace with your GitHub username
        val repoName = "profile_pictures"
        val personalAccessToken = API_KEY

        val imageFileName = "profile_pictures/$userId-${System.currentTimeMillis()}.jpg"

        val byteArray = convertAndResizeImageToJpeg(imageUri)

        if (byteArray.isEmpty()) {
            Log.e(TAG, "Image is empty or failed to convert.")
            callback(null)
            return
        }

        val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        val json = JSONObject().apply {
            put("message", "Upload profile picture for $userId")
            put("content", base64Image)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$imageFileName"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "token $personalAccessToken")
            .put(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to upload to GitHub", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "GitHub upload failed: ${response.code}, Response: $responseBody")
                    callback(null)
                    return
                }

                val jsonResponse = JSONObject(responseBody ?: "{}")
                val downloadUrl = jsonResponse.getJSONObject("content").getString("download_url")

                callback(downloadUrl)
            }
        })
    }

    private fun saveProfilePictureUrlToFirestore(userId: String, imageUrl: String) {
        firestore.collection("users").document(userId)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener {
                runOnUiThread {
                    binding.ivProfilePicture.load(imageUrl)
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to update Firestore", Toast.LENGTH_SHORT).show()
                }
                Log.e(TAG, "Failed to update Firestore", e)
            }
            .addOnCompleteListener {
                binding.btnUpdateProfilePicture.isEnabled = true
            }
    }

    private fun convertAndResizeImageToJpeg(uri: Uri): ByteArray {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) {
            Log.e(TAG, "Failed to decode image stream")
            return ByteArray(0)
        }

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 1024, 1024, true)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

        return outputStream.toByteArray()
    }

    private fun runOnUiThread(action: () -> Unit) {
        activity?.runOnUiThread(action)
    }
}
