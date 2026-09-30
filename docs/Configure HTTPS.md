Yes. For your current Windows development setup, I recommend keeping the existing ports and changing only the protocol:

```text
Shibboleth IdP
Before: http://localhost:8080/idp
After:  https://localhost:8443/idp

Spring Boot SP
Before: http://localhost:9091
After:  https://localhost:9091
```

This is simpler than moving immediately to 443. The Shibboleth Jetty Base Plugin is already designed for HTTP 8080 and HTTPS 8443 and includes HTTPS/TLS configuration support. ([Atlassian Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152?utm_source=chatgpt.com "JettyBasePlugin - Identity Provider Plugins - Confluence"))

## 1. Create a certificate for Shibboleth

Since you already have Java, use `keytool`. From CMD:

```bat
keytool -genkeypair ^
  -alias shibboleth ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 3650 ^
  -storetype PKCS12 ^
  -keystore idp-userfacing.p12 ^
  -storepass changeit ^
  -keypass changeit ^
  -dname "CN=localhost, OU=Development, O=Dragon, L=Local, ST=MD, C=US" ^
  -ext "SAN=dns:localhost,ip:127.0.0.1"
```

This creates:

```text
idp-userfacing.p12
```

Move it to your Shibboleth credentials directory, for example:

```text
C:\Projects\SpringBoot_SAML_Shibboleth_Demo\
shibboleth-idp\credentials\idp-userfacing.p12
```

Shibboleth's Jetty documentation recommends PKCS12 for the browser-facing TLS certificate/private key. ([Atlassian Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3516104706?utm_source=chatgpt.com "Jetty12 - Identity Provider 5 - Confluence"))

## 2. Configure Shibboleth Jetty HTTPS

Your Jetty Base Plugin setup should have something similar to:

```text
shibboleth-idp\
    jetty-base-12\
        start.d\
            shibboleth.ini
```

The current Jetty Base Plugin documentation specifically identifies `start.d/shibboleth.ini` as the place to adjust HTTP/HTTPS networking settings. ([Atlassian Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152?utm_source=chatgpt.com "JettyBasePlugin - Identity Provider Plugins - Confluence"))

Open:

```text
jetty-base-12\start.d\shibboleth.ini
```

Find the SSL settings. Configure approximately:

```properties
jetty.sslContext.keyStorePath=../credentials/idp-userfacing.p12
jetty.sslContext.trustStorePath=../credentials/idp-userfacing.p12

jetty.sslContext.keyStoreType=PKCS12
jetty.sslContext.trustStoreType=PKCS12

jetty.sslContext.keyStorePassword=changeit
jetty.sslContext.trustStorePassword=changeit
jetty.sslContext.keyManagerPassword=changeit

jetty.ssl.port=8443
```

The exact relative path depends on where your `JETTY_BASE` is. The official configuration uses the same PKCS12 keystore/truststore pattern. ([Atlassian Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3516104706?utm_source=chatgpt.com "Jetty12 - Identity Provider 5 - Confluence"))

For localhost you can leave the listener restricted to loopback.

Restart Jetty:

```bat
bin\runjetty.bat
```

Then test:

```text
https://localhost:8443/idp/status
```

and:

```text
https://localhost:8443/idp/shibboleth
```

Because the certificate is self-signed, Chrome will initially warn that the certificate isn't trusted. That's expected for this development certificate.

## 3. Update the IdP metadata URLs

This part is critical.

Your Spring Boot application currently has a modified copy of Shibboleth metadata containing:

```text
http://localhost:8080/idp/profile/...
```

Change those endpoint `Location` values to:

```text
https://localhost:8443/idp/profile/...
```

For example:

```xml
<SingleSignOnService
    Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
    Location="https://localhost:8443/idp/profile/SAML2/Redirect/SSO"/>
```

You may also have:

```xml
<SingleSignOnService
    Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
    Location="https://localhost:8443/idp/profile/SAML2/POST/SSO"/>
```

Keep your IdP entity ID unchanged:

```text
https://localhost/idp/shibboleth
```

Changing the network endpoint doesn't require changing an established SAML entity ID; `idp.entityID` is the IdP's unique SAML issuer identifier. ([Atlassian Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199508044?utm_source=chatgpt.com "RelyingPartyConfiguration - Identity Provider 5 - Confluence"))

---

# 4. Create a separate certificate for Spring Boot

Now generate another certificate:

