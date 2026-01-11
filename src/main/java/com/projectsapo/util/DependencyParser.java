/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.libraries.Library;
import com.projectsapo.model.OsvPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse dependencies from the project using IntelliJ's internal model.
 * This is more reliable than calling Gradle/Maven externally.
 */
public final class DependencyParser {

    private static final Pattern GRADLE_LIB_PATTERN = Pattern.compile("^Gradle: (.+):(.+):(.+)$");

    private DependencyParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Extracts dependencies from the given project.
     *
     * @param project The IntelliJ project.
     * @return A list of OsvPackage objects representing the dependencies.
     */
    public static List<OsvPackage> parseDependencies(@NotNull Project project) {
        Set<OsvPackage> dependencies = new HashSet<>();

        // 1. Try Maven-specific API for better accuracy if it's a Maven project
        if (MavenProjectsManager.getInstance(project).hasProjects()) {
            dependencies.addAll(parseMavenDependencies(project));
        }

        // 2. Use OrderEnumerator to catch everything else (Gradle, manual Jars, etc.)
        // This acts as a fallback and primary source for Gradle projects
        OrderEnumerator.orderEntries(project).librariesOnly().forEachLibrary(library -> {
            OsvPackage pkg = parseLibrary(library);
            if (pkg != null) {
                dependencies.add(pkg);
            }
            return true;
        });

        return dependencies.stream()
                .sorted(Comparator.comparing(OsvPackage::name))
                .toList();
    }

    private static OsvPackage parseLibrary(Library library) {
        String name = library.getName();
        if (name == null) return null;

        // Handle Gradle format: "Gradle: group:artifact:version"
        Matcher matcher = GRADLE_LIB_PATTERN.matcher(name);
        if (matcher.matches()) {
            String group = matcher.group(1);
            String artifact = matcher.group(2);
            String version = matcher.group(3);
            return new OsvPackage(group + ":" + artifact, "Maven", version);
        }

        // Fallback for other formats or generic libraries
        // Many plugins use "group:artifact:version" directly
        String[] parts = name.split(":");
        if (parts.length == 3) {
            return new OsvPackage(parts[0] + ":" + parts[1], "Maven", parts[2]);
        }

        return null;
    }

    private static List<OsvPackage> parseMavenDependencies(Project project) {
        Set<OsvPackage> dependencies = new HashSet<>();
        MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
        
        for (MavenProject mavenProject : projectsManager.getProjects()) {
            mavenProject.getDependencies().forEach(dep -> {
                String group = dep.getGroupId();
                String artifact = dep.getArtifactId();
                String version = dep.getVersion();
                
                if (group != null && artifact != null && version != null) {
                    dependencies.add(new OsvPackage(group + ":" + artifact, "Maven", version));
                }
            });
        }
        
        return new ArrayList<>(dependencies);
    }
}
