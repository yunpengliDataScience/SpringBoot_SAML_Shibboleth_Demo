package org.dragon.yunpeng.samldemo.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

	/*
	 * Returns the Thymeleaf template:
	 *
	 * src/main/resources/templates/home.html
	 *
	 * Do NOT add @ResponseBody here. Otherwise Spring will return the literal text
	 * "home".
	 */
	@GetMapping("/")
	public String home() {
		return "home";
	}

	/*
	 * Simple protected endpoint. Shows basic Spring Security authentication
	 * information.
	 */
	@GetMapping("/private")
	@ResponseBody
	public Map<String, Object> privatePage(Authentication authentication) {

		Map<String, Object> result = new LinkedHashMap<>();

		result.put("message", "Shibboleth SAML authentication succeeded.");

		result.put("name", authentication.getName());

		result.put("authenticationType", authentication.getClass().getName());

		result.put("authorities", authentication.getAuthorities());

		return result;
	}

	/*
	 * Return the authenticated Shibboleth SAML user's information.
	 */
	@GetMapping("/user")
	@ResponseBody
	public Map<String, Object> user(Authentication authentication) {

		Map<String, Object> result = new LinkedHashMap<>();

		if (!(authentication instanceof Saml2Authentication samlAuthentication)) {

			result.put("message", "Current authentication is not a Saml2Authentication.");

			return result;
		}

		Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) samlAuthentication.getPrincipal();

		/*
		 * Usually derived from the SAML Subject/NameID.
		 */
		//result.put("name", principal.getName());

		/*
		 * Should be "shibboleth" for our current application.yml configuration.
		 */
		result.put("registrationId", principal.getRelyingPartyRegistrationId());

		//the SAML/OID representation of uid
		String username = principal.getFirstAttribute("urn:oid:0.9.2342.19200300.100.1.1");

		result.put("username", username);
		
		/*
		 * Spring Security authorities.
		 */
		result.put("authorities", samlAuthentication.getAuthorities());

		/*
		 * All SAML attributes released by Shibboleth.
		 *
		 * This is especially useful while configuring Shibboleth because we can see
		 * exactly what attributes the IdP sent.
		 */
		result.put("attributes", principal.getAttributes());

		/*
		 * Shibboleth SAML session indexes.
		 */
		result.put("sessionIndexes", principal.getSessionIndexes());

		/*
		 * Shibboleth commonly uses standard attribute names such as mail, givenName,
		 * and sn.
		 *
		 * These may be null until we configure attribute resolution/release on the
		 * Shibboleth IdP.
		 */
		result.put("email", principal.getFirstAttribute("mail"));

		result.put("firstName", principal.getFirstAttribute("givenName"));

		result.put("lastName", principal.getFirstAttribute("sn"));

		return result;
	}
}