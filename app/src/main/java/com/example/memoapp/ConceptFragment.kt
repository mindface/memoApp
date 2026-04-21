package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.memoapp.databinding.FragmentConceptBinding

class ConceptFragment : Fragment() {

    private var _binding: FragmentConceptBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConceptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // You can set image and text dynamically here if needed
        // binding.imageConcept.setImageResource(R.drawable.your_image)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
