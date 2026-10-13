@REM ----------------------------------------------------------------------------
@REM WorkforceHub Maven Wrapper for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-24" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    )
)

set "FALLBACK_MVN=C:\Users\ASUS\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd"

where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    mvn %*
) else (
    if exist "%FALLBACK_MVN%" (
        call "%FALLBACK_MVN%" %*
    ) else (
        echo Error: Neither mvn nor the local Maven wrapper distribution was found.
        exit /b 1
    )
)
