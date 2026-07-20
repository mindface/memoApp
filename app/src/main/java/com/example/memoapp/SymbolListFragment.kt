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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoapp.databinding.FragmentSymbolListBinding
import com.example.memoapp.model.Symbol
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SymbolListFragment : Fragment() {

    private var _binding: FragmentSymbolListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val symbols = mutableListOf<Symbol>()
    private val filteredSymbols = mutableListOf<Symbol>()
    private lateinit var adapter: SymbolAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSymbolListBinding.inflate(inflater, container, false)
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
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
        } else {
            fetchSymbols(currentUser.uid)
        }

        binding.fabAddSymbol.setOnClickListener {
            showAddEditSymbolDialog(null)
        }
    }

    private fun showAddEditSymbolDialog(symbol: Symbol?) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_symbol, null)
        
        val editTitle = dialogView.findViewById<TextInputEditText>(R.id.edit_symbol_title)
        val editContent = dialogView.findViewById<TextInputEditText>(R.id.edit_symbol_content)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_language)
        val btnFillTest = dialogView.findViewById<View>(R.id.btn_fill_test_data)

        symbol?.let {
            editTitle.setText(it.title)
            editContent.setText(it.content)
            // Set chip selection based on language
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip
                if (chip?.text == it.language) {
                    chip.isChecked = true
                    break
                }
            }
        }

        btnFillTest.setOnClickListener {
            val selectedChipId = chipGroup.checkedChipId
            val selectedLanguage = if (selectedChipId != View.NO_ID) {
                dialogView.findViewById<Chip>(selectedChipId).text.toString()
            } else ""
            
            val testContent = when (selectedLanguage) {
                "Rust" -> "fn main() {\n    println!(\"Hello, Rust!\");\n}"
                "Kotlin" -> "fun main() {\n    println(\"Hello, Kotlin!\")\n}"
                "Java" -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Java!\");\n    }\n}"
                "Python" -> "print(\"Hello, Python!\")"
                else -> "// Select a language to see sample code"
            }
            editContent.setText(testContent)
        }

        builder.setView(dialogView)
            .setTitle(if (symbol == null) "New Symbol" else "Edit Symbol")
            .setPositiveButton("Save") { _, _ ->
                val title = editTitle.text.toString()
                val content = editContent.text.toString()
                val selectedChipId = chipGroup.checkedChipId
                val language = if (selectedChipId != View.NO_ID) {
                    dialogView.findViewById<Chip>(selectedChipId).text.toString()
                } else ""
                
                saveSymbol(symbol, title, content, language)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSymbol(existingSymbol: Symbol?, title: String, content: String, language: String) {
        val userId = auth.currentUser?.uid ?: return
        val symbolId = existingSymbol?.id ?: db.collection("symbols").document().id

        val symbol = Symbol(
            id = symbolId,
            userId = userId,
            title = title,
            content = content,
            created_at = existingSymbol?.created_at ?: System.currentTimeMillis().toString(),
            updated_at = System.currentTimeMillis().toString(),
            language = language
        )

        db.collection("symbols").document(symbolId).set(symbol)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Symbol saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        adapter = SymbolAdapter(filteredSymbols) { symbol ->
            showAddEditSymbolDialog(symbol)
        }
        binding.recyclerViewSymbols.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSymbols.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchViewSymbols.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
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
        filteredSymbols.clear()
        if (text.isEmpty()) {
            filteredSymbols.addAll(symbols)
        } else {
            val query = text.lowercase()
            for (symbol in symbols) {
                if (symbol.title.lowercase().contains(query) || 
                    symbol.content.lowercase().contains(query) ||
                    symbol.language.lowercase().contains(query)) {
                    filteredSymbols.add(symbol)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun fetchSymbols(userId: String) {
        db.collection("symbols")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    symbols.clear()
                    symbols.addAll(snapshots.toObjects(Symbol::class.java))
                    // Sort by updated_at descending if available
                    symbols.sortByDescending { it.updated_at }

                    filter(binding.searchViewSymbols.query.toString())
                    Log.d("Firestore", "Updated symbols: ${symbols.size}")
                }
            }
    }

    inner class SymbolAdapter(
        private val list: List<Symbol>,
        private val onClick: (Symbol) -> Unit
    ) : RecyclerView.Adapter<SymbolAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.text_symbol_title)
            val content: TextView = v.findViewById(R.id.text_symbol_content)
            val chipLanguage: Chip = v.findViewById(R.id.chip_language)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_symbol, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val symbol = list[position]
            holder.title.text = symbol.title
            holder.content.text = symbol.content
            
            if (symbol.language.isNotEmpty()) {
                holder.chipLanguage.visibility = View.VISIBLE
                holder.chipLanguage.text = symbol.language
            } else {
                holder.chipLanguage.visibility = View.GONE
            }
            
            holder.itemView.setOnClickListener { onClick(symbol) }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
