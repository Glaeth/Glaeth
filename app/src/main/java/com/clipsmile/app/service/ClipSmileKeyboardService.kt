package com.clipsmile.app.service

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.getSystemService
import com.clipsmile.app.R

/**
 * A normal keyboard. Nothing typed here is saved or sent.
 *
 * Android only lets a background app read the clipboard if that app is the
 * selected keyboard. Selecting this keyboard is what allows a copy made in
 * another app to reach WhatsApp. Keystrokes themselves are only inserted
 * into the field the user is typing in.
 */
class ClipSmileKeyboardService : InputMethodService() {

    private var shifted = false
    private var symbols = false

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1B1030.toInt())
            setPadding(8, 12, 8, 18)
        }
        rows().forEach { labels ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            labels.forEach { label ->
                row.addView(keyButton(label), keyLayoutParams(label))
            }
            root.addView(row)
        }
        return root
    }

    private fun rows(): List<List<String>> {
        if (symbols) {
            return listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "₺", "_", "&", "-", "+", "(", ")", "/"),
                listOf("ABC", ".", ",", "?", "!", "'", "\"", ":", "DEL"),
                listOf("NEXT", "SPACE", "ENTER")
            )
        }
        val letter: (String) -> String = { if (shifted) it.uppercase() else it }
        return listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "ı", "o", "p", "ğ", "ü").map(letter),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ş", "i").map(letter),
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "ö", "ç", "DEL").map {
                if (it == "SHIFT" || it == "DEL") it else letter(it)
            },
            listOf("123", "SPACE", "NEXT", "ENTER")
        )
    }

    private fun keyButton(label: String): Button {
        return Button(this).apply {
            text = when (label) {
                "SPACE" -> getString(R.string.keyboard_space)
                "DEL" -> "⌫"
                "ENTER" -> "⏎"
                "NEXT" -> "🌐"
                "SHIFT" -> "⇧"
                else -> label
            }
            isAllCaps = false
            transformationMethod = null
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF3A2A55.toInt())
            textSize = 14f
            setOnClickListener { onKey(label) }
        }
    }

    private fun keyLayoutParams(label: String): LinearLayout.LayoutParams {
        val weight = when (label) {
            "SPACE" -> 4f
            "SHIFT", "DEL", "ENTER", "NEXT", "123", "ABC" -> 1.4f
            else -> 1f
        }
        return LinearLayout.LayoutParams(0, dp(48), weight).apply {
            setMargins(3, 3, 3, 3)
        }
    }

    private fun onKey(label: String) {
        val input = currentInputConnection ?: return
        when (label) {
            "SHIFT" -> {
                shifted = !shifted
                setInputView(onCreateInputView())
            }
            "123" -> {
                symbols = true
                setInputView(onCreateInputView())
            }
            "ABC" -> {
                symbols = false
                setInputView(onCreateInputView())
            }
            "DEL" -> input.deleteSurroundingText(1, 0)
            "SPACE" -> input.commitText(" ", 1)
            "ENTER" -> {
                input.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED)
                input.commitText("\n", 1)
            }
            "NEXT" -> switchInputMethod()
            else -> {
                input.commitText(label, 1)
                if (shifted) {
                    shifted = false
                    setInputView(onCreateInputView())
                }
            }
        }
    }

    private fun switchInputMethod() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToNextInputMethod(false)
            return
        }
        getSystemService<InputMethodManager>()?.showInputMethodPicker()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
