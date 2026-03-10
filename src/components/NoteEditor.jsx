import { useEffect, useRef } from 'react';

export default function NoteEditor({ note, onUpdate, saveStatus, onExport }) {
  const titleRef = useRef(null);
  const contentRef = useRef(null);

  useEffect(() => {
    if (titleRef.current && note) titleRef.current.value = note.title;
    if (contentRef.current && note) contentRef.current.value = note.content;
  }, [note?.id]);

  if (!note) {
    return (
      <div className="editor-empty">
        <div className="editor-empty-icon">🐾</div>
        <div className="editor-empty-msg">Select or create a note</div>
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
        placeholder="Begin writing..."
        onChange={e => onUpdate(note.id, { content: e.target.value })}
      />
      <div className="editor-footer">
        <div className={`save-indicator ${saveStatus}`}>
          {saveStatus === 'saved' && <><div className="save-dot" />Saved</>}
          {saveStatus === 'saving' && <>Saving…</>}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ color: 'var(--text3)' }}>{new Date(note.updatedAt).toLocaleString()}</span>
          <button className="btn-export-note" onClick={() => onExport(note)}>Export .txt</button>
        </div>
      </div>
    </div>
  );
}
