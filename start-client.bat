@echo off
chcp 65001 >nul
title ChatWave - Desktop Client
cd /d "%~dp0"

echo ========================================================
echo          KHOI DONG CLIENT DESKTOP (CHATWAVE)
echo ========================================================
echo.

:: 1. Kiem tra Java
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [LOI] Khong tim thay javac! Vui long cai dat JDK 17 tro len.
    pause
    exit /b 1
)

:: 2. Bien dich code Java Swing (neu chua co hoac can cap nhat)
echo [1/2] Dang kiem tra va bien dich ma nguon Client...
if not exist "clientswing\clientswing\build\classes" (
    mkdir "clientswing\clientswing\build\classes"
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = (Get-ChildItem -Recurse -Filter '*.java' clientswing\clientswing\src).FullName; javac -encoding UTF-8 -d clientswing\clientswing\build\classes $files"

if %errorlevel% neq 0 (
    echo [LOI] Bien dich Client that bai!
    pause
    exit /b 1
)

:: 3. Khoi chay ung dung
echo [2/2] Dang mo ung dung ChatWave...
echo.
java -cp "clientswing\clientswing\build\classes" chatwave.Main

if %errorlevel% neq 0 (
    echo.
    echo [THONG TIN] Ung dung ChatWave da dong.
    pause
)
