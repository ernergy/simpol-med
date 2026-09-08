# SIMPLE Update 02

Replace/upload these files over Update 01.

Adds:
- temporary DEVELOPMENT MODE after account creation so UI work is not blocked by email confirmation
- clean authentication errors (no headers/URLs shown)
- real study_sessions + materials registration when the user has a valid Supabase session
- development mode intentionally does not write protected user data
- prepares the app for temporary PDF upload and OpenAI backend

Keep `ui/theme/` unchanged.
