package com.projectsapo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;

/**
 * Represents the package information to be queried in the OSV API.
 *
 * @param name      The name of the package (e.g., "org.slf4j:slf4j-api").
 * @param ecosystem The ecosystem of the package (e.g., "Maven").
 * @param version   The specific version of the package.
 */
@Builder
@Jacksonized
public record OsvPackage(
        @JsonProperty("name") String name,
        @JsonProperty("ecosystem") String ecosystem,
        @JsonProperty("version") String version
) {

    public OsvPackage {
        Objects.requireNonNull(name, "Package name cannot be null");
        Objects.requireNonNull(ecosystem, "Ecosystem cannot be null");
    }
}
