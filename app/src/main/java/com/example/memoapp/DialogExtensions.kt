package com.example.memoapp

import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog

fun AlertDialog.makeFullScreen() {
    setOnShowListener {
        window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.white)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }
}
