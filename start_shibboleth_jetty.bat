:: Get the directory where the script is actually located
set "BASE_DIR=%~dp0"
:: Remove the trailing backslash
set "BASE_DIR=%BASE_DIR:~0,-1%"

set "SHIBBOLETH_IDP_HOME=%BASE_DIR%\shibboleth-idp"

:: set "JAVA_HOME=%BASE_DIR%\jdk-21.0.8"
set "JAVA_HOME=C:\Java\jdk-21"
echo JAVA_HOME is set to: %JAVA_HOME%

set IDP_BASE_URL=http://localhost:8080/idp

call "%SHIBBOLETH_IDP_HOME%\bin\runjetty.bat"
