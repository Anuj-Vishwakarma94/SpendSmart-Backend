@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo  Service: analytics-service
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

"C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=analytics-service ^
  -Dsonar.projectName="analytics-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_9257f755460972d238284ca973eb989a4fe12a70

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=analytics-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
