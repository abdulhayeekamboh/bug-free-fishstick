# oDialer Pro

Phone app with a ColorOS-style design and silent automatic call recording.
Targets Android 8+ (built for Realme C21 on Android 10/11).

## Get the APK with GitHub
1. Create a new GitHub repository (empty).
2. Upload everything from this folder (keep the `.github` folder).
3. Open the **Actions** tab, then **Build APK**, then **Run workflow** (it also runs on every push).
4. When it finishes (about 5 minutes), open the run and download the **ODialerPro-APK** artifact.
5. Unzip it and install `app-debug.apk` on your phone (allow "install unknown apps").

## First-run setup on the phone (important for recording)
1. Allow all permissions when asked (Phone, Microphone, Call log, Contacts).
2. Settings tab: turn on **Accessibility helper** (Installed apps > oDialer Pro > On).
3. Settings tab: Battery > No restrictions. Also allow Auto-start in the Realme app info.
4. Optional: set oDialer Pro as default phone app for the full in-call screen.
5. If the other person is silent in recordings, turn on **Speaker boost**.
