import { useState, useRef } from 'react';
import { useNotes } from './hooks/useNotes';
import { useChat } from './hooks/useChat';
import NotesList from './components/NotesList';
import NoteEditor from './components/NoteEditor';
import AiChat from './components/AiChat';
import './App.css';

export default function App() {
  const { notes, saveStatus, createNote, updateNote, deleteNote, exportNotes, importNotes, exportNote } = useNotes();
  const chat = useChat();
  const [activeId, setActiveId] = useState(null);
  const [showChat, setShowChat] = useState(true);
  const [importMsg, setImportMsg] = useState('');
  const importRef = useRef(null);

  const activeNote = notes.find(n => n.id === activeId) || null;

  function handleCreate() {
    const note = createNote();
    setActiveId(note.id);
  }

  function handleDelete(id) {
    deleteNote(id);
    if (activeId === id) setActiveId(notes.find(n => n.id !== id)?.id || null);
  }

  async function handleImport(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const count = await importNotes(file);
      setImportMsg(`↑ ${count} note${count !== 1 ? 's' : ''} imported`);
      setTimeout(() => setImportMsg(''), 3000);
    } catch (err) {
      setImportMsg(`✕ ${err.message}`);
      setTimeout(() => setImportMsg(''), 3000);
    }
    e.target.value = '';
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-logo">
          <span className="paw">🐾</span>
          Stray Notes
        </div>
        <div className="header-actions">
          {importMsg && <span style={{ fontSize: '.75rem', color: 'var(--amber2)' }}>{importMsg}</span>}
          <input ref={importRef} type="file" accept=".json" className="import-input" onChange={handleImport} />
          <button className="btn-header" onClick={() => importRef.current.click()} title="Import backup">
            ↑ Import
          </button>
          <button className="btn-header" onClick={exportNotes} title="Export all notes">
            ↓ Export
          </button>
          <button
            className={`btn-header ${showChat ? 'active' : ''}`}
            onClick={() => setShowChat(v => !v)}
            title="Toggle AI Chat"
          >
            ⚡ AI Chat
          </button>
        </div>
      </header>

      <div className="app-body">
        <NotesList
          notes={notes}
          activeId={activeId}
          onSelect={setActiveId}
          onCreate={handleCreate}
          onDelete={handleDelete}
        />

        <main className="app-main">
          <NoteEditor
            note={activeNote}
            onUpdate={updateNote}
            saveStatus={saveStatus}
            onExport={exportNote}
          />
        </main>

        {showChat && (
          <AiChat chat={chat} activeNote={activeNote} />
        )}
      </div>
    </div>
  );
}
