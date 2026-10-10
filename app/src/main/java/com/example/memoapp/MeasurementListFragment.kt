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
import com.example.memoapp.model.MeasurementPage
import java.text.SimpleDateFormat
import java.util.*

class MeasurementListFragment : Fragment() {

    private val viewModel: MeasurementListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    MeasurementListScreen(
                        viewModel = viewModel,
                        onPageClick = { pageId ->
                            val bundle = Bundle().apply {
                                putString("measurementPageId", pageId)
                            }
                            findNavController().navigate(R.id.action_MeasurementListFragment_to_MeasurementDetailFragment, bundle)
                        },
                        onAddClick = { showAddMeasurementDialog() }
                    )
                }
            }
        }
    }

    private fun showAddMeasurementDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Measurement Title"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("New Measurement Project")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val title = editText.text.toString()
                if (title.isNotEmpty()) {
                    viewModel.createMeasurementPage(title) { pageId ->
                        val bundle = Bundle().apply {
                            putString("measurementPageId", pageId)
                        }
                        findNavController().navigate(R.id.action_MeasurementListFragment_to_MeasurementDetailFragment, bundle)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementListScreen(
    viewModel: MeasurementListViewModel,
    onPageClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val pages by viewModel.measurementPages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Measurement Projects") })
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
            items(pages) { page ->
                MeasurementPageItem(page, onClick = { onPageClick(page.id) })
            }
        }
    }
}

@Composable
fun MeasurementPageItem(page: MeasurementPage, onClick: () -> Unit) {
    val date = remember(page.updatedAt) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(page.updatedAt))
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
                Text(text = page.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_manage),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Last updated: $date", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
