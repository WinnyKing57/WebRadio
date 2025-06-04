@echo off
REM Simplified Windows batch script for Gradle
set DIR=%~dp0
"%DIR%\gradle\wrapper\gradle-wrapper.jar" %*
