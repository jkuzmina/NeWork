package ru.netology.nework.activity.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentSignInBinding
import ru.netology.nework.util.AndroidUtils

class SignInFragment : Fragment() {
    private val viewModel: SignInViewModel by viewModels(ownerProducer = ::requireParentFragment)
    private lateinit var binding: FragmentSignInBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)

        //доступность кнопки входа
        binding.login.doOnTextChanged{text, start, before, count ->
            enableLogin()
        }

        passError()
        binding.pass.doOnTextChanged{text, start, before, count ->
            if(text == null){
                passError()
            }
            else{
                binding.passLayout.error = null
            }
            enableLogin()
        }

        binding.signIn.isEnabled = binding.login.text.toString().isNotEmpty() && binding.pass.text.toString().isNotEmpty()

        binding.signIn.setOnClickListener{
            AndroidUtils.hideKeyboard(requireView())
            viewModel.login.value = binding.login.text.toString()
            viewModel.pass.value = binding.pass.text.toString()
            viewModel.signIn()
        }

        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            AppAuth.getInstance().setAuth(state.id, state.token!!)
            findNavController().navigateUp()
        }

        viewModel.userAuthResult.observe(viewLifecycleOwner) { state ->
            if (state.error) {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                    .show()
            }
        }

        return binding.root
    }
    private fun passError(){
        binding.passLayout.error = getString(R.string.password_cannot_be_empty)
    }

    private fun enableLogin(){
        binding.signIn.isEnabled = binding.login.text.toString().isNotEmpty() && binding.pass.text.toString().isNotEmpty()
    }



}