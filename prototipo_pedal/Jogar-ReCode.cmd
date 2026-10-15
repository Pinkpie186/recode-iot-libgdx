@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Jogar-ReCode.ps1"
if errorlevel 1 pause
