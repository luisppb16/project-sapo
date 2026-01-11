/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.client;

import com.projectsapo.model.OsvResponse;
import com.projectsapo.model.OsvVulnerability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OsvClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private OsvClient osvClient;

    @BeforeEach
    void setUp() {
        osvClient = new OsvClient(Executors.newVirtualThreadPerTaskExecutor(), httpClient);
    }

    @Test
    void testCheckDependencyFound() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        // Arrange
        String jsonResponse = """
            {
                "vulns": [
                    {
                        "id": "GHSA-jfh8-c2jp-5v3q",
                        "summary": "Vulnerability in Log4j",
                        "details": "Details here"
                    }
                ]
            }
            """;

        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);

        // Act
        CompletableFuture<Optional<OsvResponse>> future = osvClient.checkDependency("org.apache.logging.log4j:log4j-core", "2.14.1", "Maven");
        Optional<OsvResponse> result = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(result.isPresent());
        assertNotNull(result.get().vulns());
        assertEquals(1, result.get().vulns().size());
        assertEquals("GHSA-jfh8-c2jp-5v3q", result.get().vulns().get(0).id());

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testCheckDependencyNotFound() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        // Arrange
        String jsonResponse = "{}";

        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);

        // Act
        CompletableFuture<Optional<OsvResponse>> future = osvClient.checkDependency("org.slf4j:slf4j-api", "2.0.12", "Maven");
        Optional<OsvResponse> result = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(result.isEmpty()); // Empty optional for no vulns

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testCheckDependencyError() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        // Arrange
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        when(httpResponse.statusCode()).thenReturn(500);

        // Act
        CompletableFuture<Optional<OsvResponse>> future = osvClient.checkDependency("org.example:error", "1.0.0", "Maven");
        Optional<OsvResponse> result = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(result.isEmpty());
    }
}
