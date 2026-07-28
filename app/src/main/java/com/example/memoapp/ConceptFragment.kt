package com.example.memoapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.memoapp.model.CanvasElement
import com.example.memoapp.ui.concept.ConceptScreen

class ConceptFragment : Fragment() {

    private val viewModel: ConceptViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ConceptScreen(
                        viewModel = viewModel,
                        onShowTextDialog = { x, y -> showTextInputDialog(x, y) },
                        onShowColorPicker = { showColorPickerDialog() },
                        onEditSelectedText = { element -> showEditTextViewDialog(element) }
                    )
                }
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
                val newText = editTextInput.text.toString()
                val newSize = editSizeInput.text.toString().toFloatOrNull() ?: element.fontSize
                
                val paint = android.graphics.Paint()
                paint.textSize = newSize
                val bounds = android.graphics.Rect()
                paint.getTextBounds(newText, 0, newText.length, bounds)
                
                val updatedElement = element.copy(
                    text = newText,
                    fontSize = newSize,
                    width = bounds.width().toFloat() + 20f,
                    height = newSize + 10f
                )
                
                viewModel.updateElement(updatedElement)
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showColorPickerDialog() {
        val colors = listOf(
            Color.parseColor("#F44336"), Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"),
            Color.parseColor("#2196F3"), Color.parseColor("#03A9F4"), Color.parseColor("#00BCD4"),
            Color.parseColor("#009688"), Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"),
            Color.parseColor("#CDDC39"), Color.parseColor("#FFEB3B"), Color.parseColor("#FFC107"),
            Color.parseColor("#FF9800"), Color.parseColor("#FF5722"), Color.parseColor("#795548"),
            Color.parseColor("#9E9E9E"), Color.parseColor("#607D8B"), Color.parseColor("#000000"),
            Color.parseColor("#FFFFFF")
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
                    viewModel.setSelectedColor(color)
                    dialog.dismiss()
                }
            }
            gridLayout.addView(colorView)
        }

        dialog.show()
    }

    private fun showTextInputDialog(x: Float, y: Float) {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Enter Text")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    viewModel.addElement("TEXT", x, y, text)
                }
                viewModel.setMode(ConceptMode.PAN_ZOOM)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
