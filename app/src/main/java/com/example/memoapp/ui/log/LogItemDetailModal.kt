package com.example.memoapp.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogItemDetailModal(
    isShared: Boolean,
    onToggleShare: (Boolean) -> Unit,
    onSymbolize: () -> Unit,
    onShareExternally: () -> Unit,
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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "ノートの詳細・共有",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Firebase Cloud Sync
            ListItem(
                headlineContent = { Text("Firebase クラウド同期") },
                supportingContent = { Text("このノートをクラウドに保存して同期します") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_upload),
                        contentDescription = null,
                        tint = if (isShared) MaterialTheme.colorScheme.primary else Color.Gray
                    ) 
                },
                trailingContent = {
                    Switch(
                        checked = isShared,
                        onCheckedChange = { onToggleShare(it) }
                    )
                }
            )

            HorizontalDivider()

            // Symbolize Action
            ListItem(
                headlineContent = { Text("シンボルとして保存") },
                supportingContent = { Text("内容をシンボルリストに登録し、Concepts等で再利用可能にします") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_save), 
                        contentDescription = null 
                    ) 
                },
                modifier = Modifier.clickable { onSymbolize() }
            )

            // External Share
            ListItem(
                headlineContent = { Text("外部アプリで共有") },
                supportingContent = { Text("他のアプリにテキストを送信します") },
                leadingContent = { 
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_share), 
                        contentDescription = null 
                    ) 
                },
                modifier = Modifier.clickable { onShareExternally() }
            )
        }
    }
}
