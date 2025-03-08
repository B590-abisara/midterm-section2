package com.example.midterm_section2

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class PostsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the logout button
        val logoutButton: Button = view.findViewById(R.id.btnLogout)

        // Set click listener
        logoutButton.setOnClickListener {
            // Log out from Firebase
            FirebaseAuth.getInstance().signOut()

            // Navigate back to LoginFragment
            findNavController().navigate(R.id.navigateToLogin)
        }
    }
}
