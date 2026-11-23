@echo off
chcp 65001 >nul
title Mini Messenger & ChatWave - Khoi Chay Toan Bo
cd /d "%~dp0"

echo ========================================================
echo   HE THONG MINI MESSENGER & CHATWAVE - KHOI CHAY NHANH
echo ========================================================
echo.

:: 1. Khoi dong Database qua Docker (neu co)
where docker >nul 2>&1
if %errorlevel% equ 0 (
    echo [1/3] Dang khoi dong Database MySQL qua Docker Compose...
    docker compose up -d
    echo [OK] MySQL da san sang tai cong 3306 (phpMyAdmin: http://localhost:8081).
) else (
    echo [1/3] Khong co Docker, su dung MySQL Server san co tren may.
)
echo.

:: 2. Khoi dong Backend trong cua so rieng
echo [2/3] Dang khoi dong Backend Spring Boot trong cua so moi...
start "Mini Messenger - Backend Server" cmd /k "start-backend.bat"
echo [OK] Da mo tien trinh khoi dong Backend.
echo.

:: 3. Doi backend san sang va mo Client
echo [3/3] Dang chuan bi mo Desktop Client ChatWave...
echo Vui long cho khoang 12 giay de Backend hoan tat nap du lieu ban dau...
echo.
timeout /t 12 /nobreak >nul

start "ChatWave - Desktop Client" cmd /c "start-client.bat"

echo.
echo ========================================================
echo [HOAN TAT] He thong da duoc khoi dong day du!
echo - Backend API:  http://localhost:8080
echo - Web Demo:     http://localhost:8080/index.html
echo - Web Admin:    http://localhost:8080/admin.html
echo - phpMyAdmin:   http://localhost:8081 (neu dung Docker)
echo - Tai khoan:    admin / admin123
echo ========================================================
echo.
pause
