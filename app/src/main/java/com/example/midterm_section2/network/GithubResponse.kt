package com.example.midterm_section2.network

data class GitHubFileResponse(
    val content: String,  // Base64-encoded file content
    val encoding: String,
    val download_url: String
)