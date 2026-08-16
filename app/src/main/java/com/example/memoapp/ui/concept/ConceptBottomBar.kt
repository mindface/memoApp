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
    onBringToFront: () -> Unit,
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
                .navigationBarsPadding() // システムバーとの重なりを防止
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
                    iconRes = android.R.drawable.ic_menu_delete,
                    contentDescription = "Delete",
                    onClick = onDelete
                )
                
                // 重なり順操作
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_menu_revert,
                        contentDescription = "Send to Back",
                        onClick = onSendToBack
                    )
                    Text(
                        text = "L${selectedElement?.zIndex ?: 0}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_menu_upload,
                        contentDescription = "Bring to Front",
                        onClick = onBringToFront
                    )
                }

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
