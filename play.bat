@echo off
setlocal

echo Recherche de Java...

REM 1. Essayer la commande java standard
java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo Java trouve dans le PATH.
    java -jar target\Java-1.0-SNAPSHOT.jar
    goto end
)

REM 2. Chercher dans Android Studio
for /d %%i in ("C:\Program Files\Android\Android Studio\jbr*") do set JAVA_HOME=%%i
if defined JAVA_HOME (
    echo Java trouve dans Android Studio.
    "%JAVA_HOME%\bin\java.exe" -jar target\Java-1.0-SNAPSHOT.jar
    goto end
)

REM 3. Chercher dans IntelliJ
for /d %%i in ("C:\Program Files\JetBrains\IntelliJ IDEA*\jbr*") do set JAVA_HOME=%%i
if defined JAVA_HOME (
    echo Java trouve dans IntelliJ.
    "%JAVA_HOME%\bin\java.exe" -jar target\Java-1.0-SNAPSHOT.jar
    goto end
)

echo ERREUR : Java n'a pas ete trouve.
echo Veuillez installer Java ou lancer ce script depuis un environnement configure.

:end
pause