@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

REM Check that compiled classes exist
if not exist "out\com\lucas\guild\libgdx\DesktopLauncher.class" (
    echo ERROR: Compiled classes not found in out\ directory.
    echo Run compile_check.bat first to compile the project.
    pause
    exit /b 1
)

REM Build a classpath argument file to bypass Windows 8191-char command line limit.
REM Combines: out (compiled classes) + all JARs from classpath.txt
set ARGSFILE=run_args.tmp

echo -cp> "%ARGSFILE%"

REM Write classpath as a single line: out;<jar1>;<jar2>;...
<nul set /p=out;>> "%ARGSFILE%"
for /f "delims=" %%i in (classpath.txt) do (
    <nul set /p=%%i>> "%ARGSFILE%"
)
echo.>> "%ARGSFILE%"

REM Main class to launch
echo com.lucas.guild.libgdx.DesktopLauncher>> "%ARGSFILE%"

echo Launching DesktopLauncher...
java @"%ARGSFILE%" 2>&1

REM Clean up
del "%ARGSFILE%" 2>nul

endlocal
