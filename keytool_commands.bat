
cd .\spring-certs

keytool -genkeypair ^
  -alias springboot ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 3650 ^
  -storetype PKCS12 ^
  -keystore springboot-server.p12 ^
  -storepass changeit ^
  -keypass changeit ^
  -dname "CN=localhost, OU=Development, O=Dragon, L=Local, ST=MD, C=US" ^
  -ext "SAN=dns:localhost,ip:127.0.0.1"

cd ..