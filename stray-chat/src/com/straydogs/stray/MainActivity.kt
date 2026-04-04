package com.straydogs.stray

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.ListView
import android.widget.TextView

class MainActivity : Activity() {

    // ── Views ───────────────────────────────────────────────────────────────
    private lateinit var listView: ListView
    private lateinit var inputField: EditText
    private lateinit var sendBtn: TextView
    private lateinit var backendChip: TextView
    private lateinit var statusBar: TextView
    private lateinit var adapter: ChatAdapter

    // ── State ────────────────────────────────────────────────────────────────
    private val backends: List<LlmBackend> by lazy {
        listOf(
            MockLlmBackend(),
            LocalLlmBackend(filesDir),
            HuggingFaceBackend()
        )
    }
    private var activeBackend: LlmBackend? = null
    private var isGenerating = false
    private var streamBuffer = StringBuilder()
    private val mainHandler = Handler(Looper.getMainLooper())

    // ── dp helper ───────────────────────────────────────────────────────────
    private val dp get() = resources.displayMetrics.density
    private fun px(n: Int) = (n * dp + 0.5f).toInt()

    // ────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activeBackend = backends.firstOrNull { it.isReady } ?: backends.first()

        val root = buildLayout()
        setContentView(root)

        postWelcome()
    }

    // ── Layout ───────────────────────────────────────────────────────────────
    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A0A"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        root.addView(buildHeader())
        root.addView(buildMessageList(), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(buildDivider())
        root.addView(buildInputBar())

        return root
    }

    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F0F0F"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16), px(14), px(16), px(8))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val logo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleText = TextView(this).apply {
            text = "STRAY"
            setTextColor(Color.parseColor("#CC0000"))
            textSize = 22f
            letterSpacing = 0.2f
            setPadding(0, 0, px(8), 0)
        }

        val byLine = TextView(this).apply {
            text = "/ SD MEDIA"
            setTextColor(Color.parseColor("#444444"))
            textSize = 11f
            letterSpacing = 0.1f
        }

        logo.addView(titleText)
        logo.addView(byLine)

        backendChip = TextView(this).apply {
            text = activeBackend?.displayName ?: "—"
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 12f
            setPadding(px(12), px(5), px(12), px(5))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A"))
                setStroke(1, Color.parseColor("#333333"))
                cornerRadius = px(12).toFloat()
            }
            setOnClickListener { showBackendPicker() }
        }

        topRow.addView(logo)
        topRow.addView(backendChip)

        statusBar = TextView(this).apply {
            text = activeBackend?.statusLine() ?: ""
            setTextColor(Color.parseColor("#555555"))
            textSize = 10f
            setPadding(px(16), 0, px(16), px(8))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        header.addView(topRow)
        header.addView(statusBar)
        header.addView(buildDivider())

        return header
    }

    private fun buildMessageList(): ListView {
        adapter = ChatAdapter(this)
        listView = ListView(this).apply {
            setAdapter(adapter)
            divider = null
            dividerHeight = 0
            setBackgroundColor(Color.TRANSPARENT)
            transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
            clipToPadding = false
            setPadding(0, px(8), 0, px(8))
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        return listView
    }

    private fun buildDivider() = View(this).apply {
        setBackgroundColor(Color.parseColor("#1E1E1E"))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 1)
    }

    private fun buildInputBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setBackgroundColor(Color.parseColor("#0F0F0F"))
            setPadding(px(10), px(10), px(10), px(10))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        inputField = EditText(this).apply {
            hint = "Message…"
            setHintTextColor(Color.parseColor("#444444"))
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 15f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A"))
                setStroke(1, Color.parseColor("#2A2A2A"))
                cornerRadius = px(20).toFloat()
            }
            setPadding(px(16), px(11), px(16), px(11))
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            maxLines = 5
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = px(10)
            }
        }

        sendBtn = TextView(this).apply {
            text = "▶"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val sz = px(44)
            layoutParams = LayoutParams(sz, sz)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC0000"))
                cornerRadius = px(22).toFloat()
            }
            setOnClickListener { onSendOrStop() }
        }

        bar.addView(inputField)
        bar.addView(sendBtn)
        return bar
    }

    // ── Send / Stop ──────────────────────────────────────────────────────────
    private fun onSendOrStop() {
        if (isGenerating) {
            stopGeneration()
            return
        }

        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return

        inputField.text.clear()
        hideKeyboard()

        adapter.addMessage(Message(role = Role.USER, content = text))
        startGeneration()
    }

    private fun startGeneration() {
        val backend = activeBackend ?: return

        // Add empty streaming placeholder
        adapter.addMessage(Message(role = Role.ASSISTANT, content = "", isStreaming = true))
        streamBuffer.clear()
        isGenerating = true
        sendBtn.text = "■"
        sendBtn.background = GradientDrawable().apply {
            setColor(Color.parseColor("#882222"))
            cornerRadius = px(22).toFloat()
        }

        backend.generate(
            messages = adapter.getMessages(),
            onToken = { token ->
                streamBuffer.append(token)
                mainHandler.post {
                    adapter.updateLast(streamBuffer.toString(), true)
                    listView.setSelection(adapter.count - 1)
                }
            },
            onDone = {
                mainHandler.post { finishGeneration(cancelled = false) }
            },
            onError = { err ->
                mainHandler.post {
                    streamBuffer.setLength(0)
                    streamBuffer.append("⚠ $err")
                    finishGeneration(cancelled = false)
                }
            }
        )
    }

    private fun stopGeneration() {
        activeBackend?.cancel()
        finishGeneration(cancelled = true)
    }

    private fun finishGeneration(cancelled: Boolean) {
        isGenerating = false
        sendBtn.text = "▶"
        sendBtn.background = GradientDrawable().apply {
            setColor(Color.parseColor("#CC0000"))
            cornerRadius = px(22).toFloat()
        }
        val finalText = streamBuffer.toString().ifEmpty {
            if (cancelled) "[cancelled]" else "[no response]"
        }
        adapter.updateLast(finalText, false)
        listView.setSelection(adapter.count - 1)
    }

    // ── Backend picker ───────────────────────────────────────────────────────
    private fun showBackendPicker() {
        val labels = backends.map { b ->
            val tag = if (!b.isReady) " (unavailable)" else ""
            val check = if (b == activeBackend) " ✓" else ""
            "${b.displayName}$tag$check"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Inference Backend")
            .setItems(labels) { _, which ->
                val chosen = backends[which]
                activeBackend = chosen
                backendChip.text = chosen.displayName
                statusBar.text = chosen.statusLine()
                adapter.addMessage(
                    Message(
                        role = Role.ASSISTANT,
                        content = "Switched to ${chosen.displayName}. ${chosen.statusLine()}"
                    )
                )
                listView.setSelection(adapter.count - 1)
            }
            .show()
    }

    // ── Welcome ──────────────────────────────────────────────────────────────
    private fun postWelcome() {
        val backend = activeBackend ?: return
        adapter.addMessage(
            Message(
                role = Role.ASSISTANT,
                content = "STRAY online — ${backend.displayName}\n${backend.statusLine()}\n\nType a message to begin. Tap the backend chip to switch inference modes."
            )
        )
    }

    // ── Keyboard ─────────────────────────────────────────────────────────────
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(inputField.windowToken, 0)
    }

    override fun onBackPressed() {
        if (isGenerating) stopGeneration()
        else super.onBackPressed()
    }
}
