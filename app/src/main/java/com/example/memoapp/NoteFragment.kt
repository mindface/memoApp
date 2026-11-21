package com.example.memoapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoapp.databinding.FragmentNoteEditBinding
import com.example.memoapp.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NoteFragment : Fragment() {
    private var _binding: FragmentNoteEditBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var existingNote: Note? = null
    private var editorContentText by mutableStateOf("")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val noteId = arguments?.getString("noteId")
        if (noteId != null) {
            db.collection("notes").document(noteId).get()
                .addOnSuccessListener { doc ->
                    existingNote = doc.toObject(Note::class.java)?.apply { id = doc.id }
                    existingNote?.let {
                        binding.editNoteTitle.setText(it.title)
                        binding.editNoteContent.setText(it.content)
                        editorContentText = it.content
                    }
                }
        }

        binding.editNoteContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                editorContentText = s?.toString() ?: ""
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupMinimap()

        binding.buttonSaveNote.setOnClickListener { saveNote() }
        binding.buttonCancelNote.setOnClickListener { findNavController().navigateUp() }
        binding.buttonNoteInfo.setOnClickListener { checkOverlayPermissionAndStart() }
    }

    private fun setupMinimap() {
        binding.composeViewNoteMinimap.setContent {
            val lines = remember(editorContentText) { editorContentText.lines() }
            com.example.memoapp.ui.components.ScrollbarWithJumpModal(
                lines = lines,
                onJumpToLine = { index ->
                    if (lines.isNotEmpty()) {
                        val ratio = index.toFloat() / lines.size.coerceAtLeast(1)
                        val child = binding.scrollNoteContent.getChildAt(0)
                        val maxScroll = if (child != null) child.height - binding.scrollNoteContent.height else 0
                        val targetScroll = (maxScroll * ratio).toInt().coerceAtLeast(0)
                        binding.scrollNoteContent.smoothScrollTo(0, targetScroll)
                    }
                },
                modifier = Modifier.fillMaxHeight()
            )
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
        val title = binding.editNoteTitle.text.toString()
        val content = binding.editNoteContent.text.toString()
        val id = existingNote?.id ?: ""
        
        val intent = Intent(requireContext(), com.example.memoapp.service.FloatingNoteService::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("content", content)
        }
        requireContext().startService(intent)
        Toast.makeText(context, "小窓モードで起動しました", Toast.LENGTH_SHORT).show()
    }

    private fun saveNote() {
        val userId = auth.currentUser?.uid ?: return
        val title = binding.editNoteTitle.text?.toString().orEmpty().trim()
        val content = binding.editNoteContent.text?.toString().orEmpty()

        if (title.isBlank() && content.isBlank()) {
            Toast.makeText(requireContext(), "タイトルまたは本文を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        val noteId = existingNote?.id ?: db.collection("notes").document().id
        val newNote = Note(
            id = noteId,
            userId = userId,
            title = title,
            content = content,
            created_at = existingNote?.created_at ?: System.currentTimeMillis().toString(),
            updated_at = System.currentTimeMillis().toString()
        )

        binding.buttonSaveNote.isEnabled = false
        db.collection("notes").document(noteId).set(newNote)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                binding.buttonSaveNote.isEnabled = true
                Toast.makeText(requireContext(), "保存に失敗しました", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
