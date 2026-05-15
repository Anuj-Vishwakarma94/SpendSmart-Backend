@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo  Service: subscription-service
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

"C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=subscription-service ^
  -Dsonar.projectName="subscription-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_cd2e6c894bbccfa4bdfe2d9975f16e1e5d87064f

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=subscription-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
