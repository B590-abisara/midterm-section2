package com.example.midterm_section2

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.midterm_section2.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private const val TAG = "PostViewModel"

class PostViewModel : ViewModel() {

    private val _posts: MutableStateFlow<List<Post>> = MutableStateFlow(emptyList())
    val posts: StateFlow<List<Post>>
        get() = _posts.asStateFlow()

    init {
        val firestoreDB = FirebaseFirestore.getInstance()
        var postsReference = firestoreDB
            .collection("posts")
            .limit(30)
            .orderBy("creation_time", Query.Direction.DESCENDING)

        viewModelScope.launch {
            postsReference.addSnapshotListener { snapshot, exception ->
                if (exception != null || snapshot == null) {
                    Log.e(TAG, "Exception when querying posts", exception)
                    return@addSnapshotListener
                }

                val postList = snapshot.toObjects(Post::class.java)
                _posts.value = postList as MutableList<Post>
                for (post in postList) {
                    Log.i(TAG, "Post $post")
                }
            }
        }
    }
}
