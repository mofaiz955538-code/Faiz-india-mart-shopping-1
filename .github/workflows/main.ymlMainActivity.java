name: FAIZMART V50 APK Build

on:
  workflow_dispatch:
  push:
    branches:
      - main

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.10'

      - name: Prepare Android project
        shell: bash
        run: |
          mkdir -p app/src/main/java/com/faizmart/india
          mkdir -p app/src/main/res/values
          mkdir -p app/src/main/res/drawable

          cp MainActivity.java app/src/main/java/com/faizmart/india/MainActivity.java
          cp SupabaseConfig.java app/src/main/java/com/faizmart/india/SupabaseConfig.java

          cp AndroidManifest.xml app/src/main/AndroidManifest.xml
          cp colors.xml app/src/main/res/values/colors.xml
          cp themes.xml app/src/main/res/values/themes.xml

          if [ -f ic_launcher.png ]; then
            cp ic_launcher.png app/src/main/res/drawable/ic_launcher.png
          fi

          sed -i 's/@mipmap\/ic_launcher/@drawable\/ic_launcher/g' \
            app/src/main/AndroidManifest.xml

          cat > app/build.gradle <<'EOF'
          plugins {
              id 'com.android.application'
          }

          android {
              namespace 'com.faizmart.india'
              compileSdk 35

              defaultConfig {
                  applicationId 'com.faizmart.india'
                  minSdk 23
                  targetSdk 35
                  versionCode 50
                  versionName '5.0'
          }

              buildFeatures {
                  buildConfig true
              }

              def supabaseUrl = project.findProperty("FAIZMART_SUPABASE_URL") ?: "https://YOUR-PROJECT.supabase.co"
              def supabaseKey = project.findProperty("FAIZMART_SUPABASE_ANON_KEY") ?: "YOUR_SUPABASE_ANON_KEY"

              buildConfigField "String", "SUPABASE_URL", "\"${supabaseUrl}\""
              buildConfigField "String", "SUPABASE_ANON_KEY", "\"${supabaseKey}\""
          }

          dependencies {
          }
          EOF

      - name: Build Debug APK
        run: |
          gradle assembleDebug --stacktrace

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: FAIZMART-V50-debug
          path: app/build/outputs/apk/debug/app-debug.apk
