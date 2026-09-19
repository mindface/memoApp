package com.example.memoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoapp.model.Concept
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*

class ConceptListViewModel(application: Application) : AndroidViewModel(application) {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val localRepo = ConceptLocalRepository(application)

    private val _cloudConcepts = MutableStateFlow<List<Concept>>(emptyList())
    
    val concepts: StateFlow<List<Concept>> = _cloudConcepts.combine(
        MutableStateFlow(Unit) // dummy to trigger refresh
    ) { cloudList, _ ->
        val localItems = localRepo.getAllLocalData()
        val mergedMap = cloudList.associateBy { it.id }.toMutableMap()
        
        localItems.forEach { local ->
            val existing = mergedMap[local.concept.id]
            if (existing == null || local.concept.updatedAt > existing.updatedAt) {
                mergedMap[local.concept.id] = local.concept.copy(isLocalOnly = true)
            }
        }
        
        mergedMap.values.toList().sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchConcepts()
    }

    fun fetchConcepts() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("concepts")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.mapNotNull { doc ->
                    try {
                        doc.toObject(Concept::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        null
                    }
                }
                _cloudConcepts.value = list
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
