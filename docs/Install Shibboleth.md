Yes. Since you're on **Windows** and ultimately want to connect Shibboleth to your existing **Spring Boot SAML application**, I would install it in a development-friendly way first, then configure SAML afterward.

I checked the current Shibboleth documentation. As of September 2026, the stable IdP is **5.2.3**. It requires **Java 17 or newer**, and for IdP 5.2.x the Shibboleth project currently recommends **Jetty 12.1**. ([Shibboleth Consortium](https://shibboleth.net/downloads/identity-provider/latest/?utm_source=chatgpt.com "Index of /downloads/identity-provider/latest"))

One correction to my previous answer: Shibboleth still provides a Windows MSI, but the project now marks the MSI-based IdP/Jetty installers as **deprecated**. For a new installation, the recommended direction is the normal ZIP/command-line IdP installation plus the Jetty plugin. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500769?utm_source=chatgpt.com "WindowsInstallation - Identity Provider 5 - Confluence"))

## Our target setup

We'll build this first:

```text
Windows 10/11

Java 17+
   │
   ├── Shibboleth IdP 5.2.3
   │
   └── Jetty 12.1
           │
           ▼
    Shibboleth IdP
```

After that works, we'll connect:

```text
Browser
   │
   ▼
Spring Boot :9091
   │
   │ SAML AuthnRequest
   ▼
Shibboleth IdP
   │
   │ login
   ▼
Test User / Active Directory
   │
   │ SAML Response
   ▼
Spring Boot
```

I recommend **not configuring Active Directory, attributes, or Spring Boot yet**. First get a clean IdP running.

### Step 1 — Check Java

Open **Command Prompt** and run:

```bat
java -version
```

Also run:

```bat
where java
```

For example, you might see:

```text
openjdk version "17.0.x"
```

Java 17 is the minimum platform for Shibboleth IdP 5. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199511079?utm_source=chatgpt.com "SystemRequirements - Identity Provider 5 - Confluence"))

If you already have Java 17 or 21 installed, tell me what `java -version` shows before changing anything.

### Step 2 — Download Shibboleth IdP

Use the official Shibboleth download directory:

[Download Shibboleth Identity Provider 5.2.3](https://shibboleth.net/downloads/identity-provider/latest/?utm_source=chatgpt.com)

Download:

```text
shibboleth-identity-provider-5.2.3.zip
```

For this tutorial, **do not use the MSI**.

Extract it somewhere simple, for example:

```text
C:\software\shibboleth-identity-provider-5.2.3
```

You should see something similar to:

```text
C:\software\shibboleth-identity-provider-5.2.3

├── bin
├── conf
├── dist
├── docs
├── system
├── webapp
├── build.xml
└── ...
```

### Step 3 — Choose the permanent IdP location

Let's use:

```text
C:\opt\shibboleth-idp
```

Don't manually copy the extracted distribution there. The Shibboleth installer will create it.

The distinction is important:

```text
INSTALLER

C:\software\
   shibboleth-identity-provider-5.2.3\
          │
          │ install.bat
          ▼
INSTALLED IdP

C:\opt\
   shibboleth-idp\
```

Shibboleth's Windows documentation also notes that paths used in Shibboleth configuration generally should use forward slashes, such as `C:/opt/shibboleth-idp`, even on Windows. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500577/Installation?utm_source=chatgpt.com "Installation - Identity Provider 5 - Confluence"))

### Step 4 — Run the Shibboleth installer

Open **Command Prompt as Administrator**.

Then:

```bat
cd C:\software\shibboleth-identity-provider-5.2.3
```

Run:

```bat
bin\install.bat
```

You'll be asked several questions.

For our development environment, I suggest values along these lines:

```text
Installation Directory:
C:/opt/shibboleth-idp
```

For the hostname, I would eventually prefer a development hostname such as:

```text
idp.example.org
```

rather than designing a production configuration around `localhost`.

But for a purely local proof-of-concept, we can use:

```text
localhost
```

The resulting entity ID would normally resemble:

```text
https://localhost/idp/shibboleth
```

The installer creates the configuration tree and generates the initial IdP signing/encryption credentials and WAR application. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500577/Installation?utm_source=chatgpt.com "Installation - Identity Provider 5 - Confluence"))

### Step 5 — Examine the installation

After installation, look at:

```text
C:\opt\shibboleth-idp
```

You should have directories along these lines:

```text
C:\opt\shibboleth-idp
│
├── bin
│
├── conf
│
├── credentials
│
├── dist
│
├── edit-webapp
│
├── logs
│
├── metadata
│
├── views
│
└── war
```

Three directories are especially important.

`conf` is where most of your future work happens:

```text
C:/opt/shibboleth-idp/conf/
```

You'll eventually work with things such as:

```text
idp.properties
ldap.properties
attribute-resolver.xml
attribute-filter.xml
metadata-providers.xml
relying-party.xml
```

`credentials` contains important cryptographic material:

```text
C:/opt/shibboleth-idp/credentials/
```

For example, IdP signing/encryption certificates and private keys.

And:

```text
C:/opt/shibboleth-idp/war/idp.war
```

is the actual Java web application that Jetty will run.

This illustrates something important:

```text
Shibboleth IdP
       ≠
Web Server
```

