package com.example.memoapp.ui.concept

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.memoapp.ConceptMode

@Composable
fun ConceptToolbar(
    currentMode: ConceptMode,
    isGridEnabled: Boolean,
    onModeChange: (ConceptMode) -> Unit,
    onToggleGrid: () -> Unit,
    onSave: () -> Unit,
    onExportImage: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp),
        shadowElevation = 4.dp,
        color = Color.White,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarButton(
                iconRes = android.R.drawable.ic_menu_crop,
                contentDescription = "Add Rectangle",
                isSelected = currentMode == ConceptMode.ADD_RECT,
                onClick = { onModeChange(ConceptMode.ADD_RECT) }
            )
            ToolbarButton(
                iconRes = android.R.drawable.ic_menu_add,
                contentDescription = "Add Circle",
                isSelected = currentMode == ConceptMode.ADD_CIRCLE,
                onClick = { onModeChange(ConceptMode.ADD_CIRCLE) }
            )
            ToolbarButton(
                iconRes = android.R.drawable.ic_menu_send,
                contentDescription = "Add Arrow",
                isSelected = currentMode == ConceptMode.ADD_ARROW,
                onClick = { onModeChange(ConceptMode.ADD_ARROW) }
            )
            ToolbarButton(
                iconRes = android.R.drawable.ic_menu_edit,
                contentDescription = "Add Text",
                isSelected = currentMode == ConceptMode.ADD_TEXT,
                onClick = { onModeChange(ConceptMode.ADD_TEXT) }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.LightGray))
            Spacer(modifier = Modifier.width(8.dp))

            ToolbarButton(
                iconRes = if (isGridEnabled) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_close_clear_cancel,
                contentDescription = "Toggle Grid",
                isSelected = isGridEnabled,
                onClick = onToggleGrid
            )

            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.LightGray))
            Spacer(modifier = Modifier.width(8.dp))

            ToolbarButton(
                iconRes = android.R.drawable.ic_menu_save,
                contentDescription = "Save Canvas",
                onClick = onSave
            )
            ToolbarButton(
                iconRes = android.R.drawable.ic_menu_gallery,
                contentDescription = "Export Image",
                onClick = onExportImage
            )
            ToolbarButton(
                iconRes = android.R.drawable.ic_menu_delete,
                contentDescription = "Clear Canvas",
                onClick = onClear
            )
        }
    }
}
