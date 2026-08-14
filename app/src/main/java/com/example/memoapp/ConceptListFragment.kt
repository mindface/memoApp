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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.memoapp.model.Concept
import java.text.SimpleDateFormat
import java.util.*

class ConceptListFragment : Fragment() {

    private val viewModel: ConceptListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ConceptListScreen(
                        viewModel = viewModel,
                        onConceptClick = { conceptId ->
                            val bundle = Bundle().apply {
                                putString("conceptId", conceptId)
                            }
                            findNavController().navigate(R.id.action_ConceptListFragment_to_ConceptFragment, bundle)
                        },
                        onAddClick = { showAddConceptDialog() }
                    )
                }
            }
        }
    }

    private fun showAddConceptDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Concept Title"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("New Concept")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val title = editText.text.toString()
                if (title.isNotEmpty()) {
                    viewModel.createConcept(title) { conceptId ->
                        val bundle = Bundle().apply {
                            putString("conceptId", conceptId)
                        }
                        findNavController().navigate(R.id.action_ConceptListFragment_to_ConceptFragment, bundle)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptListScreen(
    viewModel: ConceptListViewModel,
    onConceptClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val concepts by viewModel.concepts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Concepts") })
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
            items(concepts) { concept ->
                ConceptItem(concept, onClick = { onConceptClick(concept.id) })
            }
        }
    }
}

@Composable
fun ConceptItem(concept: Concept, onClick: () -> Unit) {
    val date = remember(concept.updatedAt) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(concept.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = concept.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Last updated: $date", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
