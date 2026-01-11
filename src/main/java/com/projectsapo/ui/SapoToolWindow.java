/*
 * Copyright (c) 2026 Luis Paolo Pepe Barra (LuisPPB16).
 * All rights reserved.
 */

package com.projectsapo.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.projectsapo.model.OsvVulnerability;
import com.projectsapo.service.VulnerabilityScannerService;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SapoToolWindow {

    private final JPanel content;
    private final JBTable resultsTable;
    private final DefaultTableModel tableModel;
    private final JEditorPane detailsPane;
    private final Project project;
    private final JButton scanButton;
    private final JLabel statusLabel;
    private final List<VulnerabilityScannerService.ScanResult> scanResults = new ArrayList<>();

    public SapoToolWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        content = new JPanel(new BorderLayout());

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scanButton = new JButton("Scan Dependencies");
        scanButton.addActionListener(e -> runScan());
        
        statusLabel = new JLabel("");
        statusLabel.setBorder(JBUI.Borders.emptyLeft(10));
        
        toolbar.add(scanButton);
        toolbar.add(statusLabel);
        content.add(toolbar, BorderLayout.NORTH);

        // Splitter
        Splitter splitter = new Splitter(false, 0.4f);

        // Table
        String[] columnNames = {"Dependency", "Version", "Vulns", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            // Define column types for proper sorting
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return Integer.class; // Vulns count
                return String.class;
            }
        };
        resultsTable = new JBTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Enable sorting
        resultsTable.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        resultsTable.setRowSorter(sorter);
        
        // Default sort by Dependency name (column 0) ascending
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = resultsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    // Convert view index to model index because of sorting
                    int modelRow = resultsTable.convertRowIndexToModel(selectedRow);
                    if (modelRow >= 0 && modelRow < scanResults.size()) {
                        // Note: scanResults list order matches the insertion order into the table model
                        // So modelRow index corresponds to scanResults index
                        showDetails(scanResults.get(modelRow));
                    }
                }
            }
        });

        splitter.setFirstComponent(new JBScrollPane(resultsTable));

        // Details Pane (HTML)
        detailsPane = new JEditorPane();
        detailsPane.setEditable(false);
        detailsPane.setContentType("text/html");
        detailsPane.setMargin(JBUI.insets(10));
        
        // Handle hyperlinks
        detailsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL());
            }
        });
        
        // Apply basic styling
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { font-family: sans-serif; font-size: 12px; }");
        styleSheet.addRule("h2 { color: #E53935; margin-bottom: 5px; }"); // Red for title
        styleSheet.addRule("h3 { margin-top: 15px; margin-bottom: 5px; font-size: 14px; }");
        styleSheet.addRule(".vuln-card { border: 1px solid #ccc; padding: 10px; margin-bottom: 15px; background-color: #f9f9f9; }");
        styleSheet.addRule(".vuln-id { font-weight: bold; color: #1976D2; }");
        styleSheet.addRule(".fixed-in { font-weight: bold; color: #388E3C; }");
        styleSheet.addRule(".safe-message { color: #388E3C; font-size: 14px; font-weight: bold; }");
        styleSheet.addRule("a { color: #1976D2; text-decoration: none; }");
        styleSheet.addRule("ul { margin-top: 5px; margin-bottom: 5px; padding-left: 20px; }");
        styleSheet.addRule("li { margin-bottom: 3px; }");
        
        // Dark mode adjustments (basic heuristic)
        if (ColorUtil.isDark(UIUtil.getPanelBackground())) {
             styleSheet.addRule("body { color: #bbbbbb; }");
             styleSheet.addRule(".vuln-card { border: 1px solid #555; background-color: #3c3f41; }");
             styleSheet.addRule("h2 { color: #ff5252; }");
             styleSheet.addRule(".vuln-id { color: #64b5f6; }");
             styleSheet.addRule(".fixed-in { color: #81c784; }");
             styleSheet.addRule(".safe-message { color: #81c784; }");
             styleSheet.addRule("a { color: #64b5f6; }");
        }

        detailsPane.setEditorKit(kit);
        
        splitter.setSecondComponent(new JBScrollPane(detailsPane));

        content.add(splitter, BorderLayout.CENTER);
    }

    private void runScan() {
        scanButton.setEnabled(false);
        statusLabel.setText("Scanning dependencies...");
        statusLabel.setIcon(new com.intellij.ui.AnimatedIcon.Default());
        
        tableModel.setRowCount(0);
        scanResults.clear();
        detailsPane.setText("");
        
        VulnerabilityScannerService.getInstance(project).scanDependencies(result -> {
            // Add to list
            scanResults.add(result);
            
            // Add to table
            Object[] row = {
                result.pkg().name(),
                result.pkg().version(),
                result.vulnerabilities().size(),
                result.vulnerable() ? "VULNERABLE" : "Safe"
            };
            tableModel.addRow(row);
        }).whenComplete((v, ex) -> {
            SwingUtilities.invokeLater(() -> {
                scanButton.setEnabled(true);
                statusLabel.setIcon(null);
                if (ex != null) {
                    statusLabel.setText("Scan failed: " + ex.getMessage());
                } else {
                    statusLabel.setText("Scan complete. Found " + scanResults.size() + " dependencies.");
                }
            });
        });
    }

    private void showDetails(VulnerabilityScannerService.ScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        
        sb.append("<h1>").append(result.pkg().name()).append("</h1>");
        sb.append("<p><b>Version:</b> ").append(result.pkg().version()).append("</p>");

        if (result.vulnerable()) {
            sb.append("<h2>").append(result.vulnerabilities().size()).append(" Vulnerabilities Found</h2>");
            
            for (OsvVulnerability vuln : result.vulnerabilities()) {
                sb.append("<div class='vuln-card'>");
                
                sb.append("<div class='vuln-id'>").append(vuln.id()).append("</div>");
                sb.append("<h3>").append(vuln.summary() != null ? vuln.summary() : "No summary").append("</h3>");
                
                String fixedVersion = "Unknown";
                if (vuln.affected() != null) {
                    for (OsvVulnerability.Affected affected : vuln.affected()) {
                        if (affected.ranges() != null) {
                            for (OsvVulnerability.Range range : affected.ranges()) {
                                if (range.events() != null) {
                                    for (OsvVulnerability.Event event : range.events()) {
                                        if (event.fixed() != null) {
                                            fixedVersion = event.fixed();
                                            break;
                                        }
                                    }
                                }
                                if (!"Unknown".equals(fixedVersion)) break;
                            }
                        }
                        if (!"Unknown".equals(fixedVersion)) break;
                    }
                }
                
                sb.append("<p>Fixed in: <span class='fixed-in'>").append(fixedVersion).append("</span></p>");
                sb.append("<p>").append(vuln.details() != null ? vuln.details().replace("\n", "<br>") : "No details provided").append("</p>");
                
                // References
                if (vuln.references() != null && !vuln.references().isEmpty()) {
                    sb.append("<p><b>References:</b></p>");
                    sb.append("<ul>");
                    for (OsvVulnerability.Reference ref : vuln.references()) {
                        sb.append("<li><a href='").append(ref.url()).append("'>").append(ref.url()).append("</a></li>");
                    }
                    sb.append("</ul>");
                }
                
                sb.append("</div>");
            }
        } else {
            sb.append("<p class='safe-message'>✅ No known vulnerabilities found for this package.</p>");
        }

        sb.append("</body></html>");
        detailsPane.setText(sb.toString());
        detailsPane.setCaretPosition(0);
    }

    public JPanel getContent() {
        return content;
    }
}
