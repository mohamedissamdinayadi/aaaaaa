package com.squeezer.oauth2.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import javax.sql.DataSource;

/**
 * Configures the authorization server.
 * The @EnableAuthorizationServer annotation is used to configure the OAuth 2.0 Authorization Server mechanism,
 * together with any @Beans that implement AuthorizationServerConfigurer (there is a handy adapter implementation with empty methods).
 */
@Configuration
@EnableAuthorizationServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {


    private static final String TRUSTED_CLIENT_ID = "squeezer";
    private static final String TRUSTED_CLIENT_PASSWORD = "squeezer";

    private final DataSource dataSource;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AuthorizationServerConfig(@Qualifier("dataSource") DataSource dataSource, AuthenticationManager authenticationManager, @Lazy @Qualifier("userDetailsService") UserDetailsService userDetailsService) {
        this.dataSource = dataSource;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("10000")
    private int tokenExpiration;

    // 1 day for refresh token expiration
    @Value("86400")
    private int refreshTokenExpiration;


    @Bean
    public TokenStore tokenStore() {
        return new JdbcTokenStore(dataSource);
    }

    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setSupportRefreshToken(true);
        return defaultTokenServices;
    }

    /**
     * Setting up the endpointsConfigurer authentication manager.
     * The AuthorizationServerEndpointsConfigurer defines the authorization and token endpoints and the token services.
     *
     * @param endpoints endpoints of WBS
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints.authenticationManager(authenticationManager);
        endpoints.tokenStore(tokenStore());
        endpoints.userDetailsService(userDetailsService);
        //Change the URL of login endpoint
        endpoints.pathMapping("/oauth/token", "/login");
    }

    /**
     * Setting up the clients with a clientId, a clientSecret, a scope, the grant types and the authorities.
     *
     * @param clients The oAuth2 client
     * @throws Exception throw exceptions
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients
                .inMemory()
                .withClient(TRUSTED_CLIENT_ID)
                .secret(passwordEncoder().encode(TRUSTED_CLIENT_PASSWORD))
                // grant type
                .authorizedGrantTypes("refresh_token", "password")
                // for specify authority of a client
//                .authorities("ROLE_CLIENT","ROLE_TRUSTED_CLIENT")
                // expire time for refresh token
                .refreshTokenValiditySeconds(refreshTokenExpiration)
                // scope related to resource server
                .scopes("read", "write")
                // id for resource client
//                .resourceIds("oauth2-resource")
                // expire time for access token in seconds
                .accessTokenValiditySeconds(tokenExpiration);
    }

    /**
     * We here defines the security constraints on the token endpoint.
     * We set it up to isAuthenticated, which returns true if the user is not anonymous
     *
     * @param oauthServer the AuthorizationServerSecurityConfigurer.
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
//        oauthServer.tokenKeyAccess("permitAll()");
        oauthServer.checkTokenAccess("isAuthenticated()");
        oauthServer.allowFormAuthenticationForClients();
    }


}
