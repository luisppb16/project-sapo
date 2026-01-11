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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootsEnumerator;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.util.Processor;
import com.projectsapo.model.OsvPackage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

  private MockedStatic<MavenProjectsManager> mavenProjectsManagerMock;
  private MockedStatic<ProjectDataManager> projectDataManagerMock;
  private MockedStatic<OrderEnumerator> orderEnumeratorMock;
  private MockedStatic<ApplicationManager> applicationManagerMock;

  @BeforeEach
  void setUp() {
    mavenProjectsManagerMock = mockStatic(MavenProjectsManager.class);
    projectDataManagerMock = mockStatic(ProjectDataManager.class);
    orderEnumeratorMock = mockStatic(OrderEnumerator.class);
    applicationManagerMock = mockStatic(ApplicationManager.class);

    mavenProjectsManagerMock
        .when(() -> MavenProjectsManager.getInstance(project))
        .thenReturn(mavenProjectsManager);
    projectDataManagerMock
        .when(ProjectDataManager::getInstance)
        .thenReturn(projectDataManager);
    orderEnumeratorMock
        .when(() -> OrderEnumerator.orderEntries(project))
        .thenReturn(orderEnumerator);
    applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(application);
    
    // Default behavior for OrderEnumerator to avoid NPEs in tests that don't specifically mock it
    when(orderEnumerator.librariesOnly()).thenReturn(orderEnumerator);
  }

  @AfterEach
  void tearDown() {
    mavenProjectsManagerMock.close();
    projectDataManagerMock.close();
    orderEnumeratorMock.close();
    applicationManagerMock.close();
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
  }

  @Test
  void testParseDependencies_Gradle() {
    // Arrange
    when(mavenProjectsManager.hasProjects()).thenReturn(false);

    ExternalProjectInfo projectInfo = mock(ExternalProjectInfo.class);
    DataNode<ProjectData> projectNode = mock(DataNode.class);

    when(projectDataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID))
        .thenReturn(List.of(projectInfo));
    when(projectInfo.getExternalProjectStructure()).thenReturn(projectNode);
    
    // Let's test the fallback mechanism which uses OrderEnumerator
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
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    OsvPackage pkg = result.get(0);
    assertEquals("com.example:lib", pkg.name());
    assertEquals("1.0.0", pkg.version());
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
}
