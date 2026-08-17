package com.example.memoapp.ui.concept

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.memoapp.ConceptMode
import com.example.memoapp.ConceptViewModel
import com.example.memoapp.model.CanvasElement

@Composable
fun ConceptScreen(
    viewModel: ConceptViewModel,
    onShowTextDialog: (Float, Float) -> Unit,
    onShowColorPicker: () -> Unit,
    onEditSelectedText: (CanvasElement) -> Unit
) {
    val elements = viewModel.elements
    val mode by viewModel.currentMode.collectAsStateWithLifecycle()
    val selectedElement by viewModel.selectedElement.collectAsStateWithLifecycle()
    val viewOffset by viewModel.viewOffset.collectAsStateWithLifecycle()
    val viewScale by viewModel.viewScale.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 保存・エクスポート結果のトースト表示
    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { success ->
            val message = if (success) "保存完了" else "保存に失敗しました"
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.exportResult.collect { message ->
            if (message != null) {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ConceptBottomBar(
                selectedElement = selectedElement,
                onDelete = { viewModel.deleteSelectedElement() },
                onSendToBack = { viewModel.sendSelectedToBack() },
                onBringToFront = { viewModel.bringSelectedToFront() },
                onPickColor = onShowColorPicker,
                onEditSelected = { selectedElement?.let { onEditSelectedText(it) } },
                onChangeFontSize = { viewModel.changeFontSize(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            ConceptCanvas(
                elements = elements,
                selectedElement = selectedElement,
                viewOffset = viewOffset,
                viewScale = viewScale,
                onSelectElement = { viewModel.selectElement(it) },
                onCanvasClick = { x, y ->
                    when (mode) {
                        ConceptMode.ADD_RECT -> viewModel.addElement("RECTANGLE", x, y)
                        ConceptMode.ADD_CIRCLE -> viewModel.addElement("CIRCLE", x, y)
                        ConceptMode.ADD_TEXT -> onShowTextDialog(x, y)
                        ConceptMode.PAN_ZOOM -> {}
                    }
                },
                onElementUpdate = { viewModel.updateElement(it) },
                onViewStateUpdate = { offset, scale -> viewModel.updateViewState(offset, scale) }
            )

            ConceptToolbar(
                currentMode = mode,
                onModeChange = { viewModel.setMode(it) },
                onSave = { viewModel.saveCanvasElements() },
                onExportImage = { viewModel.exportCanvasAsImage(context) },
                onClear = { viewModel.clearCanvas() },
                modifier = Modifier.align(Alignment.TopStart)
            )

            Text(
                text = "Mode: ${mode.name}",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp),
                color = Color.White
            )
        }
    }
}
