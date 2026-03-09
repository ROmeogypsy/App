import { useEffect, useRef } from 'react';

export default function NoteEditor({ note, onUpdate }) {
  const titleRef = useRef(null);
  const contentRef = useRef(null);

  useEffect(() => {
    if (titleRef.current && note) {
      titleRef.current.value = note.title;
    }
    if (contentRef.current && note) {
      contentRef.current.value = note.content;
    }
  }, [note?.id]);

  if (!note) {
    return (
      <div className="editor-empty">
        <div className="editor-empty-msg">Select or create a note to get started</div>
      </div>
    );
  }

  return (
    <div className="editor">
      <input
        ref={titleRef}
        className="editor-title"
        defaultValue={note.title}
        placeholder="Note title"
        onChange={e => onUpdate(note.id, { title: e.target.value })}
      />
      <textarea
        ref={contentRef}
        className="editor-content"
        defaultValue={note.content}
        placeholder="Start writing your note..."
        onChange={e => onUpdate(note.id, { content: e.target.value })}
      />
      <div className="editor-footer">
        {new Date(note.updatedAt).toLocaleString()}
      </div>
    </div>
  );
}
