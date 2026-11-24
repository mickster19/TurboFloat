# TurboFloat Pro — Game Booster & Floating Eye Overlay (Android Kotlin)

## Vipengele Vikuu
1. **Non-Blocking Floating Eye Bubble (`FLAG_NOT_FOCUSABLE`)**:
   - Haizui miguso (*touches*) ya mchezo wako kama **eFootball** kwa nyuma.
   - **Gusa mara moja (1-Tap)**: Inaficha au kurudisha dirisha la overlay papo hapo.
   - **Buruta (Drag)**: Inaweza kuhamishwa popote pembeni mwa skrini.
2. **Real Android Game Booster Engine**:
   - Inasafisha michakato ya nyuma (*background processes*) ili kuokoa RAM halisi bila kuzima mchezo wako (`jp.konami.pesam`) wala WhatsApp (`com.whatsapp`).
   - Inapima **Live FPS** moja kwa moja kupitia `Choreographer` ya Android.
   - Inawasha **Gaming Do Not Disturb (DND)** kuzuia simu/jumbe zisikate mchezo.
3. **Freeform Floating Window Launcher**:
   - Inafungua WhatsApp, Telegram, YouTube, au Chrome kama dirisha dogo linaloelea juu ya mchezo.

## Jinsi ya Ku-build APK Kupitia Termux + GitHub Actions
```bash
unzip TurboFloat-Android-Project.zip
cd TurboFloat
git init
git add .
git commit -m "Initial TurboFloat Android project"
git branch -M main
git remote add origin https://github.com/USERNAME/TurboFloat.git
git push -u origin main
```
Baada ya ku-push, nenda kwenye tab ya **Actions** kwenye GitHub repo yako na pakua **TurboFloat-Debug-APK**.
