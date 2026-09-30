REM ----------------------------------------------------------------------------------------------------------------------------
REM Generate public/private keys using OpenSSL.
REM Download OpenSSL at:
REM https://kb.firedaemon.com/support/solutions/articles/4000121705-openssl-binary-distributions-for-microsoft-windows#ZIP-File
REM -----------------------------------------------------------------------------------------------------------------------------
:: Get the directory where the script is actually located
set "BASE_DIR=%~dp0"
:: Remove the trailing backslash
set "BASE_DIR=%BASE_DIR:~0,-1%"

set "OPENSSL_HOME=%BASE_DIR%\openssl-4.0.1"
set "OPENSSL_CONF=%OPENSSL_HOME%\ssl\openssl.cnf"
set "PATH=%OPENSSL_HOME%\x64\bin;%PATH%"

cd "%BASE_DIR%\spring-certs"


openssl req -x509 -newkey rsa:2048 ^
  -keyout spring-saml.key ^
  -out spring-saml.crt ^
  -sha256 ^
  -days 3650 ^
  -nodes ^
  -subj "/CN=SpringBoot_Shibboleth_SAML_App/O=Dragon/C=US"

cd ..
