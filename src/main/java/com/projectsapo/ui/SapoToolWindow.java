/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.projectsapo.model.OsvVulnerability;
import com.projectsapo.service.VulnerabilityScannerService;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

/**
 * Modern UI for Project Sapo using JCEF. Replicates Snyk's design for dependency chains and
 * severity badges.
 */
public class SapoToolWindow {

  private final JPanel content;
  private final JBTable resultsTable;
  private final DefaultTableModel tableModel;
  private final JBCefBrowser browser;
  private final Project project;
  private final JButton scanButton;
  private final JLabel statusLabel;
  private final List<VulnerabilityScannerService.ScanResult> scanResults = new ArrayList<>();
  private static final ThreadLocal<NumberFormat> NUMBER_FORMAT =
      ThreadLocal.withInitial(() -> NumberFormat.getInstance(Locale.ROOT));

  public SapoToolWindow(Project project, ToolWindow toolWindow) {
    this.project = project;
    this.content = new JPanel(new BorderLayout());
    this.browser = new JBCefBrowser();

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    scanButton = new JButton("Scan Dependencies");
    scanButton.addActionListener(e -> runScan());
    statusLabel = new JLabel("Ready");
    statusLabel.setBorder(JBUI.Borders.emptyLeft(10));
    toolbar.add(scanButton);
    toolbar.add(statusLabel);
    content.add(toolbar, BorderLayout.NORTH);

    Splitter splitter = new Splitter(false, 0.35f);

    // Updated column name to "Severity"
    tableModel =
        new DefaultTableModel(new String[] {"Severity", "Dependency", "Version", "Vulns"}, 0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return false;
          }

          @Override
          public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Icon.class;
            if (columnIndex == 3) return Integer.class;
            return String.class;
          }
        };
    resultsTable = new JBTable(tableModel);
    resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    resultsTable.setAutoCreateRowSorter(true); // Enable sorting

    // Set default sort to "Dependency" column (index 1) alphabetically
    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    resultsTable.setRowSorter(sorter);
    sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING)));

    // Adjust "Severity" column width to fit the header text
    TableColumn iconColumn = resultsTable.getColumnModel().getColumn(0);
    iconColumn.setPreferredWidth(60);
    iconColumn.setMaxWidth(80);
    iconColumn.setMinWidth(60);

    resultsTable
        .getSelectionModel()
        .addListSelectionListener(
            e -> {
              if (!e.getValueIsAdjusting()) {
                int row = resultsTable.getSelectedRow();
                if (row >= 0) {
                  int modelRow = resultsTable.convertRowIndexToModel(row);
                  showDetails(scanResults.get(modelRow));
                }
              }
            });

    splitter.setFirstComponent(new JBScrollPane(resultsTable));
    splitter.setSecondComponent(browser.getComponent());
    content.add(splitter, BorderLayout.CENTER);
    browser.loadHTML(
        generateHtml("<h1>Project Sapo</h1><p>Select a dependency to see details.</p>"));
  }

  private void runScan() {
    scanButton.setEnabled(false);
    statusLabel.setText("Scanning...");
    tableModel.setRowCount(0);
    scanResults.clear();

    VulnerabilityScannerService.getInstance(project)
        .scanDependencies(
            result -> {
              ApplicationManager.getApplication()
                  .invokeLater(
                      () -> {
                        scanResults.add(result);
                        String highestSev = getHighestSeverity(result.vulnerabilities());
                        tableModel.addRow(
                            new Object[] {
                              getSeverityIcon(highestSev),
                              result.pkg().name(),
                              result.pkg().version(),
                              result.vulnerabilities().size()
                            });
                      });
            })
        .whenComplete(
            (v, ex) ->
                ApplicationManager.getApplication()
                    .invokeLater(
                        () -> {
                          scanButton.setEnabled(true);
                          statusLabel.setText(
                              ex != null
                                  ? "Scan failed"
                                  : "Scan complete (" + scanResults.size() + ")");
                        }));
  }

  private void showDetails(VulnerabilityScannerService.ScanResult result) {
    StringBuilder sb = new StringBuilder();
    sb.append("<div class='header'>");
    sb.append("<h1>").append(result.pkg().name()).append("</h1>");
    sb.append("<div class='version'>Version: ").append(result.pkg().version()).append("</div>");
    sb.append("</div>");

    Set<List<String>> chains = result.pkg().dependencyChains();
    if (chains != null && !chains.isEmpty()) {
      int pathIndex = 1;
      for (List<String> chain : chains) {
        if (chains.size() > 1) {
          sb.append("<div class='path-header'>Path #").append(pathIndex++).append("</div>");
        }
        sb.append("<div class='snyk-chain'>");

        // Root
        sb.append("<div class='chain-item root'><span class='icon'>📦</span> ")
            .append(project.getName())
            .append("</div>");

        // Intermediate
        for (int i = 0; i < chain.size() - 1; i++) {
          sb.append("<div class='chain-item intermediate' style='padding-left: ")
              .append((i + 1) * 20)
              .append("px;'>");
          sb.append("<span class='connector'>└─</span> <span class='icon'>📄</span> ")
              .append(chain.get(i));
          sb.append("</div>");
        }

        // Target (Vulnerable)
        String target = chain.get(chain.size() - 1);
        sb.append("<div class='chain-item target' style='padding-left: ")
            .append(chain.size() * 20)
            .append("px;'>");
        sb.append("<span class='connector'>└─</span> <span class='icon'>⚠️</span> <b>")
            .append(target)
            .append("</b>");
        sb.append("</div>");

        sb.append("</div>"); // End snyk-chain

        // Remediation
        if (chain.size() > 1) {
          sb.append("<div class='remediation-box'>");
          sb.append("<div class='remediation-title'>Remediation</div>");
          sb.append("<p>Upgrade <code>")
              .append(chain.get(0))
              .append("</code> to remove this vulnerability.</p>");
          sb.append("</div>");
        }
      }
    }

    if (result.vulnerable()) {
      sb.append("<div class='section-title'>")
          .append(result.vulnerabilities().size())
          .append(" Vulnerabilities</div>");
      for (OsvVulnerability vuln : result.vulnerabilities()) {
        String sev = getSeverity(vuln);
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<span class='badge ")
            .append(sev.toLowerCase())
            .append("'>")
            .append(sev)
            .append("</span>");
        sb.append("<span class='vuln-id'>").append(vuln.id()).append("</span>");
        sb.append("</div>");
        sb.append("<div class='vuln-summary'>")
            .append(vuln.summary() != null ? vuln.summary() : "No summary")
            .append("</div>");
        sb.append("<div class='fixed-box'>Fixed in: <span class='fixed-ver'>")
            .append(findFixedVersion(vuln))
            .append("</span></div>");
        sb.append("<div class='details'>")
            .append(vuln.details() != null ? vuln.details().replace("\n", "<br>") : "")
            .append("</div>");
        sb.append("</div>");
      }
    } else {
      sb.append("<div class='safe-banner'>✅ No known vulnerabilities found.</div>");
    }

    browser.loadHTML(generateHtml(sb.toString()));
  }

  private String generateHtml(String bodyContent) {
    boolean isDark = ColorUtil.isDark(UIUtil.getPanelBackground());
    String bgColor = isDark ? "#1e1e1e" : "#ffffff";
    String textColor = isDark ? "#d4d4d4" : "#333333";
    String cardBg = isDark ? "#252526" : "#ffffff";
    String borderColor = isDark ? "#454545" : "#e0e0e0";

    return "<html><head><style>"
        + "body { font-family: 'Inter', sans-serif; padding: 24px; background: "
        + bgColor
        + "; color: "
        + textColor
        + "; line-height: 1.5; }"
        + "h1 { font-size: 20px; font-weight: 600; margin: 0; }"
        + ".path-header { font-weight: bold; margin-top: 12px; margin-bottom: 4px; color: #2196f3; font-size: 14px; }"
        + ".snyk-chain { font-family: 'JetBrains Mono', monospace; margin: 8px 0; font-size: 13px; }"
        + ".chain-item { padding: 4px 0; display: flex; align-items: center; }"
        + ".connector { color: #888; margin-right: 8px; }"
        + ".icon { margin-right: 6px; }"
        + ".target { color: #e53935; font-weight: bold; }"
        + ".remediation-box { background: "
        + (isDark ? "#1a2a3a" : "#e3f2fd")
        + "; border: 1px solid #2196f3; border-radius: 4px; padding: 12px; margin-top: 8px; margin-bottom: 16px; }"
        + ".remediation-title { font-weight: 800; color: #2196f3; text-transform: uppercase; font-size: 11px; margin-bottom: 4px; }"
        + ".card { background: "
        + cardBg
        + "; border: 1px solid "
        + borderColor
        + "; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }"
        + ".badge { padding: 4px 8px; border-radius: 4px; color: white; font-size: 11px; font-weight: 800; margin-right: 12px; text-transform: uppercase; }"
        + ".critical { background: #b71c1c; } .high { background: #e65100; } .medium { background: #f57f17; color: white; } .low { background: #33691e; }"
        + ".fixed-box { background: rgba(67, 160, 71, 0.1); color: #2e7d32; padding: 4px 8px; border-radius: 4px; font-weight: 600; display: inline-block; margin: 8px 0; }"
        + "</style></head><body>"
        + bodyContent
        + "</body></html>";
  }

  private String getHighestSeverity(List<OsvVulnerability> vulns) {
    if (vulns.isEmpty()) return "SAFE";
    int max = 0;
    for (OsvVulnerability v : vulns) {
      int l =
          switch (getSeverity(v)) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
          };
      if (l > max) max = l;
    }
    return switch (max) {
      case 4 -> "CRITICAL";
      case 3 -> "HIGH";
      case 2 -> "MEDIUM";
      case 1 -> "LOW";
      default -> "SAFE";
    };
  }

  private Icon getSeverityIcon(String s) {
    return switch (s) {
      case "CRITICAL" -> AllIcons.General.Error;
      case "HIGH" -> AllIcons.General.Warning;
      case "MEDIUM" -> AllIcons.General.Note;
      case "LOW" -> AllIcons.General.Information;
      default -> AllIcons.General.InspectionsOK;
    };
  }

  private String getSeverity(OsvVulnerability v) {
    if (v.databaseSpecific() != null) {
      Object sev = v.databaseSpecific().get("severity");
      if (sev instanceof String) return ((String) sev).toUpperCase();
    }
    if (v.severity() != null) {
      for (OsvVulnerability.Severity s : v.severity()) {
        if ("CVSS_V3".equals(s.type()) || "CVSS_V2".equals(s.type())) {
          try {
            double sc = NUMBER_FORMAT.get().parse(s.score()).doubleValue();
            if (sc >= 9.0) return "CRITICAL";
            if (sc >= 7.0) return "HIGH";
            if (sc >= 4.0) return "MEDIUM";
            return "LOW";
          } catch (ParseException | NumberFormatException ignored) {
          }
        }
      }
    }
    return "MEDIUM";
  }

  private String findFixedVersion(OsvVulnerability v) {
    if (v.affected() == null) return "Unknown";
    return v.affected().stream()
        .filter(a -> a.ranges() != null)
        .flatMap(a -> a.ranges().stream())
        .filter(r -> r.events() != null)
        .flatMap(r -> r.events().stream())
        .map(OsvVulnerability.Event::fixed)
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElse("Unknown");
  }

  public JComponent getContent() {
    return content;
  }
}
