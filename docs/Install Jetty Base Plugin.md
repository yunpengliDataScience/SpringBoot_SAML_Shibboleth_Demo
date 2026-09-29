Yes. Since you're on **Windows** and already installed Shibboleth IdP, here is the current **Jetty Base Plugin** procedure. This is the new replacement for the old Jetty MSI approach. Shibboleth recommends Jetty 12.1 for IdP 5.x, and the plugin requires IdP 5.1+. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

I'll assume your IdP is here:

```text
C:\opt\shibboleth-idp
```

and you're using IdP **5.2.x**.

## 1. Open Command Prompt, not PowerShell

This matters on Windows. The plugin's `.bat` scripts are designed for `cmd.exe`; if run from PowerShell, environment changes may not propagate correctly. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

Open **Command Prompt as Administrator**, then:

```bat
cd C:\opt\shibboleth-idp
```

Verify:

```bat
java -version
```

and:

```bat
echo %JAVA_HOME%
```

If `JAVA_HOME` is empty, tell me what `java -version` and `where java` return before proceeding.

## 2. Install the Jetty Base Plugin

Run:

```bat
bin\plugin.bat -I net.shibboleth.idp.plugin.jetty
```

Note the uppercase `-I`.

This installs the latest compatible Jetty Base Plugin and automatically installs/enables its `jetty.Core` module. The plugin itself does **not contain Jetty**; it installs the configuration and management scripts that will download and manage Jetty for you. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

Verify the installation:

```bat
bin\plugin.bat -l
```

You should see something corresponding to:

```text
net.shibboleth.idp.plugin.jetty
```

The current plugin release documented by Shibboleth is 1.0.1. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

## 3. Look at what the plugin created

Now look inside:

```text
C:\opt\shibboleth-idp
```

You should have two important directories:

```text
C:\opt\shibboleth-idp
│
├── jetty-base-12
│
└── jetty-dist
```

Their purposes are very different:

```text
jetty-base-12
      │
      └── YOUR Jetty configuration

jetty-dist
      │
      └── actual Jetty distributions
```

Don't modify files inside `jetty-dist` manually. Shibboleth specifically treats that directory as replaceable downloaded software. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

This architecture is one of the main advantages of modern Jetty:

```text
JETTY_HOME
    =
Jetty software

        +

JETTY_BASE
    =
your configuration
```

So upgrading Jetty doesn't require rebuilding all of your configuration.

## 4. Download Jetty 12.1

The plugin gives you a script:

```text
bin\downloadjetty.bat
```

The official documentation currently illustrates this with Jetty `12.1.4`: ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

```bat
bin\downloadjetty.bat 12.1.4
```

The script downloads Jetty, verifies it, and extracts it under:

```text
C:\opt\shibboleth-idp\jetty-dist\
```

You'll end up with something resembling:

```text
C:\opt\shibboleth-idp
│
├── jetty-base-12
│
└── jetty-dist
    │
    └── jetty-home-12.1.4
```

The exact directory naming is handled by the plugin, so don't manually create it.

## 5. Tell Shibboleth which Jetty Base to use

Run:

```bat
bin\setjettybase.bat 12
```

This sets:

```text
JETTY_BASE
```

to the plugin's Jetty 12 configuration directory. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

Check:

```bat
echo %JETTY_BASE%
```

It should point to something like:

```text
C:\opt\shibboleth-idp\jetty-base-12
```

## 6. Tell it which Jetty version to use

Run:

```bat
bin\setjettyversion.bat 12.1.4
```

This sets:

```text
JETTY_HOME
```

to the Jetty version you downloaded. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

Check:

```bat
echo %JETTY_HOME%
```

You should now conceptually have:

```text
JAVA_HOME
   │
   ▼
Java

JETTY_HOME
   │
   ▼
Jetty 12.1.x software

JETTY_BASE
   │
   ▼
Shibboleth Jetty configuration

IDP_HOME
   │
   ▼
C:\opt\shibboleth-idp
```

