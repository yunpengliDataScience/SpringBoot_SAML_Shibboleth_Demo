Sure. Here is a clean summary for your **Spring Boot SP + Shibboleth IdP** project.

## Enable Spring Boot SAML AuthnRequest Signing

The goal is:

```text
Spring Boot SP
      │
      │ Signed AuthnRequest
      │ spring-saml.key
      ▼
Shibboleth IdP
      │
      │ verifies using
      │ spring-saml.crt from SP metadata
      ▼
Authentication
```

### 1. Generate Spring's SAML signing key and certificate

Using OpenSSL:

```bat
openssl req -x509 -newkey rsa:2048 ^
  -keyout spring-saml.key ^
  -out spring-saml.crt ^
  -sha256 ^
  -days 3650 ^
  -nodes ^
  -subj "/CN=SpringBoot_Shibboleth_SAML_App1/O=Dragon/C=US"
```

This produces:

```text
spring-saml.key    ← PRIVATE key; Spring uses this to sign
spring-saml.crt    ← PUBLIC certificate; Shibboleth uses this to verify
```

Check them:

```bat
openssl pkey -in spring-saml.key -text -noout

openssl x509 -in spring-saml.crt -text -noout
```

Never give `spring-saml.key` to Shibboleth.

---

### 2. Put the files in Spring

For your project:

```text
src/main/resources/
│
└── saml/
    ├── shibboleth-metadata.xml
    ├── spring-saml.key
    └── spring-saml.crt
```

For development this is convenient. In production, don't package a private signing key into the application JAR; use an appropriately protected external secret/keystore.

---

### 3. Configure Spring's signing credential

In `application.yml`:

```yaml
spring:
  security:
    saml2:
      relyingparty:
        registration:
          shibboleth:
            entity-id: SpringBoot_Shibboleth_SAML_App1

            signing:
              credentials:
                - private-key-location: classpath:saml/spring-saml.key
                  certificate-location: classpath:saml/spring-saml.crt

            assertingparty:
              metadata-uri: classpath:saml/shibboleth-metadata.xml
```

This gives Spring the private key/certificate it can use for SAML signing.

---

### 4. Tell Spring that Shibboleth wants signed AuthnRequests

In the Shibboleth IdP metadata that **Spring consumes**:

```text
src/main/resources/saml/shibboleth-metadata.xml
```

find:

```xml
<md:IDPSSODescriptor
    protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
```

and change it to:

```xml
<md:IDPSSODescriptor
    WantAuthnRequestsSigned="true"
    protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
```

This is the piece that made the difference in your setup.

Together:

```text
signing.credentials
        +
WantAuthnRequestsSigned="true"
        │
        ▼
Spring signs AuthnRequest
```

Restart Spring Boot after changing the metadata.

---

### 5. Update Spring SP metadata on Shibboleth

Open Spring's generated SP metadata:

```text
https://localhost:9091/saml2/metadata/shibboleth
```

It should contain Spring's public signing certificate in a signing `KeyDescriptor`.

Save/update that metadata on the Shibboleth side:

```text
shibboleth-idp/
└── metadata/
    └── springboot-sp-metadata.xml
```

Shibboleth should get the **public certificate through this metadata**, not the private key.

So:

```text
Spring                         Shibboleth
------                         ----------
spring-saml.key     ❌ NEVER → IdP

spring-saml.crt
       ↓
Spring SP metadata  ─────────→ IdP
```

---

## Verify That Spring Is Actually Signing

Start authentication using:

```text
https://localhost:9091/saml2/authenticate/shibboleth
```

Spring redirects the browser to something like:

```text
https://localhost:8443/idp/profile/SAML2/Redirect/SSO?...
```

Use Chrome **Developer Tools → Network → Preserve log**, select the request to `/Redirect/SSO`, and inspect its query parameters.

### Unsigned request

Before signing was enabled, yours looked like:

```text
SAMLRequest=...
RelayState=...
```

There was no signature.

### Signed request

After the configuration change, yours contained:

```text
SAMLRequest=...
RelayState=...
SigAlg=...
Signature=...
```

Specifically, your actual request showed:

```text
SigAlg=
http://www.w3.org/2001/04/xmldsig-more#rsa-sha256
```

and:

```text
Signature=ESqhyVfgqPB7Z1L...
```

That confirms the HTTP-Redirect AuthnRequest is signed using **RSA-SHA256**.

So the quickest verification rule is:

```text
SAMLRequest
RelayState
       ↓
NO SigAlg / Signature
       ↓
❌ unsigned


SAMLRequest
RelayState
SigAlg
Signature
       ↓
✅ signed
```

One final distinction: **Spring is now signing the AuthnRequest**, but if you want Shibboleth to *reject* any unsigned AuthnRequest from this SP, that's a separate IdP-side enforcement setting.
