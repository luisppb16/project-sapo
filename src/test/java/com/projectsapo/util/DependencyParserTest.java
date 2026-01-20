/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootsEnumerator;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.util.Processor;
import com.projectsapo.model.OsvPackage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DependencyParserTest {

  @Mock private Project project;
  @Mock private MavenProjectsManager mavenProjectsManager;
  @Mock private ProjectDataManager projectDataManager;
  @Mock private MavenProject mavenProject;
  @Mock private MavenArtifactNode mavenArtifactNode;
  @Mock private MavenArtifact mavenArtifact;
  @Mock private OrderEnumerator orderEnumerator;
  @Mock private OrderRootsEnumerator orderRootsEnumerator;
  @Mock private Application application;
  @Mock private ModuleManager moduleManager;
  @Mock private Module module;
  @Mock private LibraryTablesRegistrar libraryTablesRegistrar;
  @Mock private LibraryTable libraryTable;

  private MockedStatic<MavenProjectsManager> mavenProjectsManagerMock;
  private MockedStatic<ProjectDataManager> projectDataManagerMock;
  private MockedStatic<OrderEnumerator> orderEnumeratorMock;
  private MockedStatic<ApplicationManager> applicationManagerMock;
  private MockedStatic<ModuleManager> moduleManagerMock;
  private MockedStatic<LibraryTablesRegistrar> libraryTablesRegistrarMock;

  @BeforeEach
  void setUp() {
    mavenProjectsManagerMock = mockStatic(MavenProjectsManager.class);
    projectDataManagerMock = mockStatic(ProjectDataManager.class);
    orderEnumeratorMock = mockStatic(OrderEnumerator.class);
    applicationManagerMock = mockStatic(ApplicationManager.class);
    moduleManagerMock = mockStatic(ModuleManager.class);
    libraryTablesRegistrarMock = mockStatic(LibraryTablesRegistrar.class);

    mavenProjectsManagerMock
        .when(() -> MavenProjectsManager.getInstance(project))
        .thenReturn(mavenProjectsManager);
    projectDataManagerMock
        .when(ProjectDataManager::getInstance)
        .thenReturn(projectDataManager);
    orderEnumeratorMock
        .when(() -> OrderEnumerator.orderEntries(project))
        .thenReturn(orderEnumerator);
    orderEnumeratorMock
        .when(() -> OrderEnumerator.orderEntries(any(Module.class)))
        .thenReturn(orderEnumerator);
    applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(application);
    moduleManagerMock.when(() -> ModuleManager.getInstance(project)).thenReturn(moduleManager);
    libraryTablesRegistrarMock.when(LibraryTablesRegistrar::getInstance).thenReturn(libraryTablesRegistrar);

    when(moduleManager.getModules()).thenReturn(new Module[]{module});
    when(libraryTablesRegistrar.getLibraryTable(project)).thenReturn(libraryTable);
    when(libraryTable.getLibraries()).thenReturn(new Library[0]);
    
    // Default behavior for OrderEnumerator to avoid NPEs in tests that don't specifically mock it
    when(orderEnumerator.librariesOnly()).thenReturn(orderEnumerator);
    when(orderEnumerator.recursively()).thenReturn(orderEnumerator);
  }

  @AfterEach
  void tearDown() {
    mavenProjectsManagerMock.close();
    projectDataManagerMock.close();
    orderEnumeratorMock.close();
    applicationManagerMock.close();
    moduleManagerMock.close();
    libraryTablesRegistrarMock.close();
  }

  @Test
  void testParseDependencies_Maven() {
    // Arrange
    when(mavenProjectsManager.hasProjects()).thenReturn(true);
    when(mavenProjectsManager.getProjects()).thenReturn(List.of(mavenProject));
    when(mavenProject.getDependencyTree()).thenReturn(List.of(mavenArtifactNode));
    when(mavenArtifactNode.getArtifact()).thenReturn(mavenArtifact);
    when(mavenArtifact.getGroupId()).thenReturn("com.example");
    when(mavenArtifact.getArtifactId()).thenReturn("lib");
    when(mavenArtifact.getVersion()).thenReturn("1.0.0");
    when(mavenArtifactNode.getDependencies()).thenReturn(new ArrayList<>());

    // Act
    List<OsvPackage> result = DependencyParser.parseDependencies(project);

    // Assert
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    OsvPackage pkg = result.get(0);
    assertEquals("com.example:lib", pkg.name());
    assertEquals("1.0.0", pkg.version());
    assertEquals("Maven", pkg.ecosystem());
    // Verify it has chains
    assertNotNull(pkg.dependencyChains());
    assertFalse(pkg.dependencyChains().isEmpty());
  }

  @Test
  void testParseDependencies_Gradle_SkipFallback() {
    // Arrange
    when(mavenProjectsManager.hasProjects()).thenReturn(false);
    ExternalProjectInfo projectInfo = mock(ExternalProjectInfo.class);
    when(projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID))
        .thenReturn(List.of(projectInfo));

    // Fallback mocks (should NOT be called)
    Library library = mock(Library.class);
    when(library.getName()).thenReturn("Gradle: com.example:lib:1.0.0");
    doAnswer(invocation -> {
      Processor<Library> processor = invocation.getArgument(0);
      processor.process(library);
      return null;
    }).when(orderEnumerator).forEachLibrary(any());

    // Act
    List<OsvPackage> result = DependencyParser.parseDependencies(project);

    // Assert
    // Should be empty because we didn't mock Gradle structure and we skipped fallback
    assertTrue(result.isEmpty());

    // Verify OrderEnumerator was NOT called
    verify(orderEnumerator, never()).forEachLibrary(any());
  }

  @Test
  void testParseDependencies_Gradle_Parsing() {
    try (MockedStatic<com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil> externalSystemApiUtilMock =
             mockStatic(com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.class)) {

      when(mavenProjectsManager.hasProjects()).thenReturn(false);

      ExternalProjectInfo projectInfo = mock(ExternalProjectInfo.class);
      DataNode<ProjectData> projectNode = mock(DataNode.class);
      when(projectInfo.getExternalProjectStructure()).thenReturn(projectNode);

      when(projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID))
          .thenReturn(List.of(projectInfo));

      DataNode<LibraryData> libNode = mock(DataNode.class);
      LibraryData libData = mock(LibraryData.class);
      when(libNode.getData()).thenReturn(libData);
      when(libData.getGroupId()).thenReturn("com.example");
      when(libData.getArtifactId()).thenReturn("lib");
      when(libData.getVersion()).thenReturn("1.0.0");

      externalSystemApiUtilMock.when(() -> com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findAll(projectNode, ProjectKeys.LIBRARY))
          .thenReturn(List.of(libNode));

      List<OsvPackage> result = DependencyParser.parseDependencies(project);

      assertFalse(result.isEmpty());
      assertEquals("com.example:lib", result.get(0).name());
    }
  }

  @Test
  void testParseDependencies_NoManagers_RunsFallback() {
    // Arrange
    when(mavenProjectsManager.hasProjects()).thenReturn(false);
    when(projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID))
        .thenReturn(Collections.emptyList());

    Library library = mock(Library.class);
    when(library.getName()).thenReturn("com.example:manual:1.0.0");
    doAnswer(invocation -> {
      Processor<Library> processor = invocation.getArgument(0);
      processor.process(library);
      return null;
    }).when(orderEnumerator).forEachLibrary(any());

    // Act
    List<OsvPackage> result = DependencyParser.parseDependencies(project);

    // Assert
    assertFalse(result.isEmpty());
    assertEquals("com.example:manual", result.get(0).name());
    verify(orderEnumerator, times(1)).forEachLibrary(any());
  }
  
  @Test
  void testParseDependencies_Fallback_StandardFormat() {
      // Arrange
      when(mavenProjectsManager.hasProjects()).thenReturn(false);
      when(projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID))
          .thenReturn(Collections.emptyList());

      Library library = mock(Library.class);
      when(library.getName()).thenReturn("com.example:lib:1.0.0");

      doAnswer(invocation -> {
          Processor<Library> processor = invocation.getArgument(0);
          processor.process(library);
          return null;
      }).when(orderEnumerator).forEachLibrary(any());

      // Act
      List<OsvPackage> result = DependencyParser.parseDependencies(project);

      // Assert
      assertFalse(result.isEmpty());
      assertEquals("com.example:lib", result.get(0).name());
      assertEquals("1.0.0", result.get(0).version());
  }

  @Test
  void testParseDependencies_MavenManagerNull() {
      // Arrange
      // Simulate MavenProjectsManager.getInstance returning null
      mavenProjectsManagerMock
          .when(() -> MavenProjectsManager.getInstance(project))
          .thenReturn(null);
      
      when(projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID))
          .thenReturn(Collections.emptyList());

      // Act
      List<OsvPackage> result = DependencyParser.parseDependencies(project);

      // Assert
      assertTrue(result.isEmpty());
  }

  @Test
  void testParseDependencies_Deduplication() {
    // Arrange
    // We simulate two sources (e.g. Maven) providing the same dependency but different paths
    when(mavenProjectsManager.hasProjects()).thenReturn(true);
    when(mavenProjectsManager.getProjects()).thenReturn(List.of(mavenProject));

    // Create two artifact nodes representing the same artifact but different locations
    MavenArtifactNode node1 = mock(MavenArtifactNode.class);
    MavenArtifact artifact1 = mock(MavenArtifact.class);
    when(node1.getArtifact()).thenReturn(artifact1);
    when(artifact1.getGroupId()).thenReturn("com.example");
    when(artifact1.getArtifactId()).thenReturn("common");
    when(artifact1.getVersion()).thenReturn("1.0.0");
    when(node1.getDependencies()).thenReturn(new ArrayList<>()); // No children

    // Node 2: Same artifact
    MavenArtifactNode node2 = mock(MavenArtifactNode.class);
    MavenArtifact artifact2 = mock(MavenArtifact.class);
    when(node2.getArtifact()).thenReturn(artifact2);
    when(artifact2.getGroupId()).thenReturn("com.example");
    when(artifact2.getArtifactId()).thenReturn("common");
    when(artifact2.getVersion()).thenReturn("1.0.0");
    when(node2.getDependencies()).thenReturn(new ArrayList<>());

    // Set the tree to have both
    // Actually DependencyParser iterates over getDependencyTree which returns roots (direct dependencies).
    // If we want different paths, usually one is direct and one is transitive.
    // Let's make node1 a direct dependency and node2 a child of another direct dependency.

    // Direct dependency A -> common
    MavenArtifactNode nodeA = mock(MavenArtifactNode.class);
    MavenArtifact artifactA = mock(MavenArtifact.class);
    when(nodeA.getArtifact()).thenReturn(artifactA);
    when(artifactA.getGroupId()).thenReturn("com.example");
    when(artifactA.getArtifactId()).thenReturn("A");
    when(artifactA.getVersion()).thenReturn("2.0.0");
    when(nodeA.getDependencies()).thenReturn(List.of(node1)); // A depends on common

    // Direct dependency B -> common (represented by node2)
    MavenArtifactNode nodeB = mock(MavenArtifactNode.class);
    MavenArtifact artifactB = mock(MavenArtifact.class);
    when(nodeB.getArtifact()).thenReturn(artifactB);
    when(artifactB.getGroupId()).thenReturn("com.example");
    when(artifactB.getArtifactId()).thenReturn("B");
    when(artifactB.getVersion()).thenReturn("2.0.0");
    when(nodeB.getDependencies()).thenReturn(List.of(node2)); // B depends on common

    when(mavenProject.getDependencyTree()).thenReturn(List.of(nodeA, nodeB));

    // Act
    List<OsvPackage> result = DependencyParser.parseDependencies(project);

    // Assert
    // We expect: A, B, and common.
    // "common" should appear ONCE.
    // "A" and "B" appear once.
    // Total 3 packages.

    long commonCount = result.stream()
        .filter(p -> p.name().equals("com.example:common") && p.version().equals("1.0.0"))
        .count();

    assertEquals(1, commonCount, "Dependency should be deduplicated");

    OsvPackage commonPkg = result.stream()
        .filter(p -> p.name().equals("com.example:common"))
        .findFirst()
        .orElseThrow();

    assertEquals(2, commonPkg.dependencyChains().size(), "Should have 2 chains");

    // Chains should be: [A, common] and [B, common]
    Set<List<String>> chains = commonPkg.dependencyChains();
    boolean hasPathA = chains.stream().anyMatch(c -> c.contains("com.example:A") && c.contains("com.example:common"));
    boolean hasPathB = chains.stream().anyMatch(c -> c.contains("com.example:B") && c.contains("com.example:common"));

    assertTrue(hasPathA, "Should contain path through A");
    assertTrue(hasPathB, "Should contain path through B");
  }
}
