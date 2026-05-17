@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

call "C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=notification-service ^
  -Dsonar.projectName="notification-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_72ff0cf529b54565e899a3fe2d19d9eebdacf3ee

if %ERRORLEVEL% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=notification-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
