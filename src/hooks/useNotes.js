import { useState, useEffect } from 'react';

const STORAGE_KEY = 'notes-app-notes';

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

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(notes));
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

  return { notes, createNote, updateNote, deleteNote };
}
