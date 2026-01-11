/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.projectsapo.service.VulnerabilityScannerService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
class CheckVulnerabilitiesActionTest {

  @Mock private AnActionEvent event;
  @Mock private Project project;
  @Mock private ToolWindowManager toolWindowManager;
  @Mock private ToolWindow toolWindow;
  @Mock private ProgressManager progressManager;
  @Mock private VulnerabilityScannerService scannerService;
  @Mock private Application application;
  @Mock private MessageBus messageBus;

  private MockedStatic<ToolWindowManager> toolWindowManagerMock;
  private MockedStatic<ProgressManager> progressManagerMock;
  private MockedStatic<VulnerabilityScannerService> scannerServiceMock;
  private MockedStatic<ApplicationManager> applicationManagerMock;
  private MockedStatic<Notifications.Bus> notificationsBusMock;

  private CheckVulnerabilitiesAction action;

  @BeforeEach
  void setUp() {
    toolWindowManagerMock = mockStatic(ToolWindowManager.class);
    progressManagerMock = mockStatic(ProgressManager.class);
    scannerServiceMock = mockStatic(VulnerabilityScannerService.class);
    applicationManagerMock = mockStatic(ApplicationManager.class);
    notificationsBusMock = mockStatic(Notifications.Bus.class);

    action = new CheckVulnerabilitiesAction();

    when(event.getProject()).thenReturn(project);
    
    toolWindowManagerMock.when(() -> ToolWindowManager.getInstance(project)).thenReturn(toolWindowManager);
    when(toolWindowManager.getToolWindow("Project Sapo")).thenReturn(toolWindow);
    
    progressManagerMock.when(ProgressManager::getInstance).thenReturn(progressManager);
    
    scannerServiceMock.when(() -> VulnerabilityScannerService.getInstance(project)).thenReturn(scannerService);
    
    applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(application);
    
    // Mock invokeLater to run immediately
    doAnswer(invocation -> {
        Runnable r = invocation.getArgument(0);
        r.run();
        return null;
    }).when(application).invokeLater(any(Runnable.class));
  }

  @AfterEach
  void tearDown() {
    toolWindowManagerMock.close();
    progressManagerMock.close();
    scannerServiceMock.close();
    applicationManagerMock.close();
    notificationsBusMock.close();
  }

  @Test
  void testActionPerformed() {
    // Arrange
    // Mock the ProgressManager to execute the task
    doAnswer(invocation -> {
        Task.Backgroundable task = invocation.getArgument(0);
        task.run(mock(com.intellij.openapi.progress.ProgressIndicator.class));
        return null;
    }).when(progressManager).run(any(Task.Backgroundable.class));

    // Mock the scanner service to complete immediately
    when(scannerService.scanDependencies(any(Consumer.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    // Act
    action.actionPerformed(event);

    // Assert
    verify(toolWindow).show();
    verify(scannerService).scanDependencies(any(Consumer.class));
    // Verify notification was sent (either success or warning, depending on callback execution which is empty here)
    notificationsBusMock.verify(() -> Notifications.Bus.notify(any(Notification.class), eq(project)));
  }
}
