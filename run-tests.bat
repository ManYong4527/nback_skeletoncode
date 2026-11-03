@echo off
REM Run all JVM unit tests for the app.
REM Pins JAVA_HOME to the IntelliJ-managed JBR 17 that's known to work on this machine.
REM After the run, the HTML report lives at:
REM   app\build\reports\tests\testDebugUnitTest\index.html

setlocal
set "JAVA_HOME=C:\Users\45274\.jdks\jbr-17.0.14"
set "PATH=%JAVA_HOME%\bin;%PATH%"

pushd "%~dp0"
call gradlew.bat :app:testDebugUnitTest --console=plain %*
set "EXIT_CODE=%ERRORLEVEL%"
popd

endlocal & exit /b %EXIT_CODE%
