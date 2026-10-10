package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.memoapp.ui.measurement.MeasurementCanvas
import com.example.memoapp.ui.measurement.MeasurementDataView
import com.example.memoapp.ui.measurement.MeasurementItemPickerModal

class MeasurementDetailFragment : Fragment() {

    private val viewModel: MeasurementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val elements = viewModel.elements
                val viewOffset by viewModel.viewOffset.collectAsStateWithLifecycle()
                val viewScale by viewModel.viewScale.collectAsStateWithLifecycle()
                val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()
                val availableSymbols by viewModel.availableSymbols.collectAsStateWithLifecycle()
                val availableNotes by viewModel.availableNotes.collectAsStateWithLifecycle()
                val availableLogItems by viewModel.availableLogItems.collectAsStateWithLifecycle()
                val availableConcepts by viewModel.availableConcepts.collectAsStateWithLifecycle()
                val selectedElementId by viewModel.selectedElementId.collectAsStateWithLifecycle()

                MaterialTheme {
                    Scaffold(
                        topBar = {
                            TabRow(selectedTabIndex = selectedTabIndex) {
                                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Canvas") })
                                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Data Summary") })
                            }
                        },
                        floatingActionButton = {
                            if (selectedTabIndex == 0) {
                                FloatingActionButton(onClick = {
                                    viewModel.setInsertionPoint(100f, 100f)
                                    viewModel.setActiveModal(MeasurementModalType.ITEM_PICKER)
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                                }
                            }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            if (selectedTabIndex == 0) {
                                MeasurementCanvas(
                                    elements = elements,
                                    selectedElementId = selectedElementId,
                                    viewOffset = viewOffset,
                                    viewScale = viewScale,
                                    onIncrement = { id -> viewModel.incrementCount(id) },
                                    onDelete = { id -> viewModel.deleteElement(id) },
                                    onSelectElement = { id -> viewModel.selectElementId(id) },
                                    onMoveSelected = { dx, dy -> viewModel.moveSelectedElement(dx, dy) },
                                    onElementUpdate = { el -> viewModel.updateElement(el) },
                                    onCanvasClick = { x, y ->
                                        viewModel.setInsertionPoint(x, y)
                                        viewModel.setActiveModal(MeasurementModalType.ITEM_PICKER)
                                    },
                                    onViewStateUpdate = { offset, scale -> viewModel.updateViewState(offset, scale) }
                                )
                            } else {
                                MeasurementDataView(
                                    elements = elements,
                                    onIncrement = { id -> viewModel.incrementCount(id) },
                                    onDelete = { id -> viewModel.deleteElement(id) }
                                )
                            }

                            if (activeModal == MeasurementModalType.ITEM_PICKER) {
                                MeasurementItemPickerModal(
                                    availableSymbols = availableSymbols,
                                    availableNotes = availableNotes,
                                    availableLogItems = availableLogItems,
                                    availableConcepts = availableConcepts,
                                    onItemSelected = { type, id, title ->
                                        viewModel.addCounterElement(type, id, title)
                                    },
                                    onDismiss = { viewModel.setActiveModal(MeasurementModalType.NONE) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
