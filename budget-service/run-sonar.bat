@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo  Service: budget-service
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

"C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=budget-service ^
  -Dsonar.projectName="budget-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_23b6f8434b683c8c10d2e4e458e31209c4042be4

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=budget-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
