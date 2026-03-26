@echo off
setlocal EnableDelayedExpansion

@rem DiaSmart - Gradle Wrapper for Windows
@rem Points to the locally cached Gradle 9.2.1 distribution

set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

set GRADLE_BIN=C:\Users\PC\.gradle\wrapper\dists\gradle-9.2.1-bin\2t0n5ozlw9xmuyvbp7dnzaxug\gradle-9.2.1\bin\gradle.bat

if not exist "%GRADLE_BIN%" (
    echo ERROR: Gradle binary not found at: %GRADLE_BIN%
    exit /b 1
)

call "%GRADLE_BIN%" %*
endlocal
