package com.example.memoapp

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.memoapp.model.CanvasElement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.io.OutputStream
import kotlin.math.roundToInt

enum class ConceptMode { PAN_ZOOM, ADD_RECT, ADD_CIRCLE, ADD_TEXT, ADD_ARROW }

class ConceptViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val conceptId: String = savedStateHandle["conceptId"] ?: ""

    val elements = mutableStateListOf<CanvasElement>()
    private var elementsListener: ListenerRegistration? = null

    private val _currentMode = MutableStateFlow(ConceptMode.PAN_ZOOM)
    val currentMode: StateFlow<ConceptMode> = _currentMode.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color.BLUE)
    val selectedColor: StateFlow<Int> = _selectedColor.asStateFlow()

    private val _selectedElement = MutableStateFlow<CanvasElement?>(null)
    val selectedElement: StateFlow<CanvasElement?> = _selectedElement.asStateFlow()

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

    fun toggleGrid() {
        _isGridEnabled.value = !_isGridEnabled.value
    }

    fun snapToGrid(value: Float): Float {
        if (!_isGridEnabled.value) return value
        val gridSize = 50f
        return (value / gridSize).roundToInt() * gridSize
    }

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
        if (conceptId.isEmpty()) {
            Log.w("Firestore", "fetchCanvasElements: conceptId is empty")
            return
        }
        
        Log.d("Firestore", "Fetching elements for conceptId: $conceptId")

        // Fetch concept metadata (view state)
        db.collection("concepts").document(conceptId).get().addOnSuccessListener { doc ->
            doc.toObject(com.example.memoapp.model.Concept::class.java)?.let { concept ->
                _viewOffset.value = Offset(concept.lastViewX, concept.lastViewY)
                _viewScale.value = if (concept.lastViewScale > 0.01f) concept.lastViewScale else 1f
                Log.d("Firestore", "View state restored: ${_viewOffset.value}, scale: ${_viewScale.value}")
            }
        }
        
        elementsListener?.remove()
        elementsListener = db.collection("canvas_elements")
            .whereEqualTo("concept_id", conceptId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", "Listen failed for canvas_elements", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    Log.d("Firestore", "Snapshot received. Document count: ${snapshots.size()}")
                    val list = snapshots.mapNotNull { document ->
                        try {
                            document.toObject(CanvasElement::class.java)?.apply { id = document.id }
                        } catch (err: Exception) {
                            Log.e("Firestore", "Error deserializing CanvasElement (ID: ${document.id})", err)
                            null
                        }
                    }.sortedBy { it.zIndex }
                    
                    elements.clear()
                    elements.addAll(list)
                    Log.d("Firestore", "Elements list updated. Count: ${elements.size}")
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        elementsListener?.remove()
    }

    fun saveCanvasElements() {
        val userId = auth.currentUser?.uid ?: return
        if (conceptId.isEmpty()) return
        
        Log.d("Firestore", "Saving ${elements.size} elements for conceptId: $conceptId")
        val batch = db.batch()
        for (element in elements) {
            // Ensure ID is not empty
            val finalId = if (element.id.isEmpty()) db.collection("canvas_elements").document().id else element.id
            element.id = finalId
            
            val docRef = db.collection("canvas_elements").document(finalId)
            element.userId = userId
            element.conceptId = conceptId
            
            Log.d("Firestore", "Queuing element: ${element.type} ID: $finalId")
            batch.set(docRef, element)
        }
        
        batch.commit()
            .addOnSuccessListener {
                Log.d("Firestore", "Successfully saved elements")
                viewModelScope.launch { _saveResult.emit(true) }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to save elements", e)
                viewModelScope.launch { _saveResult.emit(false) }
            }

        // Update the concept's metadata including view state
        val updateData = mapOf(
            "updated_at" to System.currentTimeMillis(),
            "last_view_x" to _viewOffset.value.x,
            "last_view_y" to _viewOffset.value.y,
            "last_view_scale" to _viewScale.value
        )

        db.collection("concepts").document(conceptId)
            .update(updateData)
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to update concept metadata", e)
            }
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
                // 1. 描画範囲の計算
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE

                elements.forEach {
                    minX = minOf(minX, it.x)
                    minY = minOf(minY, it.y)
                    maxX = maxOf(maxX, it.x + it.width)
                    maxY = maxOf(maxY, it.y + it.height)
                }

                // 余白の追加
                val padding = 50f
                val width = (maxX - minX + padding * 2).toInt()
                val height = (maxY - minY + padding * 2).toInt()

                // 2. Bitmap の作成と描画
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE) // 背景を白に

                val paint = Paint().apply {
                    isAntiAlias = true
                }

                elements.forEach { element ->
                    paint.color = element.color
                    val rx = element.x - minX + padding
                    val ry = element.y - minY + padding

                    when (element.type) {
                        "RECTANGLE" -> {
                            canvas.drawRect(rx, ry, rx + element.width, ry + element.height, paint)
                        }
                        "CIRCLE" -> {
                            val centerX = rx + element.width / 2
                            val centerY = ry + element.height / 2
                            canvas.drawCircle(centerX, centerY, element.width / 2, paint)
                        }
                        "TEXT" -> {
                            paint.textSize = element.fontSize
                            val bounds = Rect()
                            paint.getTextBounds(element.text, 0, element.text.length, bounds)
                            // テキストはベースラインからの描画になるため調整
                            canvas.drawText(element.text, rx, ry - bounds.top, paint)
                        }
                    }
                }

                // 3. MediaStore への保存
                val filename = "concept_${System.currentTimeMillis()}.png"
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MemoApp")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = context.contentResolver.insert(collection, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)
                    }
                    _exportResult.emit("画像を保存しました: $filename")
                } else {
                    _exportResult.emit("保存に失敗しました")
                }
            } catch (e: Exception) {
                Log.e("Export", "Error exporting image", e)
                _exportResult.emit("エラーが発生しました: ${e.message}")
            }
        }
    }
}
