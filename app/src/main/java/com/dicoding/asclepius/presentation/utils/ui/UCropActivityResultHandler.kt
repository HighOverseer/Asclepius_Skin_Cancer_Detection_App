package com.dicoding.asclepius.presentation.utils.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import com.yalantis.ucrop.UCrop

class UCropActivityResultHandler(
    private val onHandleResult: OnHandleResult
) {

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode == RESULT_CANCELED -> {
                onHandleResult.onCanceled()
            }

            resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP -> {
                if (data == null) return

                val resultImageUri = UCrop.getOutput(data) ?: return
                onHandleResult.onSuccess(resultImageUri)

            }

            resultCode == UCrop.RESULT_ERROR -> {
                if (data == null) return

                val error = UCrop.getError(data)
                error?.let {
                    onHandleResult.onError(error)
                }
            }
        }
    }

    interface OnHandleResult {
        fun onSuccess(resultImageUri: Uri)
        fun onError(t: Throwable)
        fun onCanceled()
    }
}