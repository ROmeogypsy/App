import { useState } from 'react';
import { useNotes } from './hooks/useNotes';
import { useChat } from './hooks/useChat';
import NotesList from './components/NotesList';
import NoteEditor from './components/NoteEditor';
import AiChat from './components/AiChat';
import './App.css';

export default function App() {
  const { notes, createNote, updateNote, deleteNote } = useNotes();
  const chat = useChat();
  const [activeId, setActiveId] = useState(null);
  const [showChat, setShowChat] = useState(true);

  const activeNote = notes.find(n => n.id === activeId) || null;

  function handleCreate() {
    const note = createNote();
    setActiveId(note.id);
  }

  function handleDelete(id) {
    deleteNote(id);
    if (activeId === id) setActiveId(notes.find(n => n.id !== id)?.id || null);
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-logo">📝 NotesAI</div>
        <button
          className={`btn-toggle-chat ${showChat ? 'active' : ''}`}
          onClick={() => setShowChat(v => !v)}
          title="Toggle AI Chat"
        >
          🤖 AI Chat
        </button>
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
          <NoteEditor note={activeNote} onUpdate={updateNote} />
        </main>

        {showChat && (
          <AiChat chat={chat} activeNote={activeNote} />
        )}
      </div>
    </div>
  );
}
