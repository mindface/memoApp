package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.memoapp.ui.concept.*
import com.example.memoapp.model.CanvasElement

class ReFormationDetailFragment : Fragment() {

    private val viewModel: ConceptViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                
                MaterialTheme {
                    Scaffold(
                        topBar = {
                            TabRow(selectedTabIndex = selectedTabIndex) {
                                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("List") })
                                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Canvas") })
                            }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            if (selectedTabIndex == 0) {
                                ReFormationListMode(viewModel) { type, id ->
                                    val bundle = Bundle()
                                    when (type) {
                                        "LOG_ITEM" -> {
                                            bundle.putString("logItemId", id)
                                            findNavController().navigate(R.id.action_ReFormationDetailFragment_to_LogItemFragment, bundle)
                                        }
                                        "CONCEPT" -> {
                                            bundle.putString("conceptId", id)
                                            findNavController().navigate(R.id.action_ReFormationDetailFragment_to_ConceptFragment, bundle)
                                        }
                                        else -> Toast.makeText(context, "Nav not impl for $type", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                ConceptScreen(
                                    viewModel = viewModel,
                                    onShowTextDialog = { x, y -> /* Not used much in reFormation */ },
                                    onShowColorPicker = { /* Handled in Style Modal */ },
                                    onEditSelectedText = { /* Handled in Style Modal */ },
                                    onNavigateToItem = { type, id ->
                                        val bundle = Bundle()
                                        when (type) {
                                            "LOG_ITEM" -> {
                                                bundle.putString("logItemId", id)
                                                findNavController().navigate(R.id.action_ReFormationDetailFragment_to_LogItemFragment, bundle)
                                            }
                                            "CONCEPT" -> {
                                                bundle.putString("conceptId", id)
                                                findNavController().navigate(R.id.action_ReFormationDetailFragment_to_ConceptFragment, bundle)
                                            }
                                            else -> Toast.makeText(context, "Nav not impl for $type", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReFormationListMode(
    viewModel: ConceptViewModel,
    onNavigate: (String, String) -> Unit
) {
    val elements = viewModel.elements
    val selectedElement by viewModel.selectedElement.collectAsStateWithLifecycle()
    val selectedItemDetail by viewModel.selectedItemDetail.collectAsStateWithLifecycle()
    val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()

    // Filter only linked items for the list mode
    val linkedItems = elements.filter { it.linkedItemId != null }

    if (linkedItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("リンクされたアイテムがありません。\nCanvasモードから追加してください。", color = Color.Gray)
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(linkedItems.size) { index ->
                val el = linkedItems[index]
                ListItem(
                    headlineContent = { Text(el.text) },
                    supportingContent = { Text(el.linkedItemType ?: "") },
                    modifier = Modifier.clickable {
                        viewModel.loadItemDetail(el)
                    }
                )
            }
        }
    }

    if (activeModal == ConceptModalType.ITEM_DETAIL && selectedItemDetail != null) {
        ConceptLinkedItemDetailModal(
            title = selectedItemDetail!!.first,
            content = selectedItemDetail!!.second,
            onOpenFullEditor = {
                selectedElement?.let { el ->
                    el.linkedItemId?.let { id ->
                        el.linkedItemType?.let { type ->
                            onNavigate(type, id)
                        }
                    }
                }
                viewModel.setActiveModal(ConceptModalType.NONE)
            },
            onDismiss = { viewModel.setActiveModal(ConceptModalType.NONE) }
        )
    }
}
