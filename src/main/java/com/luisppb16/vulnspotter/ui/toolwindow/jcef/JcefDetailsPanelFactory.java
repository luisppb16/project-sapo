/*
 * *****************************************************************************
 * Copyright (c)  2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 * *****************************************************************************
 */

package com.luisppb16.vulnspotter.ui.toolwindow.jcef;

import com.intellij.ui.jcef.JBCefApp;
import com.luisppb16.vulnspotter.ui.toolwindow.DetailsPanel;
import com.luisppb16.vulnspotter.ui.toolwindow.DetailsPanelFactory;

/**
 * JCEF {@link DetailsPanelFactory}. Registered as an {@code applicationService} from the optional
 * {@code vulnspotter-jcef.xml} descriptor, so it only exists when the JCEF plugin is present. The
 * {@link #create()} method additionally checks {@link JBCefApp#isSupported()} to cover the case
 * where JCEF classes are on the classpath but the native runtime is unavailable (e.g. a JBR without
 * JCEF binaries, or a headless environment).
 */
public final class JcefDetailsPanelFactory implements DetailsPanelFactory {

  @Override
  public DetailsPanel create() {
    if (!JBCefApp.isSupported()) {
      return null;
    }
    return new JcefDetailsPanel();
  }
}
