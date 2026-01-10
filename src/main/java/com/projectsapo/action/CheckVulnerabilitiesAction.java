package com.projectsapo.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.projectsapo.service.VulnerabilityScannerService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Action to trigger vulnerability checks.
 */
public class CheckVulnerabilitiesAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // Activate the ToolWindow if it exists
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Project Sapo");
        if (toolWindow != null) {
            toolWindow.show();
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Checking Vulnerabilities", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                AtomicInteger vulnerableCount = new AtomicInteger(0);
                
                VulnerabilityScannerService.getInstance(project).scanDependencies(result -> {
                    if (result.vulnerable()) {
                        vulnerableCount.incrementAndGet();
                        // We don't spam notifications anymore, the tool window handles details
                    }
                }).join(); 

                if (vulnerableCount.get() > 0) {
                     showNotification(project, "Project Sapo", "Scan complete. Found vulnerabilities in " + vulnerableCount.get() + " dependencies. Check the Project Sapo tool window for details.", NotificationType.WARNING);
                } else {
                     showNotification(project, "Project Sapo", "Scan complete. No vulnerabilities found.", NotificationType.INFORMATION);
                }
            }
        });
    }

    private void showNotification(Project project, String title, String content, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = new Notification(
                    "Project Sapo Notifications",
                    title,
                    content,
                    type
            );
            Notifications.Bus.notify(notification, project);
        });
    }
}
