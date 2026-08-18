@echo off
setlocal

rem Runs the normal Gradle build checks and builds all local artifacts:
rem - Google Play and F-Droid debug APKs
rem - signed Google Play and F-Droid release APKs
rem - signed Google Play and F-Droid release Android App Bundles

set "ANDROID_KEYSTORE_PATH=release-keystore.jks"
set /p "ANDROID_KEYSTORE_PASSWORD=Enter keystore password: "
set "ANDROID_KEY_ALIAS=release"
set "ANDROID_KEY_PASSWORD=%ANDROID_KEYSTORE_PASSWORD%"

cls
cd /d "%~dp0"

if not defined ANDROID_KEYSTORE_PATH goto missing_signing_env
if not defined ANDROID_KEYSTORE_PASSWORD goto missing_signing_env
if not defined ANDROID_KEY_ALIAS goto missing_signing_env
if not defined ANDROID_KEY_PASSWORD goto missing_signing_env

for %%I in ("%ANDROID_KEYSTORE_PATH%") do set "ANDROID_KEYSTORE_PATH=%%~fI"

if not exist "%ANDROID_KEYSTORE_PATH%" (
    echo ERROR: ANDROID_KEYSTORE_PATH does not exist:
    echo   %ANDROID_KEYSTORE_PATH%
    pause
    exit /b 1
)

if not exist "%~dp0gradlew.bat" (
    echo ERROR: gradlew.bat was not found next to this script.
    pause
    exit /b 1
)

echo Running checks, building APKs, and building signed release bundles...
call "%~dp0gradlew.bat" clean build lintFdroidDebug lintGplayDebug bundleGplayRelease bundleFdroidRelease
if errorlevel 1 (
    echo.
    echo Build failed.
    pause
    exit /b %errorlevel%
)

echo.
echo Build complete.
echo Debug APKs:
echo   %~dp0app\build\outputs\apk\gplay\debug\app-gplay-debug.apk
echo   %~dp0app\build\outputs\apk\fdroid\debug\app-fdroid-debug.apk
echo Release APKs:
echo   %~dp0app\build\outputs\apk\gplay\release\app-gplay-release.apk
echo   %~dp0app\build\outputs\apk\fdroid\release\app-fdroid-release.apk
echo Release AABs:
echo   %~dp0app\build\outputs\bundle\gplayRelease\app-gplay-release.aab
echo   %~dp0app\build\outputs\bundle\fdroidRelease\app-fdroid-release.aab
pause
exit /b 0

:missing_signing_env
echo ERROR: Required signing environment variables are missing.
echo.
echo Required variables:
echo   ANDROID_KEYSTORE_PATH
echo   ANDROID_KEYSTORE_PASSWORD
echo   ANDROID_KEY_ALIAS
echo   ANDROID_KEY_PASSWORD
pause
exit /b 1
