import { useState, useEffect, useRef } from 'react';

export default function AiChat({ chat, activeNote }) {
  const { messages, loading, error, model, models, setModel, fetchModels, sendMessage, stopGeneration, clearChat } = chat;
  const [input, setInput] = useState('');
  const [useNoteContext, setUseNoteContext] = useState(true);
  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    fetchModels();
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  function handleSend() {
    const text = input.trim();
    if (!text) return;
    const context = useNoteContext && activeNote ? `${activeNote.title}\n\n${activeNote.content}` : '';
    sendMessage(text, context);
    setInput('');
    inputRef.current?.focus();
  }

  function handleKey(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  return (
    <aside className="ai-chat">
      <div className="ai-chat-header">
        <span className="ai-chat-title">AI Chat</span>
        <div className="ai-chat-controls">
          {models.length > 0 && (
            <select
              className="model-select"
              value={model}
              onChange={e => setModel(e.target.value)}
              title="Select model"
            >
              {models.map(m => <option key={m} value={m}>{m}</option>)}
            </select>
          )}
          <button className="btn-icon btn-small" onClick={clearChat} title="Clear chat">↺</button>
        </div>
      </div>

      {activeNote && (
        <label className="context-toggle">
          <input
            type="checkbox"
            checked={useNoteContext}
            onChange={e => setUseNoteContext(e.target.checked)}
          />
          Use current note as context
        </label>
      )}

      <div className="chat-messages">
        {messages.length === 0 && !error && (
          <div className="chat-empty">
            Ask the AI anything. It can help you summarize, expand, or improve your notes.
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={`chat-msg chat-msg-${msg.role}`}>
            <div className="chat-msg-label">{msg.role === 'user' ? 'You' : 'AI'}</div>
            <div className="chat-msg-content">{msg.content}</div>
          </div>
        ))}
        {loading && messages[messages.length - 1]?.role !== 'assistant' && (
          <div className="chat-msg chat-msg-assistant">
            <div className="chat-msg-label">AI</div>
            <div className="chat-msg-content typing">...</div>
          </div>
        )}
        {error && <div className="chat-error">{error}</div>}
        <div ref={bottomRef} />
      </div>

      <div className="chat-input-area">
        <textarea
          ref={inputRef}
          className="chat-input"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKey}
          placeholder="Ask the AI... (Enter to send)"
          rows={3}
        />
        <div className="chat-input-actions">
          {loading ? (
            <button className="btn-send btn-stop" onClick={stopGeneration}>Stop</button>
          ) : (
            <button className="btn-send" onClick={handleSend} disabled={!input.trim()}>Send</button>
          )}
        </div>
      </div>
    </aside>
  );
}
