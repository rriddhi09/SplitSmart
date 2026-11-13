package com.splitsmart.security;

import com.splitsmart.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // public API
                .requestMatchers("/users/register").permitAll()

                // protected APIs
                .requestMatchers("/users/**").authenticated()
                .requestMatchers("/groups/**").authenticated()
                .requestMatchers("/expenses/**").authenticated()

                .anyRequest().authenticated()
            )
              //  .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults()); //

        return http.build();
    }

    //InMemory multiple users using userdetailsmanager
//    @Bean
//    public UserDetailsService userDetailsService(){
//        UserDetails user=User.withUsername("user").password("{noop}1234").authorities("read").build();
//        UserDetails admin=User.withUsername("admin").password("{bcrypt}$2a$12$8hTF7DyDbWTDjAOjB8AGH.ilDM1AkTydo9mj7ZSZYDAhbShLeItyO").authorities("admin").build();
//        return new InMemoryUserDetailsManager(user,admin);
//    }

     //Multiple user in database
//    @Bean
//    public UserDetailsService userDetailsService(DataSource dataSource){
//        return new JdbcUserDetailsManager(dataSource);
//    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /*
    //only strong password is allowed
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker{
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }
    */
}