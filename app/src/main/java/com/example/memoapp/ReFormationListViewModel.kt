package com.example.memoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoapp.model.ReFormation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReFormationListViewModel(application: Application) : AndroidViewModel(application) {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _reformations = MutableStateFlow<List<ReFormation>>(emptyList())
    val reformations: StateFlow<List<ReFormation>> = _reformations.asStateFlow()

    init {
        fetchReFormations()
    }

    fun fetchReFormations() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("reformations")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.mapNotNull { doc ->
                    try {
                        doc.toObject(ReFormation::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        null
                    }
                }
                _reformations.value = list.sortedByDescending { it.updatedAt }
            }
    }

    fun createReFormation(title: String, onComplete: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection("reformations").document()
        val reformation = ReFormation(
            id = docRef.id,
            userId = userId,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        docRef.set(reformation).addOnSuccessListener {
            onComplete(docRef.id)
        }
    }
}
