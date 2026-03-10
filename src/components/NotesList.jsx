export default function NotesList({ notes, activeId, onSelect, onCreate, onDelete }) {
  function formatDate(ts) {
    return new Date(ts).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }

  return (
    <aside className="notes-list">
      <div className="notes-list-header">
        <span className="notes-list-title">Scrolls</span>
        <button className="btn-new" onClick={onCreate} title="New note">+</button>
      </div>
      <div className="notes-list-items">
        {notes.length === 0 && (
          <div className="notes-empty">No scrolls yet.<br />Create your first one.</div>
        )}
        {notes.map(note => (
          <div
            key={note.id}
            className={`note-item ${note.id === activeId ? 'active' : ''}`}
            onClick={() => onSelect(note.id)}
          >
            <div className="note-item-title">{note.title || 'Untitled'}</div>
            <div className="note-item-meta">
              <span>{formatDate(note.updatedAt)}</span>
              <button
                className="btn-delete"
                onClick={e => { e.stopPropagation(); onDelete(note.id); }}
                title="Delete"
              >×</button>
            </div>
          </div>
        ))}
      </div>
    </aside>
  );
}
