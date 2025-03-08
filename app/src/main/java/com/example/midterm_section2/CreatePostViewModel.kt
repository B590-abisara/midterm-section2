package com.example.midterm_section2


import androidx.lifecycle.ViewModel

class CreatePostViewModel : ViewModel() {

    private val photoRepository = PhotoRepository.get()

    suspend fun uploadImageToGitHub(base64Image: String, filename: String) {
        photoRepository.saveImage(base64Image, filename)
    }
}
