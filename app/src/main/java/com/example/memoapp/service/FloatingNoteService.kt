package com.example.memoapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt

class FloatingNoteService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra("id") ?: ""
        val title = intent?.getStringExtra("title") ?: "No Title"
        val content = intent?.getStringExtra("content") ?: ""
        
        if (floatingView == null) {
            showFloatingWindow(id, title, content)
        }
        return START_NOT_STICKY
    }

    private fun showFloatingWindow(logItemId: String, title: String, content: String) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val repository = com.example.memoapp.LogItemRepository(applicationContext)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        floatingView = ComposeView(this).apply {
            setContent {
                var isMinimized by remember { mutableStateOf(false) }
                var isEditing by remember { mutableStateOf(false) }
                var currentContent by remember { mutableStateOf(content) }
                
                var offsetX by remember { mutableFloatStateOf(params.x.toFloat()) }
                var offsetY by remember { mutableFloatStateOf(params.y.toFloat()) }

                // Update focusability based on editing state
                LaunchedEffect(isEditing) {
                    if (isEditing) {
                        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    } else {
                        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    }
                    windowManager.updateViewLayout(floatingView, params)
                }

                if (isMinimized) {
                    MinimizedView(
                        onExpand = { isMinimized = false },
                        modifier = Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                params.x = offsetX.roundToInt()
                                params.y = offsetY.roundToInt()
                                windowManager.updateViewLayout(floatingView, params)
                            }
                        }
                    )
                } else {
                    FloatingNoteCard(
                        title = title,
                        content = currentContent,
                        isEditing = isEditing,
                        onContentChange = { 
                            currentContent = it
                            // Optional: Auto-save to repo
                            if (logItemId.isNotEmpty()) {
                                repository.getById(logItemId)?.let { item ->
                                    repository.save(item.copy(content = it, updatedAt = System.currentTimeMillis().toString()))
                                }
                            }
                        },
                        onToggleEdit = { isEditing = !isEditing },
                        onCopy = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("note", currentContent)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(applicationContext, "コピーしました", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onMinimize = { 
                            isMinimized = true 
                            isEditing = false
                        },
                        onClose = { stopSelf() },
                        modifier = Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                params.x = offsetX.roundToInt()
                                params.y = offsetY.roundToInt()
                                windowManager.updateViewLayout(floatingView, params)
                            }
                        }
                    )
                }
            }
        }

        // Essential for ComposeView in Service
        val lifecycleOwner = FloatingLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        floatingView?.setViewTreeLifecycleOwner(lifecycleOwner)
        floatingView?.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        floatingView?.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        })
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }
}

@Composable
fun FloatingNoteCard(
    title: String,
    content: String,
    isEditing: Boolean,
    onContentChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onCopy: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .heightIn(min = 150.dp, max = 450.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_edit), 
                        contentDescription = "Copy", 
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onToggleEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(id = if (isEditing) android.R.drawable.ic_menu_save else android.R.drawable.ic_menu_edit),
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                        tint = if (isEditing) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                IconButton(onClick = onMinimize, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, 
                        contentDescription = "Minimize", 
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Close", 
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (isEditing) {
                TextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
fun MinimizedView(onExpand: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(56.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        onClick = onExpand
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Info, contentDescription = "Expand", tint = Color.White)
        }
    }
}

// Minimal LifecycleOwner for Service usage
class FloatingLifecycleOwner : androidx.lifecycle.LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner {
    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateRegistryController = androidx.savedstate.SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    fun performRestore(savedState: android.os.Bundle?) = savedStateRegistryController.performRestore(savedState)
}
