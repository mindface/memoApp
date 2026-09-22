package com.example.memoapp.ui.concept

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.memoapp.model.CanvasElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptCloudModal(
    selectedElement: CanvasElement,
    onToggleShare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "クラウド共有設定",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("Firebase クラウド同期") },
                supportingContent = { 
                    Text(
                        if (selectedElement.isShared) "このオブジェクトは現在クラウドと同期されています" 
                        else "オンにすると他のデバイスやユーザーと共有可能になります"
                    ) 
                },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_upload),
                        contentDescription = null,
                        tint = if (selectedElement.isShared) MaterialTheme.colorScheme.primary else Color.Gray
                    ) 
                },
                trailingContent = {
                    Switch(
                        checked = selectedElement.isShared,
                        onCheckedChange = { onToggleShare() }
                    )
                }
            )
        }
    }
}
