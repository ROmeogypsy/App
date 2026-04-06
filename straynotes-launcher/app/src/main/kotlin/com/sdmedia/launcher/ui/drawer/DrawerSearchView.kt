package com.sdmedia.launcher.ui.drawer

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.sdmedia.launcher.databinding.ViewDrawerSearchBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

class DrawerSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = ViewDrawerSearchBinding.inflate(LayoutInflater.from(context), this, true)

    /** Flow of search queries, debounced 80ms for single-keystroke latency. */
    val queryFlow: Flow<String> = callbackFlow {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                trySend(s?.toString() ?: "")
            }
        }
        binding.etSearch.addTextChangedListener(watcher)
        awaitClose { binding.etSearch.removeTextChangedListener(watcher) }
    }.debounce(80)

    fun clear() {
        binding.etSearch.text?.clear()
    }

    fun requestSearchFocus() {
        binding.etSearch.requestFocus()
    }
}
