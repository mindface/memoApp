package com.example.memoapp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.memoapp.databinding.FragmentConceptBinding
import com.example.memoapp.model.CanvasElement
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ConceptFragment : Fragment() {

    private var _binding: FragmentConceptBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val canvasElements = mutableListOf<CanvasElement>()
    private var selectedColor = Color.BLUE
    
    private enum class Mode { PAN_ZOOM, ADD_RECT, ADD_CIRCLE, ADD_TEXT }
    private var currentMode = Mode.PAN_ZOOM

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConceptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        db = Firebase.firestore
        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupCanvas()
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchCanvasElements(currentUser.uid)
        } else {
            Toast.makeText(requireContext(), "Please login to save/load canvas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        binding.btnAddRect.setOnClickListener {
            setMode(Mode.ADD_RECT)
        }
        binding.btnAddCircle.setOnClickListener {
            setMode(Mode.ADD_CIRCLE)
        }
        
        binding.btnAddText.setOnClickListener {
            setMode(Mode.ADD_TEXT)
        }
        binding.btnSaveCanvas.setOnClickListener {
            saveCanvasElements()
        }
        binding.btnClearCanvas.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear Canvas")
                .setMessage("Are you sure you want to delete all elements?")
                .setPositiveButton("Clear") { _, _ ->
                    canvasElements.clear()
                    binding.canvasView.setElements(canvasElements)
                    setMode(Mode.PAN_ZOOM)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.btnDeleteSelected.setOnClickListener {
            binding.canvasView.deleteSelectedElement()
        }
        binding.btnSendToBack.setOnClickListener {
            binding.canvasView.sendSelectedToBack()
        }
        binding.btnFontIncrease.setOnClickListener {
            val selected = binding.canvasView.getSelectedElement()
            if (selected != null && selected.type == "TEXT") {
                selected.fontSize += 10f
                binding.canvasView.invalidate()
            }
        }
        binding.btnFontDecrease.setOnClickListener {
            val selected = binding.canvasView.getSelectedElement()
            if (selected != null && selected.type == "TEXT") {
                selected.fontSize = (selected.fontSize - 10f).coerceAtLeast(10f)
                binding.canvasView.invalidate()
            }
        }
        binding.btnPickColor.setOnClickListener {
            showColorPickerDialog()
        }
        binding.btnEditText.setOnClickListener {
            val selected = binding.canvasView.getSelectedElement()
            if (selected != null && selected.type == "TEXT") {
                showEditTextViewDialog(selected)
            }
        }
    }

    private fun showEditTextViewDialog(element: CanvasElement) {
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val editTextInput = EditText(requireContext()).apply {
            hint = "テキスト"
            setText(element.text)
        }
        val editSizeInput = EditText(requireContext()).apply {
            hint = "サイズ"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(element.fontSize.toInt().toString())
        }

        layout.addView(editTextInput)
        layout.addView(editSizeInput)

        AlertDialog.Builder(requireContext())
            .setTitle("テキスト編集")
            .setView(layout)
            .setPositiveButton("適用") { _, _ ->
                element.text = editTextInput.text.toString()
                val newSize = editSizeInput.text.toString().toFloatOrNull() ?: element.fontSize
                element.fontSize = newSize
                
                // サイズに合わせて枠も更新（InteractiveCanvasViewと同様の計算）
                val paint = android.graphics.Paint()
                paint.textSize = element.fontSize
                val bounds = android.graphics.Rect()
                paint.getTextBounds(element.text, 0, element.text.length, bounds)
                element.width = bounds.width().toFloat() + 20f
                element.height = element.fontSize + 10f
                
                binding.canvasView.invalidate()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun setMode(mode: Mode) {
        currentMode = mode
        binding.textStatus.text = "Mode: ${mode.name}"
        
        // Reset colors/highlights of buttons if desired
        binding.btnAddRect.alpha = if (mode == Mode.ADD_RECT) 1.0f else 0.5f
        binding.btnAddCircle.alpha = if (mode == Mode.ADD_CIRCLE) 1.0f else 0.5f
        binding.btnAddText.alpha = if (mode == Mode.ADD_TEXT) 1.0f else 0.5f
    }

    private fun setupCanvas() {
        binding.canvasView.onCanvasClickListener = { x, y ->
            when (currentMode) {
                Mode.ADD_RECT -> {
                    addNewElement("RECTANGLE", x, y)
                    setMode(Mode.PAN_ZOOM)
                }
                Mode.ADD_CIRCLE -> {
                    addNewElement("CIRCLE", x, y)
                    setMode(Mode.PAN_ZOOM)
                }
                Mode.ADD_TEXT -> {
                    showTextInputDialog(x, y)
                }
                Mode.PAN_ZOOM -> {
                    // Do nothing
                }
            }
        }
        
        binding.canvasView.onSelectionChangedListener = { selected ->
            val isSelected = selected != null
            binding.btnDeleteSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.btnSendToBack.visibility = if (isSelected) View.VISIBLE else View.GONE
            if (isSelected) {
                setMode(Mode.PAN_ZOOM)
            }
        }
    }

    private fun showColorPickerDialog() {
        val colors = listOf(
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#03A9F4"), // Light Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#8BC34A"), // Light Green
            Color.parseColor("#CDDC39"), // Lime
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#795548"), // Brown
            Color.parseColor("#9E9E9E"), // Grey
            Color.parseColor("#607D8B"), // Blue Grey
            Color.parseColor("#000000"), // Black
            Color.parseColor("#FFFFFF")  // White
        )

        val gridLayout = android.widget.GridLayout(requireContext()).apply {
            columnCount = 4
            rowCount = 5
            alignmentMode = android.widget.GridLayout.ALIGN_BOUNDS
            setPadding(16, 16, 16, 16)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Select Color")
            .setView(gridLayout)
            .setNegativeButton("Cancel", null)
            .create()

        for (color in colors) {
            val colorView = View(requireContext()).apply {
                val size = (50 * resources.displayMetrics.density).toInt()
                val params = android.widget.GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params
                setBackgroundColor(color)
                elevation = 4f
                
                setOnClickListener {
                    selectedColor = color
                    binding.canvasView.setSelectedElementColor(color)
                    dialog.dismiss()
                }
            }
            gridLayout.addView(colorView)
        }

        dialog.show()
    }

    private fun addNewElement(type: String, x: Float, y: Float, text: String = "") {
        val maxZ = canvasElements.maxOfOrNull { it.zIndex } ?: 0
        val element = CanvasElement(
            id = db.collection("canvas_elements").document().id,
            userId = auth.currentUser?.uid ?: "",
            type = type,
            x = x,
            y = y,
            width = 150f,
            height = 150f,
            text = text,
            color = if (type == "TEXT") Color.BLACK else selectedColor,
            zIndex = maxZ + 1
        )
        canvasElements.add(element)
        binding.canvasView.setElements(canvasElements)
    }

    private fun showTextInputDialog(x: Float, y: Float) {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Enter Text")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    addNewElement("TEXT", x, y, text)
                }
                setMode(Mode.PAN_ZOOM)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchCanvasElements(userId: String) {
        db.collection("canvas_elements")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { result ->
                canvasElements.clear()
                for (document in result) {
                    val element = document.toObject(CanvasElement::class.java)
                    element.id = document.id
                    canvasElements.add(element)
                }
                binding.canvasView.setElements(canvasElements)
            }
            .addOnFailureListener { e ->
                Log.w("Canvas", "Error fetching elements", e)
            }
    }

    private fun saveCanvasElements() {
        val userId = auth.currentUser?.uid ?: return
        
        // Get the latest state from the view
        val latestElements = binding.canvasView.getElements()
        
        val batch = db.batch()
        
        // In this version, we save all current elements.
        // To handle deletions, you would typically track deleted IDs.
        for (element in latestElements) {
            val docRef = db.collection("canvas_elements").document(element.id)
            element.userId = userId // Ensure userId is set
            batch.set(docRef, element)
        }
        
        batch.commit().addOnSuccessListener {
            Toast.makeText(requireContext(), "Canvas saved", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
