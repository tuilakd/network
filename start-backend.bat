@echo off
chcp 65001 >nul
title Mini Messenger - Backend Server
cd /d "%~dp0"

echo ========================================================
echo       KHOI DONG BACKEND SERVER (SPRING BOOT)
echo ========================================================
echo.

:: 1. Kiem tra Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [LOI] Khong tim thay Java! Vui long cai dat JDK 17 tro len.
    pause
    exit /b 1
)

:: 2. Kiem tra Docker & khoi dong MySQL (neu co Docker)
where docker >nul 2>&1
if %errorlevel% equ 0 (
    echo [1/2] Dang kiem tra container MySQL qua Docker...
    docker compose up -d mysql >nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] MySQL Docker container da san sang tai cong 3306!
    ) else (
        echo [CANH BAO] Khong the khoi dong Docker MySQL, se thu ket noi MySQL co san tren may.
    )
) else (
    echo [THONG TIN] Khong tim thay Docker, he thong se su dung MySQL Server co san tren may.
)

:: 3. Khoi dong Spring Boot
echo.
echo [2/2] Dang khoi dong Backend Spring Boot tai http://localhost:8080 ...
echo Nhan Ctrl+C de dung server.
echo.
call gradlew.bat :network-backend:bootRun

if %errorlevel% neq 0 (
    echo.
    echo [LOI] Backend bi dung hoac xay ra loi khi khoi dong!
    pause
)
