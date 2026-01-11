/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectsapo.model.OsvPackage;
import com.projectsapo.model.OsvQuery;
import com.projectsapo.model.OsvResponse;
import com.projectsapo.util.ProjectConstants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for interacting with the OSV.dev API to check for vulnerabilities.
 * Uses Java 21 Virtual Threads for asynchronous execution.
 */
public class OsvClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public OsvClient() {
        this(Executors.newVirtualThreadPerTaskExecutor());
    }

    public OsvClient(ExecutorService executorService) {
        this(executorService, HttpClient.newBuilder()
                .executor(executorService)
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public OsvClient(ExecutorService executorService, HttpClient httpClient) {
        this.executorService = executorService;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<Optional<OsvResponse>> checkDependency(String packageName, String version, String ecosystem) {
        Objects.requireNonNull(packageName, "Package name cannot be null");
        Objects.requireNonNull(version, "Version cannot be null");
        Objects.requireNonNull(ecosystem, "Ecosystem cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            try {
                OsvPackage osvPackage = new OsvPackage(packageName, ecosystem, version);
                OsvQuery query = new OsvQuery(version, osvPackage);

                String requestBody = objectMapper.writeValueAsString(query);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ProjectConstants.OSV_API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    if (response.body().equals("{}")) {
                         return Optional.empty();
                    }

                    OsvResponse osvResponse = objectMapper.readValue(response.body(), OsvResponse.class);
                    return Optional.of(osvResponse);
                } else {
                    return Optional.empty();
                }

            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return Optional.empty();
            }
        }, executorService);
    }
}
