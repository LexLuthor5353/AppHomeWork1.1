package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {
    private lateinit var binding: FragmentNewPostBinding
    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getLong("postId", 0L) ?: 0L
        val initialContent = arguments?.getString("content") ?: ""

        binding.content.setText(initialContent)
        binding.content.setSelection(initialContent.length)

        binding.save.setOnClickListener {
            val content = binding.content.text.toString().trim()
            if (content.isBlank()) {
                binding.content.error = getString(R.string.error_empty_content)
                return@setOnClickListener
            }

            if (postId != 0L) {
                viewModel.editById(postId)
            } else {
                viewModel.clearEditor()
            }

            viewModel.save(content)

            setFragmentResult("new_post_request_key", bundleOf("refresh" to true))

            parentFragmentManager.popBackStack()
        }

        binding.close.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}