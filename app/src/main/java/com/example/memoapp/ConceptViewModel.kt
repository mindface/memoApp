package com.example.memoapp

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.memoapp.model.CanvasElement
import com.example.memoapp.model.Concept
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ConceptMode { PAN_ZOOM, ADD_RECT, ADD_CIRCLE, ADD_TEXT, ADD_ARROW }

class ConceptViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val conceptId: String = savedStateHandle["conceptId"] ?: ""

    val elements = mutableStateListOf<CanvasElement>()
    private var elementsListener: ListenerRegistration? = null

    private val _currentMode = MutableStateFlow(ConceptMode.PAN_ZOOM)
    val currentMode: StateFlow<ConceptMode> = _currentMode.asStateFlow()

    private val _selectedColor = MutableStateFlow(android.graphics.Color.BLUE)
    val selectedColor: StateFlow<Int> = _selectedColor.asStateFlow()

    private val _selectedElement = MutableStateFlow<CanvasElement?>(null)
    val selectedElement: StateFlow<CanvasElement?> = _selectedElement.asStateFlow()

    private val _canPaste = MutableStateFlow(false)
    val canPaste: StateFlow<Boolean> = _canPaste.asStateFlow()

    private var clipboard: CanvasElement? = null

    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult

    private val _exportResult = MutableSharedFlow<String?>()
    val exportResult: SharedFlow<String?> = _exportResult

    private val _viewOffset = MutableStateFlow(Offset.Zero)
    val viewOffset: StateFlow<Offset> = _viewOffset.asStateFlow()

    private val _viewScale = MutableStateFlow(1f)
    val viewScale: StateFlow<Float> = _viewScale.asStateFlow()

    private val _isGridEnabled = MutableStateFlow(true)
    val isGridEnabled: StateFlow<Boolean> = _isGridEnabled.asStateFlow()

    private val _isLocalOnly = MutableStateFlow(false)
    val isLocalOnly: StateFlow<Boolean> = _isLocalOnly.asStateFlow()

    private var conceptMetadata: Concept? = null

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchCanvasElements()
        }
    }

    fun toggleGrid() {
        _isGridEnabled.value = !_isGridEnabled.value
    }

    fun snapToGrid(value: Float): Float {
        if (!_isGridEnabled.value) return value
        val gridSize = 50f
        return (value / gridSize).roundToInt() * gridSize
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
        
        val snappedX = snapToGrid(x)
        val snappedY = snapToGrid(y)
        
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
        val newElement = CanvasElement(
            id = db.collection("canvas_elements").document().id,
            userId = userId,
            conceptId = conceptId,
            type = type,
            x = snappedX,
            y = snappedY,
            width = if (type == "ARROW") 100f else 150f,
            height = if (type == "ARROW") 100f else 150f,
            text = text,
            color = if (type == "TEXT") android.graphics.Color.BLACK else _selectedColor.value,
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

    fun copySelectedElement() {
        _selectedElement.value?.let { element ->
            clipboard = element.copy()
            _canPaste.value = true
            viewModelScope.launch { _exportResult.emit("コピーしました") }
        }
    }

    fun pasteElement() {
        val item = clipboard ?: return
        val userId = auth.currentUser?.uid ?: return
        
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
        val newId = db.collection("canvas_elements").document().id
        
        val pasted = item.copy(
            id = newId,
            userId = userId,
            x = item.x + 40f,
            y = item.y + 40f,
            zIndex = maxZ + 1
        )
        
        elements.add(pasted)
        selectElement(pasted)
        viewModelScope.launch { _exportResult.emit("貼り付けました") }
    }

    fun bringSelectedToFront() {
        _selectedElement.value?.let { element ->
            val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
            val updated = element.copy(zIndex = maxZ + 1)
            updateElement(updated)
            updateElementsList()
        }
    }

    fun sendSelectedToBack() {
        _selectedElement.value?.let { element ->
            val minZ = elements.minOfOrNull { it.zIndex } ?: 0
            val updated = element.copy(zIndex = minZ - 1)
            updateElement(updated)
            updateElementsList()
        }
    }

    fun changeFontSize(delta: Float) {
        _selectedElement.value?.let { element ->
            if (element.type == "TEXT") {
                val newSize = (element.fontSize + delta).coerceAtLeast(10f)
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
        
        val localRepo = ConceptLocalRepository(getApplication())
        val localData = localRepo.loadLocal(conceptId)
        if (localData != null) {
            _isLocalOnly.value = true
            conceptMetadata = localData.concept
            _viewOffset.value = Offset(localData.concept.lastViewX, localData.concept.lastViewY)
            _viewScale.value = localData.concept.lastViewScale
            elements.clear()
            elements.addAll(localData.elements)
        }

        db.collection("concepts").document(conceptId).get().addOnSuccessListener { doc ->
            doc.toObject(Concept::class.java)?.let { concept ->
                conceptMetadata = concept
                if (localData == null) {
                    _viewOffset.value = Offset(concept.lastViewX, concept.lastViewY)
                    _viewScale.value = if (concept.lastViewScale > 0.01f) concept.lastViewScale else 1f
                    _isLocalOnly.value = false
                }
            }
        }
        
        elementsListener?.remove()
        elementsListener = db.collection("canvas_elements")
            .whereEqualTo("concept_id", conceptId)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                val list = snapshots.mapNotNull { document ->
                    try {
                        document.toObject(CanvasElement::class.java)?.apply { id = document.id }
                    } catch (err: Exception) { null }
                }.sortedBy { it.zIndex }
                
                if (localData == null) {
                    elements.clear()
                    elements.addAll(list)
                }
            }
    }

    fun saveCanvasElements(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        if (conceptId.isEmpty()) return

        val isOnline = NetworkUtils.isNetworkAvailable(context)
        val currentConcept = conceptMetadata ?: Concept(id = conceptId, userId = userId, title = "Untitled")
        val updatedConcept = currentConcept.copy(
            lastViewX = _viewOffset.value.x,
            lastViewY = _viewOffset.value.y,
            lastViewScale = _viewScale.value,
            updatedAt = System.currentTimeMillis()
        )
        conceptMetadata = updatedConcept

        if (!isOnline) {
            ConceptLocalRepository(context).saveLocal(updatedConcept, elements.toList())
            _isLocalOnly.value = true
            viewModelScope.launch { _exportResult.emit("オフライン保存しました") }
            return
        }

        val batch = db.batch()
        for (element in elements) {
            val finalId = element.id.ifEmpty { db.collection("canvas_elements").document().id }
            element.id = finalId
            val docRef = db.collection("canvas_elements").document(finalId)
            element.userId = userId
            element.conceptId = conceptId
            batch.set(docRef, element)
        }
        
        batch.commit().addOnSuccessListener {
            ConceptLocalRepository(context).deleteLocal(conceptId)
            _isLocalOnly.value = false
            viewModelScope.launch { _saveResult.emit(true) }
        }.addOnFailureListener {
            ConceptLocalRepository(context).saveLocal(updatedConcept, elements.toList())
            _isLocalOnly.value = true
            viewModelScope.launch { _saveResult.emit(false) }
        }

        db.collection("concepts").document(conceptId).set(updatedConcept)
    }

    fun pushToCloud(context: Context) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            viewModelScope.launch { _exportResult.emit("ネットワークに接続してください") }
            return
        }
        saveCanvasElements(context)
    }

    fun clearCanvas() {
        elements.clear()
        _selectedElement.value = null
        _currentMode.value = ConceptMode.PAN_ZOOM
        _viewOffset.value = Offset.Zero
        _viewScale.value = 1f
    }

    fun updateViewState(offset: Offset, scale: Float) {
        _viewOffset.value = offset
        _viewScale.value = scale
    }

    fun exportCanvasAsImage(context: Context) {
        if (elements.isEmpty()) {
            viewModelScope.launch { _exportResult.emit("保存する要素がありません") }
            return
        }

        viewModelScope.launch {
            try {
                var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
                elements.forEach {
                    minX = minOf(minX, it.x); minY = minOf(minY, it.y)
                    maxX = maxOf(maxX, it.x + it.width); maxY = maxOf(maxY, it.y + it.height)
                }
                val padding = 50f
                val width = (maxX - minX + padding * 2).toInt().coerceAtLeast(1)
                val height = (maxY - minY + padding * 2).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                val paint = Paint().apply { isAntiAlias = true }
                elements.forEach { element ->
                    paint.color = element.color
                    val rx = element.x - minX + padding
                    val ry = element.y - minY + padding
                    when (element.type) {
                        "RECTANGLE" -> canvas.drawRect(rx, ry, rx + element.width, ry + element.height, paint)
                        "CIRCLE" -> canvas.drawCircle(rx + element.width / 2, ry + element.height / 2, element.width / 2, paint)
                        "TEXT" -> {
                            paint.textSize = element.fontSize
                            val b = Rect(); paint.getTextBounds(element.text, 0, element.text.length, b)
                            canvas.drawText(element.text, rx, ry - b.top, paint)
                        }
                    }
                }

                val filename = "concept_${System.currentTimeMillis()}.png"
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MemoApp"); put(MediaStore.Images.Media.IS_PENDING, 1) }
                }
                val uri = context.contentResolver.insert(collection, cv)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { cv.clear(); cv.put(MediaStore.Images.Media.IS_PENDING, 0); context.contentResolver.update(uri, cv, null, null) }
                    _exportResult.emit("画像を保存しました: $filename")
                } else { _exportResult.emit("保存に失敗しました") }
            } catch (e: Exception) { _exportResult.emit("エラーが発生しました: ${e.message}") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        elementsListener?.remove()
    }
}
