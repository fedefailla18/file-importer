package com.importer.fileimporter.config.security;

public class WebSecurityConfigurerAdapterImpl {
//        extends WebSecurityConfigurerAdapter {
//
////    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.csrf().disable()
//                .authorizeRequests()
//                .antMatchers("/**").permitAll() // Allow all requests without authentication
//                .antMatchers("/hello").permitAll() // Allow all requests without authentication
//                .anyRequest().authenticated();
//                .antMatchers(HttpMethod.POST, "/authenticate").permitAll() // Allow login endpoint
//                .antMatchers(HttpMethod.POST, "/hello").permitAll() // Allow login endpoint
//                .anyRequest().authenticated();
//                .and()
//                .addFilter(new JwtAuthenticationFilter(authenticationManager())); // Configure JWT filter
//    }

    // You can customize the authentication manager if needed

//    @Bean
//    public JwtDecoder jwtDecoder() {
//        // Configure the JWT decoder with the necessary settings (algorithm, signing key, etc.)
//        return NimbusJwtDecoder.withJwkSetUri("https://example.com/jwks_uri").build();
//    }
//
//    @Bean
//    public JwtAuthenticationProvider jwtAuthenticationProvider(JwtDecoder jwtDecoder) {
//        return new JwtAuthenticationProvider(jwtDecoder);
//    }
}
