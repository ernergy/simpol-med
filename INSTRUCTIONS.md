# SIMPLE Update 05 — Clean Auth + Real Dashboard

Changes:
- Removed Development Mode.
- Removed email confirmation/deep-link code.
- Removed simple://auth-confirm from AndroidManifest.
- Auth is now only email + password.
- Dashboard shows the real logged-in email.
- Credits load from the real Supabase profile.
- BUY CREDITS remains inside the user's account.
- Study sessions now always belong to an authenticated user.

Supabase must remain:
mailer_autoconfirm = true
external_email_enabled = true

Upload these files over the current GitHub repository.

Then in Android Studio:
git pull origin main
Build -> Clean Project
Build -> Assemble Project
Run

Test:
1. Create account.
2. Confirm immediate entry.
3. Check email shown in dashboard.
4. Check credits.
5. Sign out.
6. Sign back in.
7. Upload a medical book and continue.
