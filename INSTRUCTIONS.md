# SIMPLE Update 04.2 - Logcat Auth Diagnostic

Replace only:
app/src/main/java/com/simple/medai/screens/LoginScreen.kt

After upload to GitHub:
git pull
Build -> Assemble Project
Run

In Android Studio Logcat use this search:
SIMPLE_AUTH

Then press CREATE ACCOUNT once.

Expected sequence:
LoginScreen opened
CREATE ACCOUNT pressed
SIGNUP request started
SIGNUP redirect = simple://auth-confirm
then one of:
SIGNUP success
SIGNUP timeout after 15 seconds
SIGNUP error: ...

Copy only the SIMPLE_AUTH lines.
