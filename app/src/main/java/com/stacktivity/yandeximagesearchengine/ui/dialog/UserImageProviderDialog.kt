package com.stacktivity.yandeximagesearchengine.ui.dialog

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stacktivity.yandeximagesearchengine.R
import com.stacktivity.yandeximagesearchengine.util.CacheWorker
import com.stacktivity.yandeximagesearchengine.util.PickImage
import com.stacktivity.yandeximagesearchengine.util.TakePictureToPrivateFile
import com.stacktivity.yandeximagesearchengine.util.ThemeUtils
import kotlinx.android.synthetic.main.user_image_provider_menu.*


class UserImageProviderDialog: BottomSheetDialogFragment() {
    private val tempFile = CacheWorker.getTempFile()

    private val photographer = registerForActivityResult(TakePictureToPrivateFile()) {
        if (it) setResult(ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY))
    }

    private val imagePicker = registerForActivityResult(PickImage()) { selectedImage ->
        if (selectedImage != null) {
            val pfd = requireActivity().contentResolver.openFileDescriptor(selectedImage, "r")
            setResult(pfd)
        }
    }

    private fun setResult(pfd: ParcelFileDescriptor?) {
        val bundle = Bundle(1)
        bundle.putParcelable("pfd", pfd)
        parentFragmentManager.setFragmentResult(tag!!, bundle)
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val contextThemeWrapper = ThemeUtils.getContextThemeWrapper(context)

        return layoutInflater.cloneInContext(contextThemeWrapper)
            .inflate(R.layout.user_image_provider_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyFixForLandscapeOrientation(view)

        btn_image_capture?.setOnClickListener {
            photographer.launch(tempFile)
        }
        btn_pick_image?.setOnClickListener {
            imagePicker.launch(null)
        }
    }

    override fun onPause() {
        super.onPause()
        enableButtons(false)
    }

    override fun onResume() {
        super.onResume()
        enableButtons(true)
    }

    private fun enableButtons(enable: Boolean) {
        btn_image_capture.isEnabled = enable
        btn_pick_image.isEnabled = enable
    }

    private fun applyFixForLandscapeOrientation(view: View) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val bottomSheet = dialog!!.findViewById<FrameLayout>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

                BottomSheetBehavior.from(bottomSheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    peekHeight = 0
                }
            }
        })
    }
}