```bat
keytool -genkeypair ^
  -alias springboot ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 3650 ^
  -storetype PKCS12 ^
  -keystore springboot.p12 ^
  -storepass changeit ^
  -keypass changeit ^
  -dname "CN=localhost, OU=Development, O=Dragon, L=Local, ST=MD, C=US" ^
  -ext "SAN=dns:localhost,ip:127.0.0.1"
```

Put:

```text
springboot.p12
```

under:

```text
src\main\resources\
```

so you have:

```text
src
└── main
    └── resources
        ├── application.yml
        ├── springboot.p12
        └── saml
            └── shibboleth-metadata.xml
```

## 5. Enable HTTPS in Spring Boot

Modify your `application.yml`:

```yaml
server:
  port: 9091

  ssl:
    enabled: true
    key-store: classpath:springboot.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: springboot

spring:
  application:
    name: SpringBoot_Shibboleth_SAML_App1

  security:
    saml2:
      relyingparty:
        registration:
          shibboleth:
            entity-id: SpringBoot_Shibboleth_SAML_App1
            assertingparty:
              metadata-uri: classpath:saml/shibboleth-metadata.xml

logging:
  level:
    org.springframework.security: INFO
    org.springframework.security.saml2: DEBUG
```

Spring Boot directly supports configuring the embedded server with a PKCS12/JKS keystore using the `server.ssl.*` properties. ([Home](https://docs.spring.io/spring-boot/3.4/appendix/application-properties/?utm_source=chatgpt.com "Common Application Properties :: Spring Boot"))

Restart Spring Boot.

Then:

```text
https://localhost:9091/
```

should work.

Again, expect a browser certificate warning until we trust the development certificates.

---

# 6. Regenerate Spring's SP metadata

This is **very important**.

Previously your Spring SP metadata had:

```xml
Location="http://localhost:9091/login/saml2/sso/shibboleth"
```

Once Spring is running over HTTPS, open:

```text
https://localhost:9091/saml2/metadata/shibboleth
```

or, depending on your Spring Security version/configuration:

```text
https://localhost:9091/saml2/service-provider-metadata/shibboleth
```

Spring's default ACS path is `/login/saml2/sso/{registrationId}`, and its generated URLs can incorporate the application's `{baseUrl}`, including scheme, host and port. ([Home](https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html?utm_source=chatgpt.com "SAML 2.0 Login Overview :: Spring Security"))

The new metadata should therefore contain:

```xml
<AssertionConsumerService
    ...
    Location="https://localhost:9091/login/saml2/sso/shibboleth"
    ... />
```

Save the new metadata as:

```text
shibboleth-idp\metadata\springboot-sp-metadata.xml
```

replacing the old HTTP version.

Your entity ID can remain:

```text
SpringBoot_Shibboleth_SAML_App1
```

## 7. Restart Shibboleth

Because we've changed the Spring SP metadata that Shibboleth consumes, restart Shibboleth/Jetty.

Your metadata provider can remain:

```xml
<MetadataProvider
    id="SpringBoot_Shibboleth_SAML_App1"
    xsi:type="FilesystemMetadataProvider"
    metadataFile="%{idp.home}/metadata/springboot-sp-metadata.xml"/>
```

Shibboleth uses SP metadata to determine the relying party's SAML capabilities and endpoints. ([Atlassian Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199506698/MetadataConfiguration?utm_source=chatgpt.com "MetadataConfiguration - Identity Provider 5 - Confluence"))

---

# 8. Test the complete HTTPS SAML flow

Start here:

```text
https://localhost:9091/user
```

You should see:

```text
https://localhost:9091/user
        ↓
Spring Security
        ↓
https://localhost:8443/idp/profile/SAML2/Redirect/SSO
        ↓
Shibboleth login
        ↓
testuser / password
        ↓
Shibboleth creates SAML Response
        ↓
POST
https://localhost:9091/login/saml2/sso/shibboleth
        ↓
Spring validates response
        ↓
https://localhost:9091/user
```

At that point the entire browser-facing SAML flow is HTTPS.

## 9. One issue you'll probably encounter: certificate trust

With `keytool` self-signed certificates, Chrome will likely show:

> Your connection is not private

For initial testing you can accept the warning, but a cleaner local setup is to create a small local development CA, trust that CA in **Windows Trusted Root Certification Authorities**, and issue both:

```text
localhost Shibboleth certificate
localhost Spring Boot certificate
```

from that CA.

That eliminates browser warnings and is especially useful before we proceed to your eventual **smart-card/client-certificate authentication**, because then certificate trust becomes much more important.

One other distinction is worth preserving: these HTTPS certificates are **transport/TLS certificates**. They are separate from Shibboleth's SAML signing/encryption credentials and from the SP encryption certificate we discussed earlier.

I recommend doing this in two stages: **get `https://localhost:8443/idp/status` working first; don't change Spring yet.** Once that works, enable Spring's HTTPS and update the metadata. This makes troubleshooting much easier.

------------

Yes. Your default `.ini` is already almost exactly what you need. You **do not need to add all the properties I showed earlier**—they're already there. For your localhost HTTPS setup, I would make only a few changes.

Your current important section is:

```properties
## TLS host and port to bind to
jetty.ssl.host=127.0.0.1
#jetty.ssl.port=8443

# Non-TLS host and port to bind to
jetty.http.host=127.0.0.1
#jetty.http.port=8080
```

Change it to:

```properties
## TLS host and port to bind to
jetty.ssl.host=127.0.0.1
jetty.ssl.port=8443

# Non-TLS host and port to bind to
jetty.http.host=127.0.0.1
jetty.http.port=8080
```

The key change is simply uncommenting:

```properties
jetty.ssl.port=8443
```

I would leave HTTP `8080` enabled temporarily while we're testing HTTPS. Once HTTPS is confirmed working, we can decide whether to disable HTTP.

For the keystore section, you already have:

```properties
jetty.sslContext.keyStorePath=../credentials/idp-userfacing.p12
jetty.sslContext.trustStorePath=../credentials/idp-userfacing.p12
```

That's good **provided the file actually exists at that relative path**.

Because your Jetty base is under the Shibboleth project, this probably resolves approximately to:

```text
C:\Projects\SpringBoot_SAML_Shibboleth_Demo\
    shibboleth-idp\
        credentials\
            idp-userfacing.p12
```

Check whether you **already have**:

```text
idp-userfacing.p12
```

This is important: Shibboleth may already have generated the browser-facing PKCS#12 keystore for you. If it exists, **don't generate another one yet**.

I would also uncomment these two lines:

```properties
jetty.sslContext.keyStoreType=PKCS12
jetty.sslContext.trustStoreType=PKCS12
```

So the complete TLS section becomes:

```properties
##################################
## Network/Host/Port configuration
##################################

## TLS host and port to bind to
jetty.ssl.host=127.0.0.1
jetty.ssl.port=8443

# Non-TLS host and port to bind to
jetty.http.host=127.0.0.1
jetty.http.port=8080


######################################
## Keystore properties for TLS support
######################################

# Keystore file path (relative to $jetty.base)
jetty.sslContext.keyStorePath=../credentials/idp-userfacing.p12
jetty.sslContext.trustStorePath=../credentials/idp-userfacing.p12

# Keystore type
jetty.sslContext.keyStoreType=PKCS12
jetty.sslContext.trustStoreType=PKCS12

# Keystore passwords
jetty.sslContext.keyStorePassword=changeit
jetty.sslContext.trustStorePassword=changeit
jetty.sslContext.keyManagerPassword=changeit

# Deny SSL renegotiation
jetty.sslContext.renegotiationAllowed=false

# Skip SNI hostcheck
jetty.ssl.sniHostCheck=false
```

Leave these alone:

```properties
jetty.server.default.showContexts=false
jetty.httpConfig.redirectUriCompliance=DEFAULT,FRAGMENT
jetty.sessionIdManager.workerName=
```

Also leave:

```properties
jetty.ssl.sniHostCheck=false
```

for localhost development.

### One thing to check before changing anything else

Go to:

```text
C:\Projects\SpringBoot_SAML_Shibboleth_Demo\
shibboleth-idp\credentials\
```

and see whether this already exists:

```text
idp-userfacing.p12
```

If it does, **don't run my earlier `keytool -genkeypair` command yet**. Your default Jetty configuration is clearly already expecting that particular keystore, so it is better to inspect the existing one first.

You can check it with:

```bat
keytool -list -v ^
  -keystore credentials\idp-userfacing.p12 ^
  -storetype PKCS12 ^
  -storepass changeit
```

If that works, look for:

```text
Entry type: PrivateKeyEntry
```

and especially:

```text
SubjectAlternativeName
DNSName: localhost
```

Then enable:

```properties
jetty.ssl.port=8443
```

restart Jetty and try:

```text
https://localhost:8443/idp/status
```

If that opens—even with a browser certificate warning—then the Shibboleth HTTPS side is basically working.

**So don't create another certificate yet. Check whether `credentials\idp-userfacing.p12` already exists first.** If you show me the output of the `keytool -list -v` command (you can omit certificate fingerprints if you want), I can tell you whether the existing certificate is suitable for `https://localhost:8443`.
