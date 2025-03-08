package com.example.midterm_section2.model

import com.google.firebase.firestore.PropertyName

data class Post(
    var description: String = "",

    @get:PropertyName("image_url") @set:PropertyName("image_url")
    var imageUrl: String = "",

    @get:PropertyName("creation_time") @set:PropertyName("creation_time")
    var creationTimeMs: Long = 0,

    var user: User? = null,

    val profileImageUrl: String = ""
)
