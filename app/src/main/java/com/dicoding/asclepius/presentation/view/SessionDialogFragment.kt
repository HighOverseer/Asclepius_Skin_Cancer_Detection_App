package com.dicoding.asclepius.presentation.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.DialogFragmentSessionBinding
import com.dicoding.asclepius.presentation.utils.showToast

class SessionDialogFragment : DialogFragment() {

    private var binding: DialogFragmentSessionBinding? = null

    private var listener: OnDismissListener? = null

    private var latestSessionName: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = requireActivity() as? OnDismissListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NO_TITLE,
            android.R.style.Theme_Material_Dialog_NoActionBar
        )
    }

    @SuppressLint("DialogFragmentCallbacksDetector")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogFragmentSessionBinding.inflate(
            inflater,
            container,
            false
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            btnDone.setOnClickListener {
                val sessionName = etSession.text?.toString()?.trim()
                if (sessionName.isNullOrBlank()) {
                    showToast(getString(R.string.session_name_can_t_be_empty))
                    return@setOnClickListener
                }

                latestSessionName = sessionName
                dismiss()
            }

            btnCancel.setOnClickListener {
                dialog?.cancel()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        latestSessionName?.let {
            listener?.onDismiss(it)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    interface OnDismissListener {
        fun onDismiss(sessionName: String)
    }

}