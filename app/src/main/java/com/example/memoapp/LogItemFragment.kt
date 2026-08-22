package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoapp.databinding.FragmentLogItemBinding
import com.example.memoapp.model.LogItem

class LogItemFragment : Fragment() {
    private var _binding: FragmentLogItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: LogItemRepository
    private var existingItem: LogItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = LogItemRepository(requireContext().applicationContext)
        
        val logItemId = arguments?.getString("logItemId")
        if (logItemId != null) {
            existingItem = repository.getById(logItemId)
            existingItem?.let {
                binding.editLogItemTitle.setText(it.title)
                binding.editLogItemContent.setText(it.content)
            }
        }

        binding.buttonSaveLogItem.setOnClickListener { saveLogItem() }
        binding.buttonCancelLogItem.setOnClickListener { findNavController().navigateUp() }
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
                        updatedAt = System.currentTimeMillis().toString()
                    )
                    repository.save(updated)
                    updated
                } else {
                    repository.create(title, content)
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
