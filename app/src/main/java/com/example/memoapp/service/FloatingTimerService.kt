package com.example.memoapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

class FloatingTimerService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "作業タイマー"
        val initialTarget = intent?.getIntExtra("targetMinutes", 0) ?: 0
        if (floatingView == null) {
            showFloatingWindow(title, initialTarget)
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun showFloatingWindow(title: String, initialTarget: Int) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val appContext = applicationContext

        floatingView = ComposeView(this).apply {
            setContent {
                var elapsedSeconds by remember { mutableLongStateOf(0L) }
                var isRunning by remember { mutableStateOf(true) }
                var targetMinutes by remember { mutableIntStateOf(initialTarget) }
                var hasNotified by remember { mutableStateOf(false) }
                
                var offsetX by remember { mutableFloatStateOf(params.x.toFloat()) }
                var offsetY by remember { mutableFloatStateOf(params.y.toFloat()) }

                LaunchedEffect(isRunning) {
                    while (isRunning) {
                        delay(1000L)
                        elapsedSeconds++
                    }
                }

                LaunchedEffect(isRunning, elapsedSeconds, targetMinutes) {
                    if (isRunning && targetMinutes > 0 && !hasNotified) {
                        if (elapsedSeconds >= targetMinutes * 60) {
                            hasNotified = true
                            showNotification(appContext, title, targetMinutes)
                        }
                    }
                }

                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                val timeString = if (hours > 0) {
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }

                Card(
                    modifier = Modifier
                        .width(260.dp)
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                params.x = offsetX.roundToInt()
                                params.y = offsetY.roundToInt()
                                windowManager.updateViewLayout(floatingView, params)
                            }
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { stopSelf() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 28.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target minutes configuration row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("目標通知(分):", fontSize = 12.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { if (targetMinutes > 0) targetMinutes -= 5 },
                                    contentPadding = PaddingValues(4.dp),
                                    modifier = Modifier.size(28.dp)
                                ) { Text("-", fontSize = 10.sp) }
                                Text("${targetMinutes}分", fontSize = 12.sp)
                                OutlinedButton(
                                    onClick = { targetMinutes += 1; hasNotified = false },
                                    contentPadding = PaddingValues(4.dp),
                                    modifier = Modifier.size(28.dp)
                                ) { Text("+", fontSize = 10.sp) }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isRunning = !isRunning },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(if (isRunning) "ストップ" else "再開")
                            }

                            OutlinedButton(
                                onClick = {
                                    isRunning = false
                                    elapsedSeconds = 0L
                                    hasNotified = false
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("リセット")
                            }
                        }
                    }
                }
            }
        }

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

    private fun showNotification(context: Context, title: String, elapsedMinutes: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "timer_target_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Timer Target Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("タイマー目標達成通知")
            .setContentText("「$title」の計測が設定時間（$elapsedMinutes 分）に達しました！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            android.util.Log.e("FloatingTimerService", "Failed to show notification", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }
}
