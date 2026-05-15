@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================

mvn clean verify sonar:sonar ^
  -Dsonar.projectKey=recurring-service ^
  -Dsonar.projectName='recurring-service' ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_4d1c8ba64ef4f21492efc28a4ea71b3d2328b3a3

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
echo  Dashboard: http://localhost:9000/dashboard?id=recurring-service
echo ============================================
pause
