@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

"C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar -Dsonar.projectKey=budget-service -Dsonar.projectName="budget-service" -Dsonar.token=sqp_23b6f8434b683c8c10d2e4e458e31209c4042be4

echo.
echo ============================================
echo  Done! Open SonarQube dashboard:
echo  http://localhost:9000/dashboard?id=budget-service
echo ============================================
pause
