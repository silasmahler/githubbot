package de.dohack.githubbot.backend;

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
/*@EnableOAuth2Sso*/
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .oauth2Login();
    }

    /*@Override
    protected void configure(HttpSecurity http)
            throws Exception {

        http.antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/login**")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .formLogin().disable();
    }*/

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PrincipalExtractor githubPrincipalExtractor() {
        return new GithubPrincipalExtractor();
    }

    @Bean
    public AuthoritiesExtractor githubAuthoritiesExtractor() {
        return new GithubAuthoritiesExtractor();
    }
}
