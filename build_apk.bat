@echo off
REM ============================================
REM  自动答题助手 - 构建脚本
REM  需要安装 Android Studio 或 Android SDK
REM ============================================

echo ============================================
echo   自动答题助手 APK 构建脚本
echo ============================================
echo.

REM 检查 Java
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到 Java，请安装 JDK 17+
    echo        下载: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java 已安装

REM 检查 ANDROID_HOME
if "%ANDROID_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
        echo [OK] 找到 Android SDK: %ANDROID_HOME%
    ) else (
        echo [警告] ANDROID_HOME 未设置
        echo        请安装 Android Studio 并设置 SDK
    )
) else (
    echo [OK] ANDROID_HOME = %ANDROID_HOME%
)

echo.
echo 开始构建 APK...
echo.

call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo  构建成功！
    echo  APK 位置: app\build\outputs\apk\debug\app-debug.apk
    echo ============================================
) else (
    echo.
    echo [错误] 构建失败，请检查错误信息
)

pause