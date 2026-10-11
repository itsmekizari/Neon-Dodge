# Neon Dodge 🎮

**Neon Dodge** က Android ဖုန်းအတွက် portrait arcade game တစ်ခုပါ။ Java နဲ့ Android Canvas ကိုသုံးထားတာကြောင့် ပုံဖိုင်၊ အသံဖိုင်၊ third-party game library မလိုပါဘူး။ APK install ပြီးသွားရင် အင်တာနက်မလိုဘဲ ကစားနိုင်ပါတယ်။

## ကစားနည်း

1. ဂိမ်းကိုဖွင့်ပြီး **TAP TO START** ကိုနှိပ်ပါ။
2. လက်ချောင်းကို ဘယ်/ညာ ဆွဲရွှေ့ပြီး ယာဉ်ကို ထိန်းပါ။
3. အနီရောင် block တွေကို ရှောင်ပါ။ တိုက်မိရင် Game Over ဖြစ်ပါတယ်။
4. အပြာနုရောင် orb တွေကို စုပါ။ တစ်ခုကို အမှတ် ၂၅ ရပါတယ်။
5. အသက်ရှင်နေနိုင်တဲ့အချိန်ကြာလာတာနဲ့ score နဲ့ အတားအဆီးအမြန်နှုန်း တိုးလာမယ်။ Best Score ကို ဖုန်းထဲမှာ သိမ်းထားပေးပါတယ်။

## GitHub ကနေ APK ရယူနည်း

1. ဒီ project ကို GitHub repository ထဲတင်ပါ။
2. GitHub repository ရဲ့ **Actions** tab ကိုဖွင့်ပါ။
3. **Build Android APK** workflow ကိုရွေးပြီး **Run workflow** ကိုနှိပ်ပါ။ `main` သို့မဟုတ် `master` branch ပေါ် push လုပ်တိုင်းလည်း အလိုအလျောက် build ပါမယ်။
4. Workflow run အောင်မြင်သွားရင် အောက်ဆုံးက **Artifacts** အပိုင်းရှိ `NeonDodge-debug-apk` ကိုနှိပ်ပြီး ZIP ကို download လုပ်ပါ။
5. ZIP ကိုဖြည်ပြီး `app-debug.apk` ကို Android ဖုန်းထဲမှာ install လုပ်ပါ။ လိုအပ်ရင် ဖုန်း Settings ထဲက install unknown apps permission ကို ခွင့်ပြုပါ။

## ဖိုင်ဖွဲ့စည်းပုံ

```text
NeonDodge/
├── .github/workflows/android-build.yml
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/train/neondodge/
│       │   ├── MainActivity.java
│       │   └── GameView.java
│       └── res/values/styles.xml
├── gradle/wrapper/gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README_MY.md
```

## ဖွံ့ဖြိုးရေးအချက်အလက်

- Package name: `com.train.neondodge`
- Minimum Android: Android 6.0 (API 23)
- Compile SDK: Android 15 (API 35)
- Java: 8 source compatibility, JDK 17 build environment
- Gradle: 8.7
- Third-party runtime dependency မရှိပါ။

## ပြင်ဆင်ရန်

- ဂိမ်း logic နဲ့ graphics: `app/src/main/java/com/train/neondodge/GameView.java`
- App launch နဲ့ fullscreen: `app/src/main/java/com/train/neondodge/MainActivity.java`
- Android version / SDK: `app/build.gradle`
- GitHub APK build workflow: `.github/workflows/android-build.yml`

## Build မှတ်ချက်

GitHub Actions က Android SDK၊ JDK နဲ့ Gradle ကိုပြင်ဆင်ပြီး Debug APK ကို build လုပ်ကာ artifact အဖြစ်သိမ်းပေးပါတယ်။ ဒီ project ကို local Android SDK environment မှာ compile စမ်းသပ်ထားခြင်း မဟုတ်သေးပါဘူး၊ အောင်မြင်မှုကို GitHub Actions run ကနေ အတည်ပြုရပါမယ်။
