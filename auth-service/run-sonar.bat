@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

"C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar -Dsonar.token=sqp_aed6805628d1c1c5a83664a589467ea2d176d4f2

echo.
echo ============================================
echo  Done! Open SonarQube dashboard:
echo  http://localhost:9000/dashboard?id=auth-service
echo ============================================
pause
