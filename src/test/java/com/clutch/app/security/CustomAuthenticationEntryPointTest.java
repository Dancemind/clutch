package com.clutch.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import com.clutch.app.config.CustomAuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationEntryPointTest {

    private CustomAuthenticationEntryPoint entryPoint;

    @Mock
    private HttpServletRequest request;

    private MockHttpServletResponse response;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        entryPoint = new CustomAuthenticationEntryPoint();
        response = new MockHttpServletResponse();
    }

    @Test
    void commence_shouldReturnJsonErrorResponseWithUnauthorizedStatus() throws IOException {
        AuthenticationException authException = mock(AuthenticationException.class);


        entryPoint.commence(request, response, authException);


        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

        String jsonContent = response.getContentAsString();
        assertNotNull(jsonContent);
        assertFalse(jsonContent.isEmpty());

        Map<?, ?> jsonMap = objectMapper.readValue(jsonContent, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), jsonMap.get("status"));
        assertEquals(HttpStatus.UNAUTHORIZED.name(), jsonMap.get("error"));
        assertEquals("Authentication is required to access this resource", jsonMap.get("message"));

        assertNotNull(jsonMap.get("timestamp"));
        assertFalse(jsonMap.get("timestamp").toString().isEmpty());
    }
}
