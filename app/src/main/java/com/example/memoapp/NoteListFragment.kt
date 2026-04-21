package com.example.memoapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoapp.databinding.FragmentNoteListBinding
import com.example.memoapp.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        if (currentUser == null) {
            // Redirect to login if not authenticated (extra safety)
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
        } else {
            fetchNotes(currentUser.uid)
        }

        binding.fabAddNote.setOnClickListener {
            showAddEditNoteDialog(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(filteredNotes) { note ->
            showAddEditNoteDialog(note)
        }
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
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    notes.clear()
                    notes.addAll(snapshots.toObjects(Note::class.java))
                    // Sort by updated_at descending
                    notes.sortByDescending { it.updated_at }
                    
                    // Apply current filter
                    filter(binding.searchViewNotes.query.toString())

                    Log.d("Firestore", "Updated notes: ${notes.size}")
                }
            }
    }

    private fun showAddEditNoteDialog(note: Note?) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_note, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.edit_note_title)
        val editContent = dialogView.findViewById<EditText>(R.id.edit_note_content)

        note?.let {
            editTitle.setText(it.title)
            editContent.setText(it.content)
            builder.setTitle("Edit Note")
            builder.setNeutralButton("Delete") { _, _ ->
                deleteNote(it.id)
            }
        } ?: builder.setTitle("New Note")

        builder.setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = editTitle.text.toString()
                val content = editContent.text.toString()
                saveNote(note, title, content)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNote(existingNote: Note?, title: String, content: String) {
        val userId = auth.currentUser?.uid ?: return
        val noteId = existingNote?.id ?: db.collection("notes").document().id
        
        val note = Note(
            id = noteId,
            userId = userId,
            title = title,
            content = content,
            created_at = existingNote?.created_at ?: System.currentTimeMillis().toString(),
            updated_at = System.currentTimeMillis().toString()
        )

        db.collection("notes").document(noteId).set(note)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
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

    inner class NoteAdapter(
        private val list: List<Note>,
        private val onClick: (Note) -> Unit
    ) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.text_title)
            val content: TextView = v.findViewById(R.id.text_content)
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
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
