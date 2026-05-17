@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

call "C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=expense-service ^
  -Dsonar.projectName="expense-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_b15752fe0d8a20bb7998e9bcce124a6c65678ccf

if %ERRORLEVEL% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=expense-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
