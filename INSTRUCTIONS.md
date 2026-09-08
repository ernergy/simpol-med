# SIMPLE Update 04.1 Diagnostic

Replace only:
app/src/main/java/com/simple/medai/screens/LoginScreen.kt

This version:
- adds a 15-second timeout to signup/login
- always displays a diagnostic result
- sanitizes URLs and API keys from error text

After uploading to GitHub:
git pull
Build -> Assemble Project
Run

Then press CREATE ACCOUNT once and report the exact card message:
SIGNUP OK / SIGNUP TIMEOUT / SIGNUP ERROR
