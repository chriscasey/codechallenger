package com.chriscasey.codechallenger.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import com.chriscasey.codechallenger.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        SecurityContextHolder.clearContext(); // ensure no authenticated user
        mvc.perform(get("/api/challenges")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withUser_returns200() throws Exception {
        // Setup your actual domain User as the principal for this request
        User user = new User();
        user.setId(99L);
        user.setEmail("securitytest@example.com");
        // Set additional required fields if needed

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, "password", authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        mvc.perform(get("/api/challenges")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}