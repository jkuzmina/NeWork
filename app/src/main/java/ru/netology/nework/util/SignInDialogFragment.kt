package ru.netology.nework.util

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import ru.netology.nework.R

class SignInDialogFragment : DialogFragment() {
    @Override
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.sign_in_dialog))
                .setPositiveButton(R.string.sign_in
                ) { _, _ ->
                    findNavController().navigate(R.id.action_global_signInFragment)
                }
                .setNegativeButton(getString(R.string.dialog_cancel)
                ) { _, _ ->
                    // User cancelled the dialog
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}