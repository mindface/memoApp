package com.example.memoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.memoapp.model.MeasurementPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeasurementListViewModel(application: Application) : AndroidViewModel(application) {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _measurementPages = MutableStateFlow<List<MeasurementPage>>(emptyList())
    val measurementPages: StateFlow<List<MeasurementPage>> = _measurementPages.asStateFlow()

    init {
        fetchMeasurementPages()
    }

    fun fetchMeasurementPages() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("measurements")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.mapNotNull { doc ->
                    try {
                        doc.toObject(MeasurementPage::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        null
                    }
                }
                _measurementPages.value = list.sortedByDescending { it.updatedAt }
            }
    }

    fun createMeasurementPage(title: String, onComplete: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection("measurements").document()
        val page = MeasurementPage(
            id = docRef.id,
            userId = userId,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        docRef.set(page).addOnSuccessListener {
            onComplete(docRef.id)
        }
    }
}
