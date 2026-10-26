package pe.acmdiproyec.logistica.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
	return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
	    throws Exception {
	http
		.authorizeHttpRequests(auth -> auth
			.requestMatchers("/", "/login", "/css/**", "/js/**", "/img/**", "/imagenes/**", "/images/**").permitAll()
			.requestMatchers("/usuarios/**", "/roles/**")
			.hasAnyRole("ADMIN")
			.requestMatchers("/movimientos/**")
			.hasAnyRole("ADMIN", "LOGISTICA")
			.requestMatchers("/dashboard/**", "/reportes/**", "/productos/**", "/inventario/**")
			.hasAnyRole("ADMIN", "LOGISTICA")
			.anyRequest().authenticated())
		.formLogin(form -> form
			.loginPage("/login")
			.loginProcessingUrl("/login")
			.defaultSuccessUrl("/panel", true)
			.failureUrl("/login?error=true")
			.permitAll())
		.logout(logout -> logout
			.logoutSuccessUrl("/login?logout=true")
			.permitAll());

	return http.build();
    }
}
