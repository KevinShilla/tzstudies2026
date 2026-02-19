import React, { useEffect, useMemo, useState } from 'react'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function Section({ title, items, onDownload, emptyText }) {
  return (
    <div style={{ border: '1px solid #ddd', borderRadius: 12, padding: 16 }}>
      <h2 style={{ marginTop: 0 }}>{title}</h2>
      {items.length === 0 ? (
        <p style={{ margin: 0, opacity: 0.7 }}>{emptyText}</p>
      ) : (
        <ul style={{ margin: 0, paddingLeft: 18 }}>
          {items.map((name) => (
            <li key={name} style={{ marginBottom: 8 }}>
              <button
                onClick={() => onDownload(name)}
                style={{
                  cursor: 'pointer',
                  padding: '6px 10px',
                  borderRadius: 10,
                  border: '1px solid #ccc',
                  background: 'white'
                }}
              >
                Download
              </button>
              <span style={{ marginLeft: 10 }}>{name}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default function App() {
  const [exams, setExams] = useState([])
  const [keys, setKeys] = useState([])
  const [status, setStatus] = useState('Loading...')

  const downloadUrl = useMemo(() => {
    return (type, filename) => `${API_BASE}/file/${type}/${encodeURIComponent(filename)}`
  }, [])

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const health = await fetch(`${API_BASE}/health`)
        if (!health.ok) throw new Error('API not reachable')

        const [examsRes, keysRes] = await Promise.all([
          fetch(`${API_BASE}/exams`),
          fetch(`${API_BASE}/answer-keys`)
        ])

        const examsJson = examsRes.ok ? await examsRes.json() : []
        const keysJson = keysRes.ok ? await keysRes.json() : []

        if (!cancelled) {
          setExams(Array.isArray(examsJson) ? examsJson : [])
          setKeys(Array.isArray(keysJson) ? keysJson : [])
          setStatus('Connected')
        }
      } catch (e) {
        if (!cancelled) {
          setStatus('Not connected. Start the Java API first (java-backend).')
          setExams([])
          setKeys([])
        }
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  function handleDownload(type) {
    return (filename) => {
      window.open(downloadUrl(type, filename), '_blank')
    }
  }

  return (
    <div style={{ fontFamily: 'system-ui, -apple-system, Segoe UI, Roboto, Arial', padding: 24, maxWidth: 980, margin: '0 auto' }}>
      <h1 style={{ marginTop: 0 }}>TZ Studies (React)</h1>
      <p style={{ marginTop: 6, opacity: 0.8 }}>
        This React UI talks to a Java API (running locally on port 8080).
      </p>

      <div style={{ marginTop: 12, padding: 12, borderRadius: 12, background: '#f6f6f6' }}>
        <div><strong>API:</strong> {API_BASE}</div>
        <div><strong>Status:</strong> {status}</div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 18 }}>
        <Section
          title="Exams"
          items={exams}
          onDownload={handleDownload('exams')}
          emptyText="No exam PDFs found (or API not connected)."
        />
        <Section
          title="Answer Keys"
          items={keys}
          onDownload={handleDownload('answer-keys')}
          emptyText="No answer key PDFs found (or API not connected)."
        />
      </div>

      <div style={{ marginTop: 18, borderTop: '1px solid #eee', paddingTop: 14, opacity: 0.9 }}>
        <h3 style={{ marginTop: 0 }}>How to run</h3>
        <ol style={{ marginTop: 6 }}>
          <li>Start Java API: <code>cd java-backend</code> then <code>.\run.ps1</code> (PowerShell)</li>
          <li>Start React: <code>cd react-frontend</code> then <code>npm install</code> then <code>npm run dev</code></li>
          <li>Open: <code>http://localhost:5173</code></li>
        </ol>
      </div>
    </div>
  )
}
