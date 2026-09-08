# SIMPLE Update 06 — Professional 3000K UI + Web starter

## Android
This update applies the requested palettes directly to the Android app.

Light mode:
- Background #FAF6E8
- Soft containers #EADA70
- Primary accent #E59F00
- Main text #2B261F

Dark mode:
- Background #1A1816
- Surfaces #2B2622
- Main interaction/text #FCE49E

Dynamic Material colors are disabled so Android will not replace the SIMPLE palette.
The app follows the phone's light/dark mode automatically.

Functional flow represented:
1. Login / create account
2. Home / credits
3. Select temporary PDF
4. AI Workspace:
   - Editable summary
   - Explain topic
   - PowerPoint
   - Compare concepts
   - Key points
   - Free chat request
5. Saved summaries/materials are persistent with limits
6. Study / Exam is only from saved summaries/materials, not the raw book

## Web
A responsive Next.js starter exists in /web.
It already follows the same light/dark palettes using prefers-color-scheme.
It contains the administrator dashboard structure for:
- users
- credits
- materials
- reports
- support
- settings

This same web base can later provide the browser experience for iPhone/iPad users.

## Upload to GitHub
Upload/replace the files from this ZIP.

Then in Android Studio:
git pull origin main

If Git says a tracked file is locally modified:
git restore <exact-file-path>
git pull origin main

Then:
Build -> Clean Project
Build -> Assemble Project
Run

## Web local test later
cd web
npm install
npm run dev
