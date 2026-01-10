/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.projectsapo.model.OsvPackage;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse dependencies from the project.
 * Currently supports Gradle projects via Gradle Tooling API.
 */
public class DependencyParser {

    private static final Pattern GRADLE_DEPENDENCY_PATTERN = Pattern.compile("([\\w\\.-]+):([\\w\\.-]+):([\\w\\.-]+)");

    /**
     * Extracts dependencies from the given project.
     *
     * @param project The IntelliJ project.
     * @return A list of OsvPackage objects representing the dependencies.
     */
    public static List<OsvPackage> parseDependencies(Project project) {
        List<OsvPackage> dependencies = new ArrayList<>();
        String basePath = project.getBasePath();

        if (basePath == null) {
            return dependencies;
        }

        File projectDir = new File(basePath);
        // Check if it's a Gradle project
        if (new File(projectDir, "build.gradle").exists() || new File(projectDir, "build.gradle.kts").exists()) {
            dependencies.addAll(parseGradleDependencies(projectDir));
        }

        // Future: Add Maven support here

        return dependencies;
    }

    private static List<OsvPackage> parseGradleDependencies(File projectDir) {
        List<OsvPackage> dependencies = new ArrayList<>();
        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(projectDir);
        
        try (ProjectConnection connection = connector.connect()) {
            IdeaProject ideaProject = connection.getModel(IdeaProject.class);
            
            for (IdeaModule module : ideaProject.getModules()) {
                module.getDependencies().forEach(dependency -> {
                    if (dependency instanceof IdeaSingleEntryLibraryDependency lib) {
                        // The Gradle Tooling API often returns file paths or coordinates.
                        // We try to extract coordinates from the Gradle coordinate if available,
                        // or fallback to parsing the file name/path if necessary, though coordinates are preferred.
                        
                        // Note: IdeaSingleEntryLibraryDependency.getGradleModuleVersion() is often null for local jars
                        // but usually populated for external dependencies.
                        var gradleModuleVersion = lib.getGradleModuleVersion();
                        
                        if (gradleModuleVersion != null) {
                            String group = gradleModuleVersion.getGroup();
                            String name = gradleModuleVersion.getName();
                            String version = gradleModuleVersion.getVersion();
                            
                            if (group != null && name != null && version != null) {
                                dependencies.add(new OsvPackage(group + ":" + name, "Maven", version));
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            // Log error or handle gracefully
            // For now, we just return what we found
            System.err.println("Error parsing Gradle dependencies: " + e.getMessage());
        }
        
        return dependencies;
    }
}
