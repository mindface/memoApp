package com.example.memoapp.ui.concept

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.memoapp.model.CanvasElement

@Composable
fun ConceptBottomBar(
    selectedElement: CanvasElement?,
    onDelete: () -> Unit,
    onSendToBack: () -> Unit,
    onPickColor: () -> Unit,
    onEditSelected: () -> Unit,
    onChangeFontSize: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedElement != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shadowElevation = 8.dp,
            color = Color.White,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(
                    iconRes = android.R.drawable.ic_delete,
                    contentDescription = "Delete",
                    onClick = onDelete
                )
                ToolbarButton(
                    iconRes = android.R.drawable.ic_menu_revert,
                    contentDescription = "Send to Back",
                    onClick = onSendToBack
                )
                ToolbarButton(
                    iconRes = android.R.drawable.ic_menu_manage,
                    contentDescription = "Color",
                    onClick = onPickColor
                )

                if (selectedElement?.type == "TEXT") {
                    VerticalDivider(modifier = Modifier.height(32.dp))
                    
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_menu_edit,
                        contentDescription = "Edit Text",
                        onClick = onEditSelected
                    )
                    
                    // Simple text for font controls as we don't have good system icons for A+ / A-
                    Button(onClick = { onChangeFontSize(10f) }) {
                        Text("+")
                    }
                    Button(onClick = { onChangeFontSize(-10f) }) {
                        Text("-")
                    }
                }
            }
        }
    }
}
