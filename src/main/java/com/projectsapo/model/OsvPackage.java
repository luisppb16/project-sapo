/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

/**
 * Represents the package information. Includes the dependency chain to show how a transitive
 * dependency reached the project.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsvPackage(
    String name, String ecosystem, String version, List<String> dependencyChain) {
  public OsvPackage {
    Objects.requireNonNull(name, "Package name cannot be null");
    Objects.requireNonNull(ecosystem, "Ecosystem cannot be null");
    dependencyChain = dependencyChain != null ? List.copyOf(dependencyChain) : List.of();
  }

  // Constructor de conveniencia para compatibilidad
  public OsvPackage(String name, String ecosystem, String version) {
    this(name, ecosystem, version, List.of());
  }
}
