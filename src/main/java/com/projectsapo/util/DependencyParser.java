package com.projectsapo.util;

import com.intellij.openapi.project.Project;
import com.projectsapo.model.OsvPackage;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        // Deduplicate and sort
        return dependencies.stream()
                .distinct() // OsvPackage is a record, so equals/hashCode are based on fields
                .sorted(Comparator.comparing(OsvPackage::name))
                .toList();
    }

    private static List<OsvPackage> parseGradleDependencies(File projectDir) {
        // Use a Set to avoid duplicates during collection if possible, though stream distinct handles it later
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
}
