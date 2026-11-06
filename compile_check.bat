@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

REM Compile Java sources using javac @argfile to avoid Windows command-line
REM length limits. classpath.txt has 655 entries (far exceeding CMD's 8191-char
REM limit), so we write all arguments to a temporary file and pass it to javac.
set ARGSFILE=compile_args.tmp

REM Start the args file with compiler options
echo -d out> "%ARGSFILE%"
echo -cp>> "%ARGSFILE%"

REM Append classpath as a single continuous line (no newlines between entries)
<nul set /p=>> "%ARGSFILE%"
for /f "delims=" %%i in (classpath.txt) do (
    <nul set /p=%%i>> "%ARGSFILE%"
)
echo.>> "%ARGSFILE%"

REM Append each source file path
for /f "delims=" %%i in ('dir /s /b src\main\java\*.java') do (
    echo %%i>> "%ARGSFILE%"
)

REM Compile using response file to bypass command-line length limit
javac @"%ARGSFILE%" 2>&1

REM Clean up temp file
del "%ARGSFILE%" 2>nul

endlocal
