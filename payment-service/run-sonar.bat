@echo off
echo ============================================
echo  Running Tests + SonarQube Analysis
echo ============================================
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

call "C:\Program Files\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd" clean verify sonar:sonar ^
  -Dsonar.projectKey=payment-service ^
  -Dsonar.projectName="payment-service" ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.token=sqp_a44b0c2a8498f1312cec7ec95985367a8aec6d6d

if %ERRORLEVEL% equ 0 (
    echo.
    echo ============================================
    echo  SUCCESS: Analysis pushed to SonarQube!
    echo  Dashboard: http://localhost:9000/dashboard?id=payment-service
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  ERROR: SonarQube analysis failed.
    echo ============================================
)
pause
