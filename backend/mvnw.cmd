@echo off
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  mvn %*
  exit /b %ERRORLEVEL%
)
set MVNW_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.11
set MVN_BIN=%MVNW_DIR%\apache-maven-3.9.11\bin\mvn.cmd
if not exist "%MVN_BIN%" (
  mkdir "%MVNW_DIR%" 2>nul
  powershell -NoProfile -Command "Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile '%MVNW_DIR%\maven.zip'; Expand-Archive '%MVNW_DIR%\maven.zip' '%MVNW_DIR%' -Force; Remove-Item '%MVNW_DIR%\maven.zip'"
)
call "%MVN_BIN%" %*

