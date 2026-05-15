@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

call "C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=income-service ^
  -Dsonar.projectName="income-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_ea9d2a6506f8b7589ddf290eb515e9d9d609e4e0

if %ERRORLEVEL% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=income-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
