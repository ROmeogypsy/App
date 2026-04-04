package com.straydogs.stray

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView

class ChatAdapter(private val context: Context) : BaseAdapter() {

    private val items = mutableListOf<Message>()
    private val dp = context.resources.displayMetrics.density
    private val screenW = context.resources.displayMetrics.widthPixels

    // --- Data API -----------------------------------------------------------

    fun addMessage(msg: Message) {
        items.add(msg)
        notifyDataSetChanged()
    }

    fun updateLast(content: String, streaming: Boolean) {
        if (items.isNotEmpty()) {
            val last = items.last()
            last.content = content
            last.isStreaming = streaming
            notifyDataSetChanged()
        }
    }

    fun getMessages(): List<Message> = items.toList()

    // --- Adapter overrides --------------------------------------------------

    override fun getCount() = items.size
    override fun getItem(pos: Int) = items[pos]
    override fun getItemId(pos: Int) = items[pos].id
    override fun areAllItemsEnabled() = false
    override fun isEnabled(pos: Int) = false

    override fun getViewTypeCount() = 2
    override fun getItemViewType(pos: Int) = if (items[pos].role == Role.USER) 0 else 1

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val msg = items[pos]
        val isUser = msg.role == Role.USER

        // Outer row — full width, gravity pushes bubble to correct side
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val hPad = px(16)
            val vPad = px(4)
            setPadding(hPad, vPad, hPad, vPad)
        }

        // Avatar dot for assistant
        if (!isUser) {
            val dot = View(context).apply {
                background = circle(Color.parseColor("#CC0000"))
                val size = px(8)
                layoutParams = LayoutParams(size, size).apply {
                    gravity = Gravity.TOP
                    topMargin = px(10)
                    marginEnd = px(8)
                }
            }
            row.addView(dot)
        }

        // Bubble
        val displayText = buildDisplayText(msg)
        val bubble = TextView(context).apply {
            text = displayText
            textSize = 15f
            setLineSpacing(px(2).toFloat(), 1f)
            val maxBubbleW = (screenW * 0.75f).toInt()
            maxWidth = maxBubbleW
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

            if (isUser) {
                setTextColor(Color.WHITE)
                background = bubbleShape(
                    color = Color.parseColor("#CC0000"),
                    tl = px(18).toFloat(), tr = px(4).toFloat(),
                    br = px(18).toFloat(), bl = px(18).toFloat()
                )
            } else {
                setTextColor(Color.parseColor("#DDDDDD"))
                background = bubbleShape(
                    color = Color.parseColor("#1C1C1C"),
                    tl = px(4).toFloat(), tr = px(18).toFloat(),
                    br = px(18).toFloat(), bl = px(18).toFloat()
                )
            }
            val hp = px(14); val vp = px(9)
            setPadding(hp, vp, hp, vp)
        }

        row.addView(bubble)
        return row
    }

    // --- Helpers ------------------------------------------------------------

    private fun buildDisplayText(msg: Message): String {
        return when {
            msg.isStreaming && msg.content.isEmpty() -> "▋"
            msg.isStreaming -> msg.content + "▋"
            else -> msg.content
        }
    }

    private fun px(dp: Int) = (dp * this.dp + 0.5f).toInt()

    private fun bubbleShape(color: Int, tl: Float, tr: Float, br: Float, bl: Float) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
        }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }
}
