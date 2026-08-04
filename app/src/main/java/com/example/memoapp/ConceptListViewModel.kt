package com.example.memoapp

import androidx.lifecycle.ViewModel
import com.example.memoapp.model.Concept
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConceptListViewModel : ViewModel() {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _concepts = MutableStateFlow<List<Concept>>(emptyList())
    val concepts: StateFlow<List<Concept>> = _concepts.asStateFlow()

    init {
        fetchConcepts()
    }

    fun fetchConcepts() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("concepts")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.mapNotNull { it.toObject(Concept::class.java).apply { id = it.id } }
                _concepts.value = list.sortedByDescending { it.updatedAt }
            }
    }

    fun createConcept(title: String, onComplete: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection("concepts").document()
        val concept = Concept(
            id = docRef.id,
            userId = userId,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        docRef.set(concept).addOnSuccessListener {
            onComplete(docRef.id)
        }
    }
}
