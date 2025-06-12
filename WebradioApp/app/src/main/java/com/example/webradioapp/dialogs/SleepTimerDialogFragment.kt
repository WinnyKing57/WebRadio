package com.example.webradioapp.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class SleepTimerDialogFragment : DialogFragment() {

    interface SleepTimerDialogListener {
        fun onTimerSet(minutes: Int)
    }

    private var listener: SleepTimerDialogListener? = null

    fun setSleepTimerDialogListener(listener: SleepTimerDialogListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = arrayOf("15 minutes", "30 minutes", "45 minutes", "60 minutes", "Cancel")
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Set Sleep Timer")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> listener?.onTimerSet(15)
                    1 -> listener?.onTimerSet(30)
                    2 -> listener?.onTimerSet(45)
                    3 -> listener?.onTimerSet(60)
                    4 -> dismiss() // Cancel
                }
            }
        return builder.create()
    }

    companion object {
        const val TAG = "SleepTimerDialogFragment"
    }
}
