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
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentLoginBinding
import ru.netology.nmedia.viewmodel.LoginViewModel

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentLoginBinding.inflate(inflater, container, false)

        binding.login.addTextChangedListener {
            binding.loginLayout.error = null
        }
        binding.password.addTextChangedListener {
            binding.passwordLayout.error = null
        }

        binding.signIn.setOnClickListener {
            val login = binding.login.text?.toString().orEmpty()
            val pass = binding.password.text?.toString().orEmpty()
            if (login.isBlank()) {
                binding.loginLayout.error = getString(R.string.login_empty)
                return@setOnClickListener
            }
            if (pass.isBlank()) {
                binding.passwordLayout.error = getString(R.string.password_empty)
                return@setOnClickListener
            }
            viewModel.login(login, pass)
        }

        viewModel.loading.observe(viewLifecycleOwner) {
            binding.progress.isVisible = it
            binding.signIn.isEnabled = !it
        }

        viewModel.loginError.observe(viewLifecycleOwner) {
            binding.loginLayout.error = getString(R.string.wrong_login_password)
            binding.passwordLayout.error = getString(R.string.wrong_login_password)
            Snackbar.make(binding.root, R.string.wrong_login_password, Snackbar.LENGTH_LONG).show()
        }

        viewModel.loginSuccess.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        return binding.root
    }
}
