@echo off
setlocal

REM Tentative de detection automatique de Java dans les dossiers courants
for /d %%i in ("C:\Program Files\Android\Android Studio\jbr*") do set JAVA_HOME=%%i
if not defined JAVA_HOME (
    for /d %%i in ("C:\Program Files\JetBrains\IntelliJ IDEA*\jbr*") do set JAVA_HOME=%%i
)

if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -jar target\guild-adventure-1.0.0.jar
) else (
    echo Java n'a pas ete trouve automatiquement.
    echo Veuillez installer Java ou l'ajouter au PATH.
)
pause
