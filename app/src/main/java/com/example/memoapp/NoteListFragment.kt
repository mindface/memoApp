package com.example.memoapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoapp.databinding.FragmentNoteListBinding
import com.example.memoapp.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val notes = mutableListOf<Note>()
    private val filteredNotes = mutableListOf<Note>()
    private lateinit var adapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = Firebase.firestore
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        setupSearchView()

        val currentUser = auth.currentUser
        Log.d("Firestore", "User ID: ${currentUser?.uid}")
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
        } else {
            fetchNotes(currentUser.uid)
        }

        binding.fabAddNote.setOnClickListener {
            findNavController().navigate(R.id.action_NoteListFragment_to_NoteFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            filteredNotes,
            onClick = { note ->
                val bundle = Bundle().apply { putString("noteId", note.id) }
                findNavController().navigate(R.id.action_NoteListFragment_to_NoteFragment, bundle)
            },
            onDelete = { note -> showDeleteConfirmationDialog(note.id) }
        )
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotes.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchViewNotes.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText ?: "")
                return true
            }
        })
    }

    private fun filter(text: String) {
        filteredNotes.clear()
        if (text.isEmpty()) {
            filteredNotes.addAll(notes)
        } else {
            val query = text.lowercase()
            for (note in notes) {
                if (note.title.lowercase().contains(query) || note.content.lowercase().contains(query)) {
                    filteredNotes.add(note)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchNotes(userId: String) {
        db.collection("notes")
            .where(
                Filter.or(
                    Filter.equalTo("user_id", userId),
                    Filter.equalTo("userId", userId)
                )
            )
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val fetchedNotes = snapshots.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Note::class.java)?.apply { id = doc.id }
                        } catch (err: Exception) {
                            null
                        }
                    }
                    
                    notes.clear()
                    notes.addAll(fetchedNotes)
                    notes.sortByDescending { it.getUpdatedAtString() }

                    filter(binding.searchViewNotes.query.toString())
                }
            }
    }

    private fun deleteNote(noteId: String) {
        db.collection("notes").document(noteId).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(noteId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("ノートの削除")
            .setMessage("このノートを削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                deleteNote(noteId)
            }
            .setNegativeButton("キャンセル", null)
            .create()
            .apply { makeFullScreen() }
            .show()
    }

    inner class NoteAdapter(
        private val list: List<Note>,
        private val onClick: (Note) -> Unit,
        private val onDelete: (Note) -> Unit
    ) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.text_title)
            val content: TextView = v.findViewById(R.id.text_content)
            val btnDelete: View = v.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val note = list[position]
            holder.title.text = note.title
            holder.content.text = note.content
            holder.itemView.setOnClickListener { onClick(note) }
            holder.btnDelete.setOnClickListener { onDelete(note) }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
