@echo off
echo ============================================
echo  Updating all SonarQube Reports
echo ============================================

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

:: Loop through all subdirectories in the same folder as this script
for /D %%d in (*) do (
    if exist "%%d\run-sonar.bat" (
        echo.
        echo --------------------------------------------
        echo  Running SonarQube analysis for: %%d
        echo --------------------------------------------
        pushd "%%d"
        :: '< nul' automatically answers the 'pause' prompt at the end of the bat file
        call run-sonar.bat < nul
        popd
    )
)

echo.
echo ============================================
echo  All Services Analyzed Successfully!
echo ============================================
pause
