@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================

mvn clean verify sonar:sonar ^
  -Dsonar.projectKey=subscription-service ^
  -Dsonar.projectName='subscription-service' ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_cd2e6c894bbccfa4bdfe2d9975f16e1e5d87064f

if %errorlevel% neq 0 (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
    pause
    exit /b %errorlevel%
)

echo.
echo ============================================
echo  SUCCESS: Analysis pushed to SonarQube!
echo  Dashboard: http://localhost:9000/dashboard?id=subscription-service
echo ============================================
pause
