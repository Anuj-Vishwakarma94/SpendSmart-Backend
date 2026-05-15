@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

"C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" verify sonar:sonar -Dsonar.token=sqp_9257f755460972d238284ca973eb989a4fe12a70

echo.
echo ============================================
echo  Done! Open SonarQube dashboard:
echo  http://localhost:9000/dashboard?id=analytics-service
echo ============================================
pause
