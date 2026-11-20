# Spelling Bee Quiz – Android app

A spelling quiz app: it reads each word aloud, you type the spelling, and at the
end of the game it shows your score with every word's **meaning**.

- Easy / Medium / Hard banks of 1,000 words each; every game picks 10 at random with no repeats until the bank is used up
- Or type your own list (`word - meaning`, one per line)
- "Say it slowly" and "How many letters?" hints
- Results table with meanings, replay any word, practise missed words
- Works offline; uses the phone's built-in text-to-speech voice

## Option A – Build with Android Studio (easiest on a PC)
1. Install Android Studio (free): https://developer.android.com/studio
2. Unzip this folder, then in Android Studio choose **File > Open** and pick the `SpellingBeeQuiz` folder.
3. Wait for "Gradle sync" to finish (first time downloads tools; needs internet).
   If Android Studio asks which Gradle to use, choose **Gradle 8.7 or newer** / "Use Gradle wrapper" and let it create one.
4. Menu **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
5. Click **locate** in the pop-up. The file is `app/build/outputs/apk/debug/app-debug.apk`.
6. Copy the APK to the phone and open it (allow "Install unknown apps" when asked).

## Option B – Build for free on GitHub (no installs needed)
1. Create a new GitHub repository and upload all files in this folder
   (keep the hidden `.github` folder).
2. Open the **Actions** tab – the "Build APK" job runs automatically (about 3–5 minutes).
3. Open the finished run and download **SpellingBeeQuiz-apk** under *Artifacts*.
4. Unzip it, send `app-debug.apk` to the phone and install.

## Changing the built-in word lists
Edit `app/src/main/assets/index.html` – look for `const BANK=` near the bottom.
Each word is written as `["word","meaning"]` inside the easy, medium or hard list.

## If words are not read aloud
Phone Settings > search "Text-to-speech" > make sure a speech engine
(e.g. Speech Services by Google) is installed and English is downloaded.
