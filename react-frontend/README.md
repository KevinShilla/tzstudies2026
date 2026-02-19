# TZ Studies - React Frontend

This folder is **added** on top of your existing TZ Studies Flask app.
It does **not** modify or break the Flask project.

## What it does
- Simple React UI that lists exam PDFs and answer key PDFs
- It calls the **Java Spring Boot API** in `../java-backend`

## Run (local)
Requirements:
- Node.js 18+ (or 20+)

From the repo root:
```bash
cd react-frontend
npm install
npm run dev
```

By default it calls:
- `http://localhost:8080/api`

If you want a different API URL, create `react-frontend/.env`:
```bash
VITE_API_BASE_URL=http://localhost:8080/api
```
