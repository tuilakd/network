@echo off
chcp 65001 >nul
title Dong Goi ChatWave.jar
cd /d "%~dp0"

echo ========================================================
echo          DONG GOI CLIENT THANH FILE CHATWAVE.JAR
echo ========================================================
echo.

if not exist "clientswing\clientswing\build\classes" (
    mkdir "clientswing\clientswing\build\classes"
)

echo [1/2] Dang bien dich source code Client...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = (Get-ChildItem -Recurse -Filter '*.java' clientswing\clientswing\src).FullName; javac -encoding UTF-8 -d clientswing\clientswing\build\classes $files"

if %errorlevel% neq 0 (
    echo [LOI] Bien dich that bai!
    pause
    exit /b 1
)

if exist "clientswing\clientswing\src\resources" (
    xcopy /E /I /Y "clientswing\clientswing\src\resources" "clientswing\clientswing\build\classes\resources" >nul
)

echo [2/2] Dang dong goi file ChatWave.jar...
jar --create --file ChatWave.jar --main-class chatwave.Main -C clientswing\clientswing\build\classes .

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo [THANH CONG] File ChatWave.jar da duoc tao tai thu muc goc!
    echo Cach chay: java -jar ChatWave.jar (hoac click dup chuot)
    echo ========================================================
) else (
    echo [LOI] Dong goi file JAR that bai!
)
echo.
pause
