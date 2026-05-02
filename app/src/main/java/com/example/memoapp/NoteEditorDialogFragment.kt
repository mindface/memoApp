package com.example.memoapp

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.memoapp.databinding.DialogAddEditNoteBinding
import com.example.memoapp.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NoteEditorDialogFragment : DialogFragment() {

    private var _binding: DialogAddEditNoteBinding? = null
    private val binding get() = _binding!!

    private var note: Note? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val ARG_NOTE = "arg_note"

        fun newInstance(note: Note?): NoteEditorDialogFragment {
            val frag = NoteEditorDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_NOTE, note)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditNoteBinding.inflate(requireActivity().layoutInflater)

        note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_NOTE, Note::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_NOTE) as? Note
        }

        // データの初期表示セット
        note?.let {
            binding.editNoteTitle.setText(it.title)
            binding.editNoteContent.setText(it.content)
            binding.textStatus.text = "メモの編集"
        } ?: run {
            binding.textStatus.text = "新規作成"
        }

        // --- クリックリスナーの設定 (View Bindingスタイル) ---

        // 閉じるボタン (x) のクリック処理
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // 拡大ボタンのクリック処理
        binding.btnExpandFull.setOnClickListener {
            switchToFullScreen()
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("保存") { _, _ ->
                saveNote()
            }
            .setNegativeButton("キャンセル", null)
            .create()
    }

    private fun saveNote() {
        val userId = auth.currentUser?.uid ?: return
        val title = binding.editNoteTitle.text.toString()
        val content = binding.editNoteContent.text.toString()

        if (title.isEmpty() && content.isEmpty()) {
            return
        }

        val noteId = note?.id ?: db.collection("notes").document().id

        val newNote = Note(
            id = noteId,
            userId = userId,
            title = title,
            content = content,
            created_at = note?.created_at ?: System.currentTimeMillis().toString(),
            updated_at = System.currentTimeMillis().toString()
        )

        db.collection("notes").document(noteId).set(newNote)
            .addOnFailureListener {
                Toast.makeText(context, "保存に失敗しました", Toast.LENGTH_SHORT).show()
            }
    }

    private fun switchToFullScreen() {
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.white)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
