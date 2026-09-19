package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoapp.databinding.FragmentLogItemBinding
import com.example.memoapp.model.LogItem
import com.example.memoapp.model.Symbol
import com.example.memoapp.ui.log.LogItemDetailModal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LogItemFragment : Fragment() {
    private var _binding: FragmentLogItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: LogItemRepository
    private var existingItem: LogItem? = null

    private var currentTextSize = 16f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var showModal by mutableStateOf(false)
    private var isSharedState by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = LogItemRepository(requireContext().applicationContext)
        
        setupZoomGestures()

        val logItemId = arguments?.getString("logItemId")
        if (logItemId != null) {
            existingItem = repository.getById(logItemId)
            existingItem?.let {
                binding.editLogItemTitle.setText(it.title)
                binding.editLogItemContent.setText(it.content)
                isSharedState = it.isShared
            }
        }

        binding.buttonSaveLogItem.setOnClickListener { saveLogItem() }
        binding.buttonCancelLogItem.setOnClickListener { findNavController().navigateUp() }
        binding.buttonLogItemInfo.setOnClickListener { showModal = true }

        setupModal()
    }

    private fun setupModal() {
        binding.composeViewModal.setContent {
            if (showModal) {
                LogItemDetailModal(
                    isShared = isSharedState,
                    noteContent = binding.editLogItemContent.text.toString(),
                    onToggleShare = { shared ->
                        isSharedState = shared
                        syncLogItemToCloud(shared)
                    },
                    onSymbolize = { symbolizeCurrentNote() },
                    onShareExternally = { shareExternally() },
                    onPopOut = { checkOverlayPermissionAndStart() },
                    onCopy = { text ->
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("note", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                        showModal = false
                    },
                    onDismiss = { showModal = false }
                )
            }
        }
    }

    private fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent)
            Toast.makeText(context, "他のアプリの上に重ねて表示を許可してください", Toast.LENGTH_LONG).show()
        } else {
            startFloatingNote()
        }
    }

    private fun startFloatingNote() {
        val title = binding.editLogItemTitle.text.toString()
        val content = binding.editLogItemContent.text.toString()
        val id = existingItem?.id ?: ""
        
        val intent = Intent(requireContext(), com.example.memoapp.service.FloatingNoteService::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("content", content)
        }
        requireContext().startService(intent)
        showModal = false
        // Optionally navigate up if we want to "leave" the app but keep the note
        // findNavController().navigateUp()
    }

    private fun syncLogItemToCloud(shared: Boolean) {
        FirebaseAuth.getInstance().currentUser ?: return
        val item = existingItem ?: return // Only sync existing items for now, or save first
        
        val db = Firebase.firestore
        if (shared) {
            db.collection("log_items").document(item.id).set(
                item.copy(isShared = true, updatedAt = System.currentTimeMillis().toString())
            ).addOnSuccessListener {
                Toast.makeText(context, "クラウドに同期しました", Toast.LENGTH_SHORT).show()
                // Update local status too
                repository.save(item.copy(isShared = true))
            }
        } else {
            db.collection("log_items").document(item.id).delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "クラウドから削除しました", Toast.LENGTH_SHORT).show()
                    repository.save(item.copy(isShared = false))
                }
        }
    }

    private fun symbolizeCurrentNote() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val title = binding.editLogItemTitle.text.toString()
        val content = binding.editLogItemContent.text.toString()
        
        val symbol = Symbol(
            id = Firebase.firestore.collection("symbols").document().id,
            userId = user.uid,
            title = title.ifBlank { "ノートからのシンボル" },
            content = content,
            created_at = System.currentTimeMillis().toString(),
            updated_at = System.currentTimeMillis().toString()
        )

        Firebase.firestore.collection("symbols").document(symbol.id).set(symbol)
            .addOnSuccessListener {
                Toast.makeText(context, "シンボルとして保存しました", Toast.LENGTH_SHORT).show()
                showModal = false
            }
    }

    private fun shareExternally() {
        val title = binding.editLogItemTitle.text.toString()
        val content = binding.editLogItemContent.text.toString()
        val shareText = "# $title\n\n$content"

        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun setupZoomGestures() {
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentTextSize *= detector.scaleFactor
                // 文字サイズを 10sp 〜 50sp の範囲に制限
                currentTextSize = currentTextSize.coerceIn(10f, 50f)
                binding.editLogItemContent.textSize = currentTextSize
                return true
            }
        })

        binding.editLogItemContent.setOnTouchListener { v: View, event: MotionEvent ->
            // ピンチ操作（指2本以上）の場合はジェスチャー検出器に渡す
            if (event.pointerCount >= 2) {
                scaleGestureDetector.onTouchEvent(event)
                true // イベントを消費してスクロールを防ぐ
            } else {
                v.performClick()
                false // 通常のタッチ（スクロール等）はシステムに任せる
            }
        }
    }

    private fun saveLogItem() {
        val title = binding.editLogItemTitle.text?.toString().orEmpty().trim()
        val content = binding.editLogItemContent.text?.toString().orEmpty()
        if (title.isBlank() && content.isBlank()) {
            Toast.makeText(requireContext(), "タイトルまたは本文を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSaveLogItem.isEnabled = false
        Thread {
            val result = runCatching {
                val item = existingItem
                if (item != null) {
                    val updated = item.copy(
                        title = title,
                        content = content,
                        isShared = isSharedState,
                        updatedAt = System.currentTimeMillis().toString()
                    )
                    repository.save(updated)
                    updated
                } else {
                    val created = repository.create(title, content)
                    created.copy(isShared = isSharedState).also { repository.save(it) }
                }
            }
            activity?.runOnUiThread {
                binding.buttonSaveLogItem.isEnabled = true
                result.onSuccess {
                    Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }.onFailure {
                    Toast.makeText(requireContext(), "保存に失敗しました: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
