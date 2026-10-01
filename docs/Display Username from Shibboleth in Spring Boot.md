Yes. Based on your actual `attribute-resolver.xml`, `HomeController.java`, and the result you tested, here is the complete summary for **displaying the authenticated username in Spring Boot**.

## Display Username from Shibboleth in Spring Boot

The goal is:

```text
User logs in as: yunpeng.li
        ↓
Shibboleth
        ↓
SAML
        ↓
Spring Boot
        ↓
username = yunpeng.li
```

### 1. Define `uid` in Shibboleth

In:

```text
shibboleth-idp/conf/attribute-resolver.xml
```

you already have:

```xml
<AttributeDefinition id="uid" xsi:type="PrincipalName" />
```

`PrincipalName` means: take the successfully authenticated principal name and create an internal Shibboleth attribute called `uid`.

For your login:

```text
Authenticated principal
        ↓
yunpeng.li
        ↓
PrincipalName
        ↓
uid = yunpeng.li
```

So this part is already correct.

### 2. Allow `uid` to be sent to Spring

In:

```text
shibboleth-idp/conf/attribute-filter.xml
```

add/keep:

```xml
<AttributeFilterPolicy id="ReleaseToSpringBoot">

    <PolicyRequirementRule
        xsi:type="Requester"
        value="SpringBoot_Shibboleth_SAML_App1" />

    <AttributeRule attributeID="uid">
        <PermitValueRule xsi:type="ANY" />
    </AttributeRule>

</AttributeFilterPolicy>
```

This means:

```text
IF requester =
SpringBoot_Shibboleth_SAML_App1

THEN

permit uid to be released
```

Notice that `attributeID="uid"` refers to the **internal Shibboleth attribute** created in step 1.

### 3. Shibboleth encodes `uid` into SAML

Your actual test shows Shibboleth sends it as:

```text
urn:oid:0.9.2342.19200300.100.1.1 = yunpeng.li
```

That's the SAML/OID representation of `uid`.

So:

```text
Shibboleth internal
uid = yunpeng.li
        ↓
SAML encoding
        ↓
urn:oid:0.9.2342.19200300.100.1.1
        =
yunpeng.li
```

This is why your Spring principal showed:

```json
"attributes": {
    "urn:oid:0.9.2342.19200300.100.1.1": [
        "yunpeng.li"
    ]
}
```

### 4. Retrieve it in Spring

Because Spring sees the SAML attribute by its encoded name, use:

```java
String username = principal.getFirstAttribute(
    "urn:oid:0.9.2342.19200300.100.1.1"
);

result.put("username", username);
```

I would make the code cleaner with a constant:

```java
private static final String ATTR_UID =
        "urn:oid:0.9.2342.19200300.100.1.1";
```

Then:

```java
String username =
        principal.getFirstAttribute(ATTR_UID);

result.put("username", username);
```

Your current controller already has the appropriate place for this mapping.

### 5. Don't use `principal.getName()` as the username

Your current controller also has:

```java
result.put("name", principal.getName());
```

In your configuration that produces:

```text
AAdzZWNyZXQx+xj0fKvrWmF3eTj...
```

That's the SAML Subject/NameID being used by your IdP. It is **not** the login username you want to display.

Therefore:

```java
principal.getName()
```

→ opaque SAML NameID

while:

```java
principal.getFirstAttribute(ATTR_UID)
```

→ `yunpeng.li`

### Final flow

```text
User authenticates
      │
      ▼
Principal = yunpeng.li
      │
      ▼
attribute-resolver.xml

<AttributeDefinition
    id="uid"
    xsi:type="PrincipalName" />

      │
      ▼
uid = yunpeng.li
      │
      ▼
attribute-filter.xml

Release uid to
SpringBoot_Shibboleth_SAML_App1

      │
      ▼
SAML encoding

urn:oid:0.9.2342.19200300.100.1.1
      =
yunpeng.li

      │
      ▼
Spring Security

principal.getFirstAttribute(ATTR_UID)

      │
      ▼
username = "yunpeng.li"
```

So the three key pieces to remember are: **resolver creates `uid` → filter allows `uid` to be released → Spring reads the SAML-encoded `uid` and displays it as `username`.**
