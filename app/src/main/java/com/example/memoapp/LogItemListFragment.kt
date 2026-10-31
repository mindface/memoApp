package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoapp.databinding.FragmentLogItemListBinding
import com.example.memoapp.model.LogItem

class LogItemListFragment : Fragment() {
    private var _binding: FragmentLogItemListBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: LogItemRepository
    private val items = mutableListOf<LogItem>()
    private lateinit var adapter: LogItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = LogItemRepository(requireContext().applicationContext)
        
        setupRecyclerView()
        binding.fabAddLogItem.setOnClickListener {
            findNavController().navigate(R.id.action_LogItemListFragment_to_LogItemFragment)
        }
        
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun setupRecyclerView() {
        adapter = LogItemAdapter(
            items,
            onClick = { item ->
                val bundle = Bundle().apply { putString("logItemId", item.id) }
                findNavController().navigate(R.id.action_LogItemListFragment_to_LogItemFragment, bundle)
            },
            onDelete = { item ->
                showDeleteConfirmationDialog(item.id)
            }
        )
        binding.recyclerViewLogItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLogItems.adapter = adapter
    }

    private fun loadItems() {
        items.clear()
        items.addAll(repository.getAll())
        adapter.notifyDataSetChanged()
    }

    private fun showDeleteConfirmationDialog(itemId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("ログの削除")
            .setMessage("このログを削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                repository.delete(itemId)
                loadItems()
            }
            .setNegativeButton("キャンセル", null)
            .create()
            .apply { makeFullScreen() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class LogItemAdapter(
        private val list: List<LogItem>,
        private val onClick: (LogItem) -> Unit,
        private val onDelete: (LogItem) -> Unit
    ) : RecyclerView.Adapter<LogItemAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.text_title)
            val content: TextView = v.findViewById(R.id.text_content)
            val btnDelete: View = v.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.title.text = item.title
            holder.content.text = item.content
            holder.itemView.setOnClickListener { onClick(item) }
            holder.btnDelete.setOnClickListener { onDelete(item) }
        }

        override fun getItemCount() = list.size
    }
}
