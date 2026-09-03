package com.example.memoapp

import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData
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
import com.example.memoapp.model.Symbol
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.text.TextWatcher
import android.text.Editable
import android.view.WindowManager
import android.widget.ImageButton

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val notes = mutableListOf<Note>()
    private val filteredNotes = mutableListOf<Note>()
    private val allSymbols = mutableListOf<Symbol>()
    private lateinit var adapter: NoteAdapter

    private lateinit var editTitle: EditText
    private lateinit var editContent: EditText
    private var existingNote: Note? = null
    private var noteId: String = ""

    // Dialog state for dynamic updates
    private var activeChipGroupTypes: ChipGroup? = null
    private var activeChipGroupSymbols: ChipGroup? = null
    private var activeEditSymbolSearch: TextInputEditText? = null

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
            // Redirect to login if not authenticated (extra safety)
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
        } else {
            fetchNotes(currentUser.uid)
            fetchSymbols(currentUser.uid)
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
                    Log.d("Firestore", "Notes snapshot received. Count: ${snapshots.size()}")
                    val fetchedNotes = snapshots.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Note::class.java)?.apply { 
                                id = doc.id 
                                Log.d("Firestore", "Note fetched: $title, ID: $id")
                            }
                        } catch (err: Exception) {
                            Log.e("Firestore", "Error parsing note ${doc.id}", err)
                            null
                        }
                    }
                    
                    notes.clear()
                    notes.addAll(fetchedNotes)
                    // Sort by updated_at descending
                    notes.sortByDescending { it.getUpdatedAtString() }

                    // Apply current filter
                    filter(binding.searchViewNotes.query.toString())

                    Log.d("Firestore", "Final notes list count: ${notes.size}")
                }
            }
    }

    private fun fetchSymbols(userId: String) {
        db.collection("symbols")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed symbols.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val newSymbols = snapshots.toObjects(Symbol::class.java)
                    allSymbols.clear()
                    
                    // IDをドキュメントから取得して補完
                    snapshots.documents.forEachIndexed { index, doc ->
                        if (index < newSymbols.size) {
                            val symbol = newSymbols[index]
                            symbol.id = doc.id
                            allSymbols.add(symbol)
                        }
                    }
                    // Sort by updated_at descending
                    allSymbols.sortByDescending { it.updated_at }
                    Log.d("Firestore", "Fetched symbols: ${allSymbols.size}")
                    
                    // Update dialog if active
                    if (isAdded && activeChipGroupTypes != null) {
                        populateTypeChips(activeChipGroupTypes!!, activeEditSymbolSearch!!, activeChipGroupSymbols!!)
                        refreshDialogSymbols()
                    }
                }
            }
    }

    private fun showAddEditNoteDialog(note: Note?) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_note, null)
        editTitle = dialogView.findViewById(R.id.edit_note_title)
        editContent = dialogView.findViewById(R.id.edit_note_content)
        val editSymbolSearch = dialogView.findViewById<TextInputEditText>(R.id.edit_symbol_search)
        val chipGroupTypes = dialogView.findViewById<ChipGroup>(R.id.chip_group_types)
        val chipGroupSymbols = dialogView.findViewById<ChipGroup>(R.id.chip_group_symbols)

        existingNote = note
        noteId = note?.id ?: db.collection("notes").document().id

        note?.let {
            editTitle.setText(it.title)
            editContent.setText(it.content)
            builder.setNeutralButton("Delete") { _, _ ->
                showDeleteConfirmationDialog(it.id)
            }
        }

        activeChipGroupTypes = chipGroupTypes
        activeChipGroupSymbols = chipGroupSymbols
        activeEditSymbolSearch = editSymbolSearch

        // 1. Symbol Type (symbolType フィールド) のリストを作成
        populateTypeChips(chipGroupTypes, editSymbolSearch, chipGroupSymbols)

        chipGroupTypes.setOnCheckedStateChangeListener { group, checkedIds ->
            refreshDialogSymbols()
        }

        // Symbol Search Logic
        editSymbolSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                refreshDialogSymbols()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Initial update
        updateSymbolChipsByFilter("", "", chipGroupSymbols)

        val dialog = builder.setView(dialogView)
            .setOnDismissListener {
                activeChipGroupTypes = null
                activeChipGroupSymbols = null
                activeEditSymbolSearch = null
            }
            .create()

        val btnExpand = dialogView.findViewById<ImageButton>(R.id.btn_expand_full)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_close)
        val btnSave = dialogView.findViewById<View>(R.id.btn_save_note)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            saveNote(existingNote, editTitle.text.toString(), editContent.text.toString())
            dialog.dismiss()
        }

        btnExpand.setOnClickListener {
            // ダイアログのウィンドウを取得
            dialog.window?.let { window ->
                // 全画面（MATCH_PARENT）に設定
                val layoutParams = window.attributes
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                window.attributes = layoutParams

                // 背景（外側の余白）を消して、隅々まで表示させる
                window.setBackgroundDrawableResource(android.R.color.white)
            }

            // ボタンを非表示にする、または「縮小」ボタンに切り替える処理
            btnExpand.visibility = View.GONE
        }

        dialog.show()
    }

    private fun populateTypeChips(chipGroup: ChipGroup, searchEdit: TextInputEditText, symbolsChipGroup: ChipGroup) {
        val currentContext = context ?: return
        val types = allSymbols.map { it.symbolType }.filter { it.isNotEmpty() }.distinct().sorted()
        
        // Keep track of current selection to restore it
        val checkedChip = chipGroup.findViewById<Chip>(chipGroup.checkedChipId)
        val currentSelectedText = checkedChip?.text?.toString() ?: "すべて"

        chipGroup.removeAllViews()
        
        // 「すべて」のチップを追加
        val allChip = Chip(currentContext)
        allChip.id = View.generateViewId()
        allChip.text = "すべて"
        allChip.isCheckable = true
        allChip.isChecked = (currentSelectedText == "すべて")
        chipGroup.addView(allChip)

        // 各タイプごとのチップを追加
        types.forEach { type ->
            val chip = Chip(currentContext)
            chip.id = View.generateViewId()
            chip.text = type
            chip.isCheckable = true
            chip.isChecked = (currentSelectedText == type)
            chipGroup.addView(chip)
        }
        
        // Ensure at least one is checked if none was restored
        if (chipGroup.checkedChipId == View.NO_ID) {
            allChip.isChecked = true
        }
    }

    private fun refreshDialogSymbols() {
        val group = activeChipGroupTypes ?: return
        val searchEdit = activeEditSymbolSearch ?: return
        val symbolsGroup = activeChipGroupSymbols ?: return

        val checkedId = group.checkedChipId
        val chip = group.findViewById<Chip>(checkedId)
        val chipText = chip?.text?.toString() ?: ""
        val selectedType = if (chipText == "すべて") "" else chipText
        updateSymbolChipsByFilter(selectedType, searchEdit.text.toString(), symbolsGroup)
    }

    private fun updateSymbolChipsByFilter(type: String, query: String, chipGroup: ChipGroup) {
        val currentContext = context ?: return
        chipGroup.removeAllViews()
        Log.d("SymbolSearch", "Filtering symbols. Type: '$type', Query: '$query', TotalSymbols: ${allSymbols.size}")
        
        var filtered = allSymbols.asSequence()

        if (type.isNotEmpty()) {
            filtered = filtered.filter { it.symbolType == type }
        }

        if (query.isNotEmpty()) {
            val q = query.lowercase()
            filtered = filtered.filter { 
                it.title.lowercase().contains(q) || 
                it.language.lowercase().contains(q) ||
                it.content.lowercase().contains(q)
            }
        }

        val resultList = filtered.toList()
        Log.d("SymbolSearch", "Filtered result size: ${resultList.size}")

        if (resultList.isEmpty()) {
            val emptyText = TextView(currentContext)
            emptyText.text = if (allSymbols.isEmpty()) "シンボルが登録されていません" else "一致するシンボルがありません"
            emptyText.setPadding(16, 16, 16, 16)
            chipGroup.addView(emptyText)
            return
        }

        for (symbol in resultList) {
            val chip = Chip(currentContext)
            chip.text = if (symbol.language.isNotEmpty()) "[${symbol.language}] ${symbol.title}" else symbol.title
            
            chip.setOnClickListener {
                if (symbol.content.isNotEmpty()) {
                    insertTextAtCursor(symbol.content)
                    Toast.makeText(currentContext, "「${symbol.title}」を挿入しました", Toast.LENGTH_SHORT).show()
                }
            }

            chip.setOnLongClickListener {
                if (symbol.content.isNotEmpty()) {
                    val clipboard = currentContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("symbol_content", symbol.content)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(currentContext, "「${symbol.title}」をコピーしました", Toast.LENGTH_SHORT).show()
                }
                true
            }

            chipGroup.addView(chip)
        }
    }

    private fun updateSymbolChips(query: String, chipGroup: ChipGroup) {
        // This is replaced by updateSymbolChipsByFilter
    }

    private fun insertTextAtCursor(textToInsert: String) {
        val start = editContent.selectionStart
        val end = editContent.selectionEnd
        val originalText = editContent.text

        if (start >= 0) {
            originalText.replace(Math.min(start, end), Math.max(start, end), textToInsert)
            editContent.setSelection(start + textToInsert.length)
        } else {
            editContent.append(textToInsert)
        }
    }

    private fun saveNote(existingNote: Note?, title: String, content: String) {
        val userId = auth.currentUser?.uid ?: return

        val note = Note(
            id = noteId,
            userId = userId,
            title = title,
            content = content,
            created_at = existingNote?.created_at ?: System.currentTimeMillis().toString(),
            updated_at = System.currentTimeMillis().toString()
        )
        db.collection("notes").document(noteId).set(note)
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
            }
//        db.collection("notes").document(noteId).set(note)
//            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
//            }
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
            .show()
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
