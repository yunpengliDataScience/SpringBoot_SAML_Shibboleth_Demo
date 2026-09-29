package org.dragon.yunpeng.samldemo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers( "/error", "/saml2/metadata", "/saml2/metadata/**",
						"/saml2/service-provider-metadata/**")
				.permitAll()

				.anyRequest().authenticated())

				// SAML Login
				.saml2Login(withDefaults())

				// IMPORTANT:
				// Registers Spring's SP metadata endpoint
				.saml2Metadata(withDefaults())

				.logout(logout -> logout.logoutSuccessUrl("/"));

		return http.build();
	}
}