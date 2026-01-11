/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.model;

import java.util.Objects;

/**
 * Represents the package information to be queried in the OSV API.
 *
 * @param name      The name of the package (e.g., "org.slf4j:slf4j-api").
 * @param ecosystem The ecosystem of the package (e.g., "Maven").
 * @param version   The specific version of the package.
 */
public record OsvPackage(
        String name,
        String ecosystem,
        String version
) {

    public OsvPackage {
        Objects.requireNonNull(name, "Package name cannot be null");
        Objects.requireNonNull(ecosystem, "Ecosystem cannot be null");
    }
}