## 7. Check the Jetty configuration

The important configuration file is:

```text
C:\opt\shibboleth-idp\jetty-base-12\start.d\shibboleth.ini
```

Open it in VS Code, Notepad++, etc.

The plugin defaults to:

```text
HTTP  : 8080
HTTPS : 8443
```

and initially binds the network interface to:

```text
127.0.0.1
```

That means **only your own computer can access it**, which is ideal while we're building the demo. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

For now, **don't change these settings**.

We'll use:

```text
http://localhost:8080
```

for initial testing.

Later we'll move to HTTPS.

## 8. Start Jetty

From:

```text
C:\opt\shibboleth-idp
```

run:

```bat
bin\runjetty.bat
```

Don't close this window.

Jetty runs in the foreground, so you should start seeing log messages.

Conceptually:

```text
runjetty.bat
     │
     ▼
JETTY_HOME
Jetty 12.1
     │
     ▼
JETTY_BASE
jetty-base-12
     │
     ▼
idp.war
     │
     ▼
Shibboleth IdP
```

The plugin's default configuration deploys the Shibboleth IdP WAR for you. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199500577/Installation?utm_source=chatgpt.com "Installation - Identity Provider 5 - Confluence"))

## 9. Test the IdP

Open **another Command Prompt**.

Run:

```bat
cd C:\opt\shibboleth-idp
```

Then set:

```bat
SET IDP_BASE_URL=http://localhost:8080/idp
```

Now run:

```bat
bin\status.bat
```

This exact `IDP_BASE_URL` adjustment is needed because the plugin initially uses Jetty's non-privileged HTTP port 8080. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

If everything is working, the status command should return information about the running IdP rather than a connection error.

You can also try in your browser:

```text
http://localhost:8080/idp/
```

Don't worry if this isn't a beautiful Keycloak-style page. Shibboleth doesn't work that way.

## 10. Don't configure a Windows service yet

At this point I'd stop.

We want to prove:

```text
Java
  ↓
Jetty 12.1
  ↓
Shibboleth IdP
  ↓
status.bat
       ✓
```

before adding Windows Service, HTTPS, Active Directory or Spring Boot.

Once that works, the plugin can turn Jetty into a proper Windows service using **Apache Procrun**. The official sequence starts by downloading Procrun and selecting its version, then uses `config-jetty.bat INSTALL`; this creates a service named `shibd_idp`, configures ACL/firewall settings, and starts it. ([Shibboleth](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152 "JettyBasePlugin - Identity Provider Plugins - Confluence"))

For example, using the documentation's current example:

```bat
bin\downloadprocrun.bat 1.4.1

bin\setprocrunversion.bat 1.4.1

bin\config-jetty.bat INSTALL
```

But **don't run those three commands yet**. First we should make sure console-mode Jetty starts successfully.

### So your commands right now are

```bat
cd C:\opt\shibboleth-idp

bin\plugin.bat -I net.shibboleth.idp.plugin.jetty

bin\plugin.bat -l

bin\downloadjetty.bat 12.1.4

bin\setjettybase.bat 12

bin\setjettyversion.bat 12.1.4

echo %JETTY_BASE%
echo %JETTY_HOME%

bin\runjetty.bat
```

Then in a **second CMD window**:

```bat
cd C:\opt\shibboleth-idp

SET IDP_BASE_URL=http://localhost:8080/idp

bin\status.bat
```

The official Jetty Base Plugin documentation is here: [Shibboleth Jetty Base Plugin documentation](https://shibboleth.atlassian.net/wiki/spaces/IDPPLUGINS/pages/3910107152?utm_source=chatgpt.com).

**If you paste the output of `bin\plugin.bat -I net.shibboleth.idp.plugin.jetty` here after Step 2, I can walk you through the rest one step at a time.**