Instead:

```text
        Jetty
          │
          │ runs
          ▼
       idp.war
          │
          ▼
   Shibboleth IdP
```

The official documentation describes IdP 5 as a standard Jakarta Java web application and strongly recommends Jetty as its servlet container. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500577/Installation?utm_source=chatgpt.com "Installation - Identity Provider 5 - Confluence"))

## Step 6 — Don't install an old Jetty tutorial

This is an important point because Google searches return a lot of outdated Shibboleth instructions.

For **Shibboleth 5.2.x**, don't blindly follow instructions for:

```text
Jetty 9
Jetty 10
Jetty 11
Tomcat 9
```

IdP 5.2+ requires Servlet API 6.1 or later. The currently recommended container is:

```text
Jetty 12.1+
```

Tomcat 11+ is another supported example, but Jetty is the Shibboleth project's recommended choice. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199511079?utm_source=chatgpt.com "SystemRequirements - Identity Provider 5 - Confluence"))

Also avoid tutorials written for:

```text
Shibboleth IdP 3.x
Shibboleth IdP 4.x
```

Their directory structures and Jetty instructions can differ substantially.

## Step 7 — Install Jetty

The current Shibboleth-recommended approach is the **Jetty Base Plugin**, rather than manually hacking a generic Jetty installation. The plugin provides scripts/configuration specifically designed to run the IdP and deploy its WAR. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500577/Installation?utm_source=chatgpt.com "Installation - Identity Provider 5 - Confluence"))

Conceptually you'll end up with something like:

```text
C:\
├── opt\
│   └── shibboleth-idp\
│
└── opt\
    └── jetty-base\
```

with Jetty itself kept separate:

```text
Jetty Home
     │
     ▼
Jetty Base
     │
     ├── HTTPS configuration
     ├── JVM configuration
     └── webapps
            │
            ▼
      Shibboleth idp.war
```

This separation is deliberate. Shibboleth recommends keeping IdP-specific Jetty configuration under `JETTY_BASE` rather than putting it into Jetty's installation itself, making Jetty upgrades easier. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3516104706?utm_source=chatgpt.com "Jetty12 - Identity Provider 5 - Confluence"))

### Step 8 — HTTPS matters

Unlike many Spring Boot tutorials where we happily start with:

```text
http://localhost:8080
```

SAML IdP deployment is normally designed around HTTPS.

Ultimately, you'll have something such as:

```text
https://idp.example.org/idp/
```

For local development we can create a development certificate.

Jetty will need a TLS certificate/keystore, commonly PKCS#12:

```text
idp-userfacing.p12
```

and its configuration specifies things such as:

```text
jetty.sslContext.keyStoreType=PKCS12
jetty.sslContext.keyStorePassword=...
jetty.ssl.port=443
```

The official Jetty example uses a PKCS#12 credential containing the certificate and private key for the browser-facing HTTPS connection. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3516104706?utm_source=chatgpt.com "Jetty12 - Identity Provider 5 - Confluence"))

There are actually **two different certificate concepts** here, and keeping them separate will save you considerable confusion later:

```text
HTTPS certificate
      │
      └── Browser ↔ Shibboleth

SAML signing certificate
      │
      └── Shibboleth ↔ Spring Boot
```

They do not have to be the same certificate.

### Step 9 — Start Jetty and test Shibboleth

Once Jetty is configured and running, Shibboleth provides a status command:

```bat
C:\opt\shibboleth-idp\bin\status.bat
```

A successful result gives information about the IdP environment and state.

Shibboleth explicitly points out that this only establishes that the IdP is running; it **doesn't mean authentication or SAML federation is configured yet**. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500577/Installation?utm_source=chatgpt.com "Installation - Identity Provider 5 - Confluence"))

At this stage we want:

```text
Java       ✓
Shibboleth ✓
Jetty      ✓
HTTPS      ✓
IdP starts ✓
Status     ✓
```

We **don't** yet care about:

```text
Active Directory
Spring Boot
SAML attributes
Smart cards
```

## Step 10 — Then configure authentication

Once the installation works, our next stage will be:

```text
Shibboleth
     │
     ▼
Password Authentication
     │
     ▼
LDAP
     │
     ▼
Active Directory
```

The official recommended sequence after installation is essentially: configure authentication, configure attribute resolution, test those pieces, load SP metadata, and establish an attribute-release policy. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500577/Installation?utm_source=chatgpt.com "Installation - Identity Provider 5 - Confluence"))

Then we'll add your Spring Boot application:

```text
                     SAML

Spring Boot :9091  <------->  Shibboleth
       SP                         IdP
       │                           │
       │                           ▼
       │                    Active Directory
       │
       ▼
Your application
```

That is where we'll configure your Spring Security `RelyingPartyRegistration`, Spring's SP metadata, Shibboleth's `metadata-providers.xml`, `attribute-resolver.xml`, and `attribute-filter.xml`.

**I suggest we do the installation interactively rather than jumping ahead.** Start with **Step 1** and run:

```bat
java -version
where java
```

Paste the output here. I'll check your Java environment and then give you the **exact next commands for Shibboleth 5.2.3 + Jetty 12.1 on your Windows machine**, explaining what each installer prompt means as we go.
