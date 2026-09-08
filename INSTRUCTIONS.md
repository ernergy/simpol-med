# SIMPLE Update 04 - Supabase email confirmation

Before testing, in Supabase:
Authentication -> URL Configuration -> Redirect URLs
Add:
simple://auth-confirm

Keep email confirmation enabled.

Upload these files over the current project, then in Android Studio:
git pull
Sync Gradle
Build -> Assemble Project
Run

Test with a clean user:
CREATE ACCOUNT -> open confirmation email -> tap link -> SIMPLE opens -> SIGN IN.
