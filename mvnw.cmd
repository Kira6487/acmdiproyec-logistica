@REM Maven Wrapper bootstrap script for Windows.
@ECHO OFF
SETLOCAL
SET BASE_DIR=%~dp0
SET WRAPPER_DIR=%BASE_DIR%.mvn\wrapper
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
SET WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties

IF EXIST "%WRAPPER_JAR%" GOTO runMaven
FOR /F "tokens=1,* delims==" %%A IN ('findstr /B wrapperUrl "%WRAPPER_PROPERTIES%"') DO SET WRAPPER_URL=%%B
IF "%WRAPPER_URL%"=="" (
  ECHO No se encontro wrapperUrl en %WRAPPER_PROPERTIES% 1>&2
  EXIT /B 1
)
IF NOT EXIST "%WRAPPER_DIR%" MKDIR "%WRAPPER_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
IF ERRORLEVEL 1 EXIT /B 1

:runMaven
java -Dmaven.multiModuleProjectDirectory="%BASE_DIR:~0,-1%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
ENDLOCAL
