package com.stacktivity.yandeximagesearchengine.ui.captcha

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.util.ImageDownloadHelper
import kotlinx.android.synthetic.main.captcha_dialog.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CaptchaDialog(
    private val imageUrl: String,
    private val showFailedMsg: Boolean,
    private val onEnterCaptcha: (captchaValue: String?) -> Unit
) : DialogFragment() {

    companion object {
        val tag = CaptchaDialog::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.captcha_dialog, container, false)
    }

    private fun applyCaptchaImage(imageUrl: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val bitmap = ImageDownloadHelper.getBitmapAsync(imageUrl)!!
            val cropFactor = image_captcha.width.toFloat() / bitmap.width
            val reqHeight = (cropFactor * bitmap.height).toInt()
            image_captcha.layoutParams.height = reqHeight
            image_captcha.setImageBitmap(bitmap)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (showFailedMsg) failed_msg.visibility = View.VISIBLE

        applyCaptchaImage(imageUrl)

        btn_submit.setOnClickListener {
            onEnterCaptcha(input_field.text.toString())
            dismiss()
        }

        input_field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btn_submit.performClick()
            }
            false
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onEnterCaptcha(null)
    }
}