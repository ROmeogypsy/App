import { useState, useEffect, useRef, useCallback } from 'react';

const STORAGE_KEY = 'stray-notes-v1';

function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2);
}

export function useNotes() {
  const [notes, setNotes] = useState(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  });
  const [saveStatus, setSaveStatus] = useState('saved'); // 'saving' | 'saved'
  const saveTimer = useRef(null);

  // Debounced persist to localStorage with status indicator
  useEffect(() => {
    setSaveStatus('saving');
    clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(() => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
        setSaveStatus('saved');
      } catch (e) {
        console.error('Failed to save notes:', e);
        setSaveStatus('error');
      }
    }, 400);
    return () => clearTimeout(saveTimer.current);
  }, [notes]);

  function createNote() {
    const note = {
      id: generateId(),
      title: 'Untitled Note',
      content: '',
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    setNotes(prev => [note, ...prev]);
    return note;
  }

  function updateNote(id, changes) {
    setNotes(prev =>
      prev.map(n => n.id === id ? { ...n, ...changes, updatedAt: Date.now() } : n)
    );
  }

  function deleteNote(id) {
    setNotes(prev => prev.filter(n => n.id !== id));
  }

  // Export all notes as a JSON backup file
  const exportNotes = useCallback(() => {
    const data = { version: 1, exportedAt: new Date().toISOString(), notes };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `stray-notes-backup-${new Date().toISOString().slice(0,10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }, [notes]);

  // Import notes from a JSON backup file (merges, no duplicates by id)
  function importNotes(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = e => {
        try {
          const data = JSON.parse(e.target.result);
          const incoming = Array.isArray(data) ? data : (data.notes || []);
          if (!Array.isArray(incoming)) throw new Error('Invalid format');
          setNotes(prev => {
            const existingIds = new Set(prev.map(n => n.id));
            const newNotes = incoming.filter(n => n.id && !existingIds.has(n.id));
            return [...newNotes, ...prev];
          });
          resolve(incoming.length);
        } catch {
          reject(new Error('Invalid backup file'));
        }
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsText(file);
    });
  }

  // Export a single note as plain text
  function exportNote(note) {
    const text = `${note.title}\n${'─'.repeat(note.title.length || 10)}\n\n${note.content}`;
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${note.title.replace(/[^a-z0-9]/gi, '_') || 'note'}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return { notes, saveStatus, createNote, updateNote, deleteNote, exportNotes, importNotes, exportNote };
}
