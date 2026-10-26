package pe.acmdiproyec.logistica.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
			.requestMatchers(HttpMethod.POST, "/categorias/**", "/unidades-medida/**", "/productos/**", "/movimientos/**")
			.hasAnyRole("ADMIN")
			.requestMatchers("/categorias/nuevo", "/categorias/*/editar",
					"/unidades-medida/nuevo", "/unidades-medida/*/editar",
					"/productos/nuevo", "/productos/*/editar",
					"/movimientos/nuevo")
			.hasAnyRole("ADMIN")
			.requestMatchers("/categorias/**", "/unidades-medida/**", "/productos/**", "/movimientos/**")
			.hasAnyRole("ADMIN", "LOGISTICA")
			.requestMatchers("/usuarios/**", "/roles/**")
			.hasAnyRole("ADMIN")
			.requestMatchers("/dashboard/**", "/reportes/**", "/inventario/**")
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
