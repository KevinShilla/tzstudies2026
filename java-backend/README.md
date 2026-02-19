# TZ Studies - Java Backend (Plain Java 8)

This folder is **only** for your interview so you can show a Java backend alongside your existing Python app.

It runs a tiny HTTP API using **Java 8 only** (no Maven, no Spring). That means it will work on your laptop right now.

## Endpoints
- `GET /api/health` -> `{ "status": "ok" }`
- `GET /api/exams` -> a small JSON list (demo data)

## Run (Windows PowerShell)
From the repo root:

```powershell
cd java-backend
.\run.ps1
```

Then open:
- http://localhost:8080/api/health
- http://localhost:8080/api/exams

## Run (Mac/Linux)
```bash
cd java-backend
chmod +x run.sh
./run.sh
```

## Notes
- This does **not** replace your Flask app.
- This is a clean, separate Java service you can explain in the interview.
