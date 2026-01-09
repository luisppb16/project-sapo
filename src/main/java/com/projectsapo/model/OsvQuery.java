package com.projectsapo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;

/**
 * Represents a query to the OSV API.
 *
 * @param version The version of the package.
 * @param packageInfo The package details.
 */
@Builder
@Jacksonized
public record OsvQuery(
        @JsonProperty("version") String version,
        @JsonProperty("package") OsvPackage packageInfo
) {

    public OsvQuery {
        Objects.requireNonNull(packageInfo, "Package info cannot be null");
    }
}
