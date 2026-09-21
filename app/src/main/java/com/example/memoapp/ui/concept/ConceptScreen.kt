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
    onEditSelectedText: (CanvasElement) -> Unit,
    onNavigateToItem: (itemType: String, itemId: String) -> Unit
) {
    val elements = viewModel.elements
    val mode by viewModel.currentMode.collectAsStateWithLifecycle()
    val selectedElement by viewModel.selectedElement.collectAsStateWithLifecycle()
    val canPaste by viewModel.canPaste.collectAsStateWithLifecycle()
    val viewOffset by viewModel.viewOffset.collectAsStateWithLifecycle()
    val viewScale by viewModel.viewScale.collectAsStateWithLifecycle()
    val isGridEnabled by viewModel.isGridEnabled.collectAsStateWithLifecycle()
    val isLocalOnly by viewModel.isLocalOnly.collectAsStateWithLifecycle()
    val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()
    val availableSymbols by viewModel.availableSymbols.collectAsStateWithLifecycle()
    val availableNotes by viewModel.availableNotes.collectAsStateWithLifecycle()
    val availableLogItems by viewModel.availableLogItems.collectAsStateWithLifecycle()
    val availableConcepts by viewModel.availableConcepts.collectAsStateWithLifecycle()
    val quickColors = viewModel.quickColors
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
                canPaste = canPaste,
                onDelete = { viewModel.deleteSelectedElement() },
                onCopy = { viewModel.copySelectedElement() },
                onPaste = { viewModel.pasteElement() },
                onShowCloudSettings = { viewModel.setActiveModal(com.example.memoapp.ConceptModalType.CLOUD) },
                onShowStyleSettings = { viewModel.setActiveModal(com.example.memoapp.ConceptModalType.STYLE) },
                onOpenLinkedItem = { el ->
                    el.linkedItemId?.let { id ->
                        el.linkedItemType?.let { type ->
                            onNavigateToItem(type, id)
                        }
                    }
                },
                onSendToBack = { viewModel.sendSelectedToBack() },
                onBringToFront = { viewModel.bringSelectedToFront() },
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
            // ... (ConceptCanvas and ConceptToolbar)
            ConceptCanvas(
                elements = elements,
                selectedElement = selectedElement,
                viewOffset = viewOffset,
                viewScale = viewScale,
                isGridEnabled = isGridEnabled,
                onSelectElement = { viewModel.selectElement(it) },
                onCanvasClick = { x, y ->
                    when (mode) {
                        ConceptMode.ADD_RECT -> viewModel.addElement("RECTANGLE", x, y)
                        ConceptMode.ADD_CIRCLE -> viewModel.addElement("CIRCLE", x, y)
                        ConceptMode.ADD_TEXT -> onShowTextDialog(x, y)
                        ConceptMode.ADD_ARROW -> viewModel.addElement("ARROW", x, y)
                        ConceptMode.ADD_LINKED -> {
                            viewModel.setInsertionPoint(x, y)
                            viewModel.setActiveModal(com.example.memoapp.ConceptModalType.ITEM_PICKER)
                        }
                        ConceptMode.PAN_ZOOM -> {}
                    }
                },
                onElementUpdate = { viewModel.updateElement(it) },
                onViewStateUpdate = { offset, scale -> viewModel.updateViewState(offset, scale) },
                onSnapToGrid = { viewModel.snapToGrid(it) }
            )

            ConceptToolbar(
                currentMode = mode,
                isGridEnabled = isGridEnabled,
                onModeChange = { viewModel.setMode(it) },
                onToggleGrid = { viewModel.toggleGrid() },
                onSaveLocal = { viewModel.saveLocalOnly(context) },
                onSaveFirebase = { viewModel.saveToFirebase(context) },
                onExportImage = { viewModel.exportCanvasAsImage(context) },
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

            if (selectedElement != null) {
                when (activeModal) {
                    com.example.memoapp.ConceptModalType.CLOUD -> {
                        ConceptCloudModal(
                            selectedElement = selectedElement!!,
                            onToggleShare = { viewModel.toggleElementSharing() },
                            onDismiss = { viewModel.setActiveModal(com.example.memoapp.ConceptModalType.NONE) }
                        )
                    }
                    com.example.memoapp.ConceptModalType.STYLE -> {
                        ConceptStyleModal(
                            selectedElement = selectedElement!!,
                            availableSymbols = availableSymbols,
                            quickColors = quickColors,
                            onInsertSymbol = { viewModel.insertSymbolText(it) },
                            onCopyText = { text ->
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("concept_text", text)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "コピーしました", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onShareText = { text ->
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            onUpdateBodyColor = { viewModel.updateElementBodyColor(it) },
                            onPickBodyColor = {
                                viewModel.setColorTarget("BODY")
                                onShowColorPicker()
                            },
                            onUpdateStrokeColor = { viewModel.updateElementStrokeColor(it) },
                            onPickStrokeColor = {
                                viewModel.setColorTarget("STROKE")
                                onShowColorPicker()
                            },
                            onUpdateDrawStyle = { viewModel.setElementDrawStyle(it) },
                            onDismiss = { viewModel.setActiveModal(com.example.memoapp.ConceptModalType.NONE) }
                        )
                    }
                    com.example.memoapp.ConceptModalType.ITEM_PICKER -> {
                        ConceptItemPickerModal(
                            availableNotes = availableNotes,
                            availableLogItems = availableLogItems,
                            availableConcepts = availableConcepts,
                            onItemSelected = { type, id, title ->
                                viewModel.addLinkedElement(type, id, title)
                            },
                            onDismiss = { viewModel.setActiveModal(com.example.memoapp.ConceptModalType.NONE) }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
