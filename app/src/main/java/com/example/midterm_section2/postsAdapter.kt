package com.example.midterm_section2

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.midterm_section2.databinding.PostItemBinding
import com.example.midterm_section2.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PostsAdapter"

class PostHolder(private val binding: PostItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.tvUsername.text = post.user?.username ?: "Unknown"
        binding.tvDescription.text = post.description
        binding.tvRelativeTime.text = DateUtils.getRelativeTimeSpanString(post.creationTimeMs)

        // Load profile picture beside username
        if (!post.profileImageUrl.isNullOrEmpty()) {
            binding.ivProfilePicture.load(post.profileImageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_logout)  // Optional placeholder
            }
        }

        // Load post image
        if (!post.imageUrl.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                val bitmap = withContext(Dispatchers.IO) {
                    PhotoRepository.get().fetchAndDecodeImage(post.imageUrl.substringAfterLast("/"))
                }
                if (bitmap != null) {
                    binding.ivPost.setImageBitmap(bitmap)
                }
            }
        }
    }
}

class PostsAdapter(
    private val posts: List<Post>
) : RecyclerView.Adapter<PostHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PostItemBinding.inflate(inflater, parent, false)
        return PostHolder(binding)
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }
}
