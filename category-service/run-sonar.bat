@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo  Service: category-service
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

"C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=category-service ^
  -Dsonar.projectName="category-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_9573ee24b53abaf32ff5f7309be2be922ca3ccc0

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=category-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
