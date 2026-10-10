package com.example.memoapp

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.memoapp.model.Concept
import com.example.memoapp.model.CounterElement
import com.example.memoapp.model.LogItem
import com.example.memoapp.model.MeasurementPage
import com.example.memoapp.model.Note
import com.example.memoapp.model.Symbol
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MeasurementModalType { NONE, ITEM_PICKER }

class MeasurementViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val measurementPageId: String = savedStateHandle["measurementPageId"] ?: ""

    val elements = mutableStateListOf<CounterElement>()
    private var elementsListener: ListenerRegistration? = null

    private val _viewOffset = MutableStateFlow(Offset.Zero)
    val viewOffset: StateFlow<Offset> = _viewOffset.asStateFlow()

    private val _viewScale = MutableStateFlow(1f)
    val viewScale: StateFlow<Float> = _viewScale.asStateFlow()

    private val _insertionPoint = MutableStateFlow(Offset.Zero)
    val insertionPoint: StateFlow<Offset> = _insertionPoint.asStateFlow()

    private val _activeModal = MutableStateFlow(MeasurementModalType.NONE)
    val activeModal: StateFlow<MeasurementModalType> = _activeModal.asStateFlow()

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    fun selectElementId(id: String?) {
        _selectedElementId.value = id
    }

    fun moveSelectedElement(dx: Float, dy: Float) {
        val id = _selectedElementId.value ?: return
        val index = elements.indexOfFirst { it.id == id }
        if (index != -1) {
            val el = elements[index]
            val updated = el.copy(x = el.x + dx, y = el.y + dy)
            elements[index] = updated
            db.collection("counter_elements").document(id).set(updated)
        }
    }

    private val _availableSymbols = MutableStateFlow<List<Symbol>>(emptyList())
    val availableSymbols: StateFlow<List<Symbol>> = _availableSymbols.asStateFlow()

    private val _availableNotes = MutableStateFlow<List<Note>>(emptyList())
    val availableNotes: StateFlow<List<Note>> = _availableNotes.asStateFlow()

    private val _availableLogItems = MutableStateFlow<List<LogItem>>(emptyList())
    val availableLogItems: StateFlow<List<LogItem>> = _availableLogItems.asStateFlow()

    private val _availableConcepts = MutableStateFlow<List<Concept>>(emptyList())
    val availableConcepts: StateFlow<List<Concept>> = _availableConcepts.asStateFlow()

    private var pageMetadata: MeasurementPage? = null

    init {
        val currentUser = auth.currentUser
        if (currentUser != null && measurementPageId.isNotEmpty()) {
            fetchPageAndElements()
            fetchAvailableSymbols(currentUser.uid)
            fetchAvailableNotes(currentUser.uid)
            fetchAvailableConcepts(currentUser.uid)
            fetchAvailableLogItems()
        }
    }

    fun setActiveModal(type: MeasurementModalType) {
        _activeModal.value = type
    }

    fun setInsertionPoint(x: Float, y: Float) {
        _insertionPoint.value = Offset(x, y)
    }

    fun addCounterElement(itemType: String, itemId: String, title: String) {
        val userId = auth.currentUser?.uid ?: return
        if (measurementPageId.isEmpty()) return

        val x = _insertionPoint.value.x
        val y = _insertionPoint.value.y
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0

        val initialWidth = maxOf(300f, title.length * 18f).coerceAtMost(500f)
        val newElement = CounterElement(
            id = db.collection("counter_elements").document().id,
            userId = userId,
            measurementPageId = measurementPageId,
            x = x,
            y = y,
            width = initialWidth,
            height = 110f,
            title = title,
            count = 0,
            fontSize = 20f,
            linkedItemId = itemId,
            linkedItemType = itemType,
            zIndex = maxZ + 1
        )
        elements.add(newElement)
        db.collection("counter_elements").document(newElement.id).set(newElement)
        setActiveModal(MeasurementModalType.NONE)
    }

    fun incrementCount(elementId: String) {
        val index = elements.indexOfFirst { it.id == elementId }
        if (index != -1) {
            val el = elements[index]
            val updated = el.copy(count = el.count + 1)
            elements[index] = updated
            db.collection("counter_elements").document(elementId).set(updated)
        }
    }

    fun updateElement(element: CounterElement) {
        val index = elements.indexOfFirst { it.id == element.id }
        if (index != -1) {
            elements[index] = element
            db.collection("counter_elements").document(element.id).set(element)
        }
    }

    fun deleteElement(elementId: String) {
        elements.removeAll { it.id == elementId }
        db.collection("counter_elements").document(elementId).delete()
    }

    fun bringToFront(elementId: String) {
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
        val index = elements.indexOfFirst { it.id == elementId }
        if (index != -1) {
            val el = elements[index]
            if (el.zIndex < maxZ) {
                val updated = el.copy(zIndex = maxZ + 1)
                elements[index] = updated
                db.collection("counter_elements").document(elementId).set(updated)
                val sorted = elements.sortedBy { it.zIndex }
                elements.clear()
                elements.addAll(sorted)
            }
        }
    }

    fun updateViewState(offset: Offset, scale: Float) {
        _viewOffset.value = offset
        _viewScale.value = scale
    }

    private fun fetchPageAndElements() {
        db.collection("measurements").document(measurementPageId).get().addOnSuccessListener { doc ->
            doc.toObject(MeasurementPage::class.java)?.let { page ->
                pageMetadata = page
                _viewOffset.value = Offset(page.lastViewX, page.lastViewY)
                _viewScale.value = if (page.lastViewScale > 0.01f) page.lastViewScale else 1f
            }
        }

        elementsListener?.remove()
        elementsListener = db.collection("counter_elements")
            .whereEqualTo("measurement_page_id", measurementPageId)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                val list = snapshots.mapNotNull { document ->
                    try {
                        document.toObject(CounterElement::class.java)?.apply { id = document.id }
                    } catch (err: Exception) { null }
                }.sortedBy { it.zIndex }
                elements.clear()
                elements.addAll(list)
            }
    }

    private fun fetchAvailableSymbols(userId: String) {
        db.collection("symbols").whereEqualTo("userId", userId).get().addOnSuccessListener { snapshots ->
            _availableSymbols.value = snapshots.toObjects(Symbol::class.java)
        }
    }

    private fun fetchAvailableNotes(userId: String) {
        db.collection("notes").whereEqualTo("user_id", userId).get().addOnSuccessListener { snapshots ->
            _availableNotes.value = snapshots.documents.mapNotNull { it.toObject(Note::class.java)?.apply { id = it.id } }
        }
    }

    private fun fetchAvailableConcepts(userId: String) {
        db.collection("concepts").whereEqualTo("user_id", userId).get().addOnSuccessListener { snapshots ->
            _availableConcepts.value = snapshots.documents.mapNotNull { it.toObject(Concept::class.java)?.apply { id = it.id } }
        }
    }

    private fun fetchAvailableLogItems() {
        viewModelScope.launch {
            val repo = LogItemRepository(getApplication())
            _availableLogItems.value = repo.getAll()
        }
    }
}
