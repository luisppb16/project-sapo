/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Represents a query to the OSV API.
 *
 * @param version The version of the package.
 * @param packageInfo The package details.
 */
public record OsvQuery(
        String version,
        @JsonProperty("package") OsvPackage packageInfo
) {

    public OsvQuery {
        Objects.requireNonNull(packageInfo, "Package info cannot be null");
    }
}
