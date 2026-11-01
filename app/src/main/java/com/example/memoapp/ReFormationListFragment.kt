package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.memoapp.model.ReFormation
import java.text.SimpleDateFormat
import java.util.*

class ReFormationListFragment : Fragment() {

    private val viewModel: ReFormationListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ReFormationListScreen(
                        viewModel = viewModel,
                        onReFormationClick = { reformationId ->
                            val bundle = Bundle().apply {
                                putString("reformationId", reformationId)
                            }
                            findNavController().navigate(R.id.action_ReFormationListFragment_to_ReFormationDetailFragment, bundle)
                        },
                        onDeleteClick = { reformationId ->
                            showDeleteConfirmationDialog(reformationId)
                        },
                        onAddClick = { showAddReFormationDialog() }
                    )
                }
            }
        }
    }

    private fun showAddReFormationDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Project Title"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("New reFormation Project")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val title = editText.text.toString()
                if (title.isNotEmpty()) {
                    viewModel.createReFormation(title) { reformationId ->
                        val bundle = Bundle().apply {
                            putString("reformationId", reformationId)
                        }
                        findNavController().navigate(R.id.action_ReFormationListFragment_to_ReFormationDetailFragment, bundle)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .apply { makeFullScreen() }
            .show()
    }

    private fun showDeleteConfirmationDialog(reformationId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("reFormationの削除")
            .setMessage("このプロジェクトを削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                viewModel.deleteReFormation(reformationId)
            }
            .setNegativeButton("キャンセル", null)
            .create()
            .apply { makeFullScreen() }
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReFormationListScreen(
    viewModel: ReFormationListViewModel,
    onReFormationClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val reformations by viewModel.reformations.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("reFormation Projects") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(reformations) { ref ->
                ReFormationItem(
                    ref,
                    onClick = { onReFormationClick(ref.id) },
                    onDelete = { onDeleteClick(ref.id) }
                )
            }
        }
    }
}

@Composable
fun ReFormationItem(ref: ReFormation, onClick: () -> Unit, onDelete: () -> Unit) {
    val date = remember(ref.updatedAt) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(ref.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = ref.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_delete),
                        contentDescription = "Delete",
                        tint = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Last updated: $date", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
