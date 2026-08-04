package com.example.memoapp

import android.graphics.Color
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.memoapp.model.CanvasElement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConceptMode { PAN_ZOOM, ADD_RECT, ADD_CIRCLE, ADD_TEXT }

class ConceptViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val conceptId: String = savedStateHandle["conceptId"] ?: ""

    val elements = mutableStateListOf<CanvasElement>()

    private val _currentMode = MutableStateFlow(ConceptMode.PAN_ZOOM)
    val currentMode: StateFlow<ConceptMode> = _currentMode.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color.BLUE)
    val selectedColor: StateFlow<Int> = _selectedColor.asStateFlow()

    private val _selectedElement = MutableStateFlow<CanvasElement?>(null)
    val selectedElement: StateFlow<CanvasElement?> = _selectedElement.asStateFlow()

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchCanvasElements()
        }
    }

    fun setMode(mode: ConceptMode) {
        _currentMode.value = mode
    }

    fun setSelectedColor(color: Int) {
        _selectedColor.value = color
        _selectedElement.value?.let { element ->
            val updated = element.copy(color = color)
            _selectedElement.value = updated
            updateElement(updated)
        }
    }

    fun selectElement(element: CanvasElement?) {
        _selectedElement.value = element
        if (element != null) {
            _currentMode.value = ConceptMode.PAN_ZOOM
        }
    }

    fun addElement(type: String, x: Float, y: Float, text: String = "") {
        val userId = auth.currentUser?.uid ?: return
        if (conceptId.isEmpty()) return
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
        val newElement = CanvasElement(
            id = db.collection("canvas_elements").document().id,
            userId = userId,
            conceptId = conceptId,
            type = type,
            x = x,
            y = y,
            width = 150f,
            height = 150f,
            text = text,
            color = if (type == "TEXT") Color.BLACK else _selectedColor.value,
            zIndex = maxZ + 1
        )
        elements.add(newElement)
        _currentMode.value = ConceptMode.PAN_ZOOM
    }

    fun updateElement(element: CanvasElement) {
        val index = elements.indexOfFirst { it.id == element.id }
        if (index != -1) {
            elements[index] = element
        }
        // 選択中の要素も新しいインスタンスに更新して、UI（BottomBar等）に即時反映させる
        if (_selectedElement.value?.id == element.id) {
            _selectedElement.value = element
        }
    }

    fun deleteSelectedElement() {
        _selectedElement.value?.let { element ->
            elements.removeAll { it.id == element.id }
            _selectedElement.value = null
        }
    }

    fun sendSelectedToBack() {
        _selectedElement.value?.let { element ->
            val minZ = elements.minOfOrNull { it.zIndex } ?: 0
            element.zIndex = minZ - 1
            updateElementsList()
        }
    }

    fun changeFontSize(delta: Float) {
        _selectedElement.value?.let { element ->
            if (element.type == "TEXT") {
                val newSize = (element.fontSize + delta).coerceAtLeast(10f)
                
                // フォントサイズの比率で拡大縮小
                val ratio = newSize / element.fontSize
                val updated = element.copy(
                    fontSize = newSize,
                    width = element.width * ratio,
                    height = newSize + 10f
                )
                
                updateElement(updated)
            }
        }
    }

    private fun updateElementsList() {
        val sorted = elements.sortedBy { it.zIndex }
        elements.clear()
        elements.addAll(sorted)
    }

    private fun fetchCanvasElements() {
        if (conceptId.isEmpty()) return
        db.collection("canvas_elements")
            .whereEqualTo("concept_id", conceptId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { document ->
                    document.toObject(CanvasElement::class.java).apply { id = document.id }
                }.sortedBy { it.zIndex }
                elements.clear()
                elements.addAll(list)
            }
    }

    fun saveCanvasElements() {
        val userId = auth.currentUser?.uid ?: return
        if (conceptId.isEmpty()) return
        val batch = db.batch()
        for (element in elements) {
            val docRef = db.collection("canvas_elements").document(element.id)
            element.userId = userId
            element.conceptId = conceptId
            batch.set(docRef, element)
        }
        batch.commit()

        // Update the concept's updatedAt timestamp
        db.collection("concepts").document(conceptId)
            .update("updated_at", System.currentTimeMillis())
    }

    fun clearCanvas() {
        elements.clear()
        _selectedElement.value = null
        _currentMode.value = ConceptMode.PAN_ZOOM
    }
}
