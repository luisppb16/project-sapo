/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.util;

import com.intellij.openapi.project.Project;
import com.projectsapo.model.OsvPackage;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to parse dependencies from the project.
 * Supports Gradle (via Tooling API) and Maven (via IntelliJ Maven Plugin).
 */
public class DependencyParser {

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
        
        // Check for Gradle
        if (new File(projectDir, "build.gradle").exists() || new File(projectDir, "build.gradle.kts").exists()) {
            dependencies.addAll(parseGradleDependencies(projectDir));
        }

        // Check for Maven
        if (MavenProjectsManager.getInstance(project).hasProjects()) {
            dependencies.addAll(parseMavenDependencies(project));
        }

        // Deduplicate and sort
        return dependencies.stream()
                .distinct()
                .sorted(Comparator.comparing(OsvPackage::name))
                .toList();
    }

    private static List<OsvPackage> parseGradleDependencies(File projectDir) {
        Set<OsvPackage> dependencies = new HashSet<>();
        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(projectDir);
        
        try (ProjectConnection connection = connector.connect()) {
            IdeaProject ideaProject = connection.getModel(IdeaProject.class);
            
            for (IdeaModule module : ideaProject.getModules()) {
                module.getDependencies().forEach(dependency -> {
                    if (dependency instanceof IdeaSingleEntryLibraryDependency lib) {
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
            System.err.println("Error parsing Gradle dependencies: " + e.getMessage());
        }
        
        return new ArrayList<>(dependencies);
    }

    private static List<OsvPackage> parseMavenDependencies(Project project) {
        Set<OsvPackage> dependencies = new HashSet<>();
        MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
        
        for (MavenProject mavenProject : projectsManager.getProjects()) {
            mavenProject.getDependencies().forEach(dep -> {
                String group = dep.getGroupId();
                String name = dep.getArtifactId();
                String version = dep.getVersion();
                
                if (group != null && name != null && version != null) {
                    dependencies.add(new OsvPackage(group + ":" + name, "Maven", version));
                }
            });
        }
        
        return new ArrayList<>(dependencies);
    }
}
