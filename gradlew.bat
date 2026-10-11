@echo off
setlocal
set APP_HOME=%~dp0
set GRADLE_VERSION=8.7
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle -p "%APP_HOME%" %*
  exit /b %ERRORLEVEL%
)
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set DIST_ROOT=%GRADLE_USER_HOME%\neon-dodge-gradle
set GRADLE_HOME=%DIST_ROOT%\gradle-%GRADLE_VERSION%
if exist "%GRADLE_HOME%\bin\gradle.bat" goto runGradle
if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
set ZIP=%DIST_ROOT%\gradle-%GRADLE_VERSION%-bin.zip
echo Gradle %GRADLE_VERSION% ကို official distribution ကနေ download လုပ္ေနပါတယ္...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
if errorlevel 1 exit /b %ERRORLEVEL%
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%DIST_ROOT%' -Force"
if errorlevel 1 exit /b %ERRORLEVEL%
:runGradle
call "%GRADLE_HOME%\bin\gradle.bat" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
