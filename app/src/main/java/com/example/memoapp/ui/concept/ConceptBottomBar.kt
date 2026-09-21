package com.example.memoapp.ui.concept

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
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
    canPaste: Boolean,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onShowCloudSettings: () -> Unit,
    onShowStyleSettings: () -> Unit,
    onOpenLinkedItem: (CanvasElement) -> Unit,
    onSendToBack: () -> Unit,
    onBringToFront: () -> Unit,
    onEditSelected: () -> Unit,
    onChangeFontSize: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = selectedElement != null || canPaste,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            shadowElevation = 8.dp,
            color = Color.White,
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group: Edit
                if (selectedElement != null) {
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_menu_delete,
                        contentDescription = "Delete",
                        onClick = onDelete
                    )
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_menu_share,
                        contentDescription = "Copy",
                        onClick = onCopy
                    )
                    
                    // Firebase Cloud Settings (Cloud Icon)
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_menu_upload,
                        contentDescription = "Cloud Settings",
                        isSelected = selectedElement.isShared,
                        onClick = onShowCloudSettings
                    )

                    // Appearance Settings (Info Icon)
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_menu_info_details,
                        contentDescription = "Appearance Settings",
                        onClick = onShowStyleSettings
                    )

                    // Open Linked Item (Link Icon)
                    if (selectedElement.linkedItemId != null) {
                        ToolbarButton(
                            iconRes = android.R.drawable.ic_menu_directions,
                            contentDescription = "Open Item",
                            onClick = { onOpenLinkedItem(selectedElement) }
                        )
                    }
                }
                
                if (canPaste) {
                    ToolbarButton(
                        iconRes = android.R.drawable.ic_input_add,
                        contentDescription = "Paste",
                        onClick = onPaste
                    )
                }

                if (selectedElement != null) {
                    VerticalDivider(modifier = Modifier.height(32.dp))

                    // Group: Layers
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ToolbarButton(
                            iconRes = android.R.drawable.ic_menu_revert,
                            contentDescription = "Send to Back",
                            onClick = onSendToBack
                        )
                        Text(
                            text = "L${selectedElement.zIndex}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        ToolbarButton(
                            iconRes = android.R.drawable.ic_menu_upload,
                            contentDescription = "Bring to Front",
                            onClick = onBringToFront
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(32.dp))

                    if (selectedElement.type == "TEXT") {
                        ToolbarButton(
                            iconRes = android.R.drawable.ic_menu_edit,
                            contentDescription = "Edit Text",
                            onClick = onEditSelected
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onChangeFontSize(10f) },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("+")
                            }
                            Spacer(Modifier.width(4.dp))
                            Button(
                                onClick = { onChangeFontSize(-10f) },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("-")
                            }
                        }
                    }
                }
            }
        }
    }
}
