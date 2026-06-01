package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentRegistrationBinding
import ru.netology.nmedia.viewmodel.RegistrationViewModel

class RegistrationFragment : Fragment() {

    private val viewModel: RegistrationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentRegistrationBinding.inflate(inflater, container, false)

        binding.name.addTextChangedListener { binding.nameLayout.error = null }
        binding.login.addTextChangedListener { binding.loginLayout.error = null }
        binding.password.addTextChangedListener { binding.passwordLayout.error = null }
        binding.confirm.addTextChangedListener { binding.confirmLayout.error = null }

        binding.register.setOnClickListener {
            val name = binding.name.text?.toString().orEmpty()
            val login = binding.login.text?.toString().orEmpty()
            val pass = binding.password.text?.toString().orEmpty()
            val confirm = binding.confirm.text?.toString().orEmpty()

            if (name.isBlank()) {
                binding.nameLayout.error = getString(R.string.name_empty)
                return@setOnClickListener
            }
            if (login.isBlank()) {
                binding.loginLayout.error = getString(R.string.login_empty)
                return@setOnClickListener
            }
            if (pass.isBlank()) {
                binding.passwordLayout.error = getString(R.string.password_empty)
                return@setOnClickListener
            }
            if (confirm.isBlank()) {
                binding.confirmLayout.error = getString(R.string.password_empty)
                return@setOnClickListener
            }

            viewModel.register(name, login, pass, confirm)
        }

        viewModel.loading.observe(viewLifecycleOwner) {
            binding.progress.isVisible = it
            binding.register.isEnabled = !it
        }

        viewModel.passwordMismatch.observe(viewLifecycleOwner) {
            binding.confirmLayout.error = getString(R.string.passwords_not_match)
            Snackbar.make(binding.root, R.string.passwords_not_match, Snackbar.LENGTH_LONG).show()
        }

        viewModel.registerError.observe(viewLifecycleOwner) {
            Snackbar.make(binding.root, R.string.registration_error, Snackbar.LENGTH_LONG).show()
        }

        viewModel.registerSuccess.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        return binding.root
    }
}
