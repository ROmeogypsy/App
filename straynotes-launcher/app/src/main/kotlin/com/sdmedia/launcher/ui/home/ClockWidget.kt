package com.sdmedia.launcher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.sdmedia.launcher.databinding.ViewClockBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = ViewClockBinding.inflate(LayoutInflater.from(context), this, true)

    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    private val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTime()
        }
    }

    init {
        orientation = VERTICAL
        updateTime()
    }

    private fun updateTime() {
        val now = Date()
        binding.tvTime.text = timeFormat.format(now)
        binding.tvAmPm.text = amPmFormat.format(now)
        binding.tvDate.text = dateFormat.format(now)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter(Intent.ACTION_TIME_TICK).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(tickReceiver, filter)
        updateTime()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(tickReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered
        }
    }
}
