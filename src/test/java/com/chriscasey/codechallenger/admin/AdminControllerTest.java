package com.chriscasey.codechallenger.admin;

import com.chriscasey.codechallenger.challenge.*;
import com.chriscasey.codechallenger.auth.User;
import com.chriscasey.codechallenger.auth.UserRepository;
import com.chriscasey.codechallenger.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false) // disables security for controller test
class AdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.chriscasey.codechallenger.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CodeChallengeRepository challengeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllUsersWithChallenges_paginated_and_sorted() throws Exception {
        User u1 = new User();
        u1.setId(1L);
        u1.setEmail("alice@example.com");

        User u2 = new User();
        u2.setId(2L);
        u2.setEmail("bob@example.com");

        Page<User> page = new PageImpl<>(List.of(u1, u2),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")), 2);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        CodeChallenge c1 = CodeChallenge.builder()
                .id(1001L)
                .user(u1)
                .title("FizzBuzz")
                .description("Implement FizzBuzz")
                .solution(123)
                .difficulty(1)
                .failedAttempts(0)
                .status(ChallengeStatus.PENDING)
                .build();

        CodeChallenge c2 = CodeChallenge.builder()
                .id(1002L)
                .user(u2)
                .title("Palindrome")
                .description("Check palindrome")
                .solution(456)
                .difficulty(2)
                .failedAttempts(1)
                .status(ChallengeStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();

        when(challengeRepository.findByUserIn(any())).thenReturn(List.of(c1, c2));

        var result = mvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "id")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.sortBy").value("id"))
                .andExpect(jsonPath("$.sortDir").value("desc"))
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        // Useful for seeing the response if anything fails
        System.out.println("RESPONSE: " + result.getResponse().getContentAsString());
    }

}
