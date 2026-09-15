package com.keyora.keyboard.enablement

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.keyora.keyboard.ime.KeyoraInputMethodService

object ImeEnablement {

    fun isEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val id = ComponentName(context, KeyoraInputMethodService::class.java).flattenToString()
        return imm.enabledInputMethodList.any { it.id == id || it.packageName == context.packageName }
    }

    fun isSelected(context: Context): Boolean {
        val current = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ).orEmpty()
        val id = ComponentName(context, KeyoraInputMethodService::class.java).flattenToString()
        return current.equals(id, ignoreCase = true) ||
            current.startsWith("${context.packageName}/", ignoreCase = true)
    }

    fun openInputMethodSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun showInputMethodPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}
