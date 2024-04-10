package ru.netology.nework.activity.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentSignUpBinding
import ru.netology.nework.util.AndroidUtils
import javax.inject.Inject
@AndroidEntryPoint
class SignUpFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth
    private val viewModel: SignUpViewModel by viewModels(ownerProducer = ::requireActivity)
    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)

        //загрузка аватарки
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            when(it.resultCode){
                ImagePicker.RESULT_ERROR ->{
                    Snackbar.make(
                        binding.root,
                        ImagePicker.getError(it.data),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                Activity.RESULT_OK ->{
                    val uri: Uri? = it.data?.data
                    viewModel.changePhoto(uri, uri?.toFile())
                }
            }
        }
        binding.photo.setBackgroundResource(R.drawable.monogram)
        binding.photo.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(2048)
                .galleryMimeTypes(
                    arrayOf(
                        "image/png",
                        "image/jpeg"
                    )
                )
                .start(20)

        }


        viewModel.photo.observe(viewLifecycleOwner) {

            binding.photo.setImageURI(it.uri)
        }
        //доступность кнопки входа
        enableLogin()
        binding.name.doOnTextChanged{text, start, before, count ->
            enableLogin()
        }
        binding.login.doOnTextChanged{text, start, before, count ->
            enableLogin()
        }

        binding.pass.doOnTextChanged{text, start, before, count ->
            enableLogin()
        }
        binding.passRepeat.doOnTextChanged{text, start, before, count ->
            enableLogin()
        }
        //регистрация
        binding.signUp.setOnClickListener {
            AndroidUtils.hideKeyboard(requireView())
            if (binding.pass.text.toString() != binding.passRepeat.text.toString()) {
                Snackbar.make(binding.root,
                    getString(R.string.passwords_don_t_match), Snackbar.LENGTH_LONG).show()
            } else {
                viewModel.name.value = binding.name.text.toString()
                viewModel.login.value = binding.login.text.toString()
                viewModel.pass.value = binding.pass.text.toString()
                viewModel.signUp()
            }

        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            auth.setAuth(state.id, state.token!!)
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

    private fun enableLogin(){
        binding.signUp.isEnabled = binding.name.text.toString().isNotEmpty() &&
                binding.login.text.toString().isNotEmpty() &&
                binding.pass.text.toString().isNotEmpty() &&
                binding.passRepeat.text.toString().isNotEmpty()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                //Image Uri will not be null for RESULT_OK
                val uri: Uri = data?.data!!

                // Use Uri object instead of File to avoid storage permissions
                viewModel.changePhoto(uri, uri.toFile())
                binding.photo.setImageURI(uri)
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(requireContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
            }
        }
    }

}