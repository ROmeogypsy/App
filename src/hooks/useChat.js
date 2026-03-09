import { useState, useRef } from 'react';

const OLLAMA_BASE = 'http://localhost:11434';

export function useChat() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [model, setModel] = useState('');
  const [models, setModels] = useState([]);
  const [error, setError] = useState('');
  const abortRef = useRef(null);

  async function fetchModels() {
    try {
      const res = await fetch(`${OLLAMA_BASE}/api/tags`);
      if (!res.ok) throw new Error('Ollama not reachable');
      const data = await res.json();
      const list = (data.models || []).map(m => m.name);
      setModels(list);
      if (list.length > 0 && !model) setModel(list[0]);
      setError('');
      return list;
    } catch {
      setError('Cannot connect to Ollama. Make sure it is running on port 11434.');
      return [];
    }
  }

  async function sendMessage(userText, noteContext = '') {
    if (!userText.trim() || loading) return;

    const userMsg = { role: 'user', content: userText };
    setMessages(prev => [...prev, userMsg]);
    setLoading(true);
    setError('');

    const systemPrompt = noteContext
      ? `You are a helpful AI assistant integrated into a notes app. The user is currently viewing this note:\n\n---\n${noteContext}\n---\n\nHelp the user with their note or answer their questions.`
      : 'You are a helpful AI assistant integrated into a notes app. Help the user with their notes and questions.';

    const history = [...messages, userMsg].map(m => ({
      role: m.role,
      content: m.content,
    }));

    try {
      abortRef.current = new AbortController();
      const res = await fetch(`${OLLAMA_BASE}/api/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        signal: abortRef.current.signal,
        body: JSON.stringify({
          model: model || 'llama3',
          messages: [{ role: 'system', content: systemPrompt }, ...history],
          stream: true,
        }),
      });

      if (!res.ok) throw new Error(`Ollama error: ${res.status}`);

      const assistantMsg = { role: 'assistant', content: '' };
      setMessages(prev => [...prev, assistantMsg]);

      const reader = res.body.getReader();
      const decoder = new TextDecoder();

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value);
        const lines = chunk.split('\n').filter(Boolean);
        for (const line of lines) {
          try {
            const json = JSON.parse(line);
            if (json.message?.content) {
              setMessages(prev => {
                const updated = [...prev];
                updated[updated.length - 1] = {
                  ...updated[updated.length - 1],
                  content: updated[updated.length - 1].content + json.message.content,
                };
                return updated;
              });
            }
          } catch {
            // skip malformed chunks
          }
        }
      }
    } catch (err) {
      if (err.name !== 'AbortError') {
        setError(err.message || 'Failed to get response from Ollama.');
        setMessages(prev => prev.slice(0, -1)); // remove empty assistant msg
      }
    } finally {
      setLoading(false);
      abortRef.current = null;
    }
  }

  function stopGeneration() {
    abortRef.current?.abort();
  }

  function clearChat() {
    setMessages([]);
    setError('');
  }

  return { messages, loading, error, model, models, setModel, fetchModels, sendMessage, stopGeneration, clearChat };
}
