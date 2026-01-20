/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.util;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.libraries.Library;
import com.projectsapo.model.OsvPackage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * Utility class to parse dependencies and their transitive chains. Fixed to avoid including the
 * project name as a parent dependency.
 */
public final class DependencyParser {

  private static final Pattern GRADLE_LIB_PATTERN = Pattern.compile("^Gradle: (.+):(.+):(.+)$");

  private DependencyParser() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static List<OsvPackage> parseDependencies(@NotNull Project project) {
    List<OsvPackage> allPackages = new ArrayList<>();

    // 1. Maven: Real tree support
    MavenProjectsManager mavenManager = MavenProjectsManager.getInstance(project);
    if (mavenManager != null && mavenManager.hasProjects()) {
      allPackages.addAll(parseMavenDependencies(project));
    }

    // 2. Gradle: External System support
    allPackages.addAll(parseGradleDependencies(project));

    // 3. Fallback: OrderEnumerator
    OrderEnumerator.orderEntries(project)
        .librariesOnly()
        .forEachLibrary(
            library -> {
              OsvPackage pkg = parseLibrary(library);
              if (pkg != null) {
                allPackages.add(pkg);
              }
              return true;
            });

    // Aggregation: Deduplicate based on name, version, ecosystem (Optimized with Record key)
    Map<PackageKey, List<OsvPackage>> grouped =
        allPackages.stream()
            .collect(
                Collectors.groupingBy(
                    pkg -> new PackageKey(pkg.name(), pkg.version(), pkg.ecosystem())));

    return grouped.values().stream()
        .map(
            group -> {
              OsvPackage representative = group.get(0);
              Set<List<String>> allChains =
                  group.stream()
                      .map(OsvPackage::dependencyChains)
                      .flatMap(Collection::stream)
                      .collect(Collectors.toSet());
              return new OsvPackage(
                  representative.name(),
                  representative.ecosystem(),
                  representative.version(),
                  allChains);
            })
        .sorted(Comparator.comparing(OsvPackage::name))
        .toList();
  }

  private static List<OsvPackage> parseMavenDependencies(Project project) {
    List<OsvPackage> dependencies = new ArrayList<>();
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
    if (projectsManager == null) return dependencies;

    for (MavenProject mavenProject : projectsManager.getProjects()) {
      for (MavenArtifactNode node : mavenProject.getDependencyTree()) {
        // Start chain with the direct dependency itself
        collectMavenDependencies(node, new ArrayList<>(), dependencies);
      }
    }
    return dependencies;
  }

  private static void collectMavenDependencies(
      MavenArtifactNode node, List<String> parentChain, List<OsvPackage> result) {
    MavenArtifact artifact = node.getArtifact();
    String fullName = artifact.getGroupId() + ":" + artifact.getArtifactId();

    List<String> currentChain = new ArrayList<>(parentChain);
    currentChain.add(fullName);

    result.add(new OsvPackage(fullName, "Maven", artifact.getVersion(), Set.of(currentChain)));

    for (MavenArtifactNode child : node.getDependencies()) {
      collectMavenDependencies(child, currentChain, result);
    }
  }

  private static List<OsvPackage> parseGradleDependencies(Project project) {
    List<OsvPackage> dependencies = new ArrayList<>();
    Collection<ExternalProjectInfo> projectInfos =
        ProjectDataManager.getInstance()
            .getExternalProjectsData(project, GradleConstants.SYSTEM_ID);

    for (ExternalProjectInfo info : projectInfos) {
      DataNode<ProjectData> projectNode = info.getExternalProjectStructure();
      if (projectNode == null) continue;

      Collection<DataNode<LibraryData>> libraryNodes =
          ExternalSystemApiUtil.findAll(projectNode, ProjectKeys.LIBRARY);
      for (DataNode<LibraryData> libNode : libraryNodes) {
        LibraryData data = libNode.getData();
        String group = data.getGroupId();
        String artifact = data.getArtifactId();
        String version = data.getVersion();

        if (group != null && artifact != null && version != null) {
          String fullName = group + ":" + artifact;
          // For Gradle, we treat it as direct if we can't easily find the parent in this flat list
          dependencies.add(
              new OsvPackage(fullName, "Maven", version, Set.of(List.of(fullName))));
        }
      }
    }
    return dependencies;
  }

  private static OsvPackage parseLibrary(Library library) {
    String name = library.getName();
    if (name == null) return null;
    Matcher matcher = GRADLE_LIB_PATTERN.matcher(name);
    if (matcher.matches()) {
      String fullName = matcher.group(1) + ":" + matcher.group(2);
      return new OsvPackage(
          fullName, "Maven", matcher.group(3), Set.of(List.of(fullName)));
    }
    String[] parts = name.split(":");
    if (parts.length == 3) {
      String fullName = parts[0] + ":" + parts[1];
      return new OsvPackage(
          fullName, "Maven", parts[2], Set.of(List.of(fullName)));
    }
    return null;
  }

}
