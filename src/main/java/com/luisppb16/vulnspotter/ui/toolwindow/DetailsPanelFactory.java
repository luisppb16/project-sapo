/*
 * *****************************************************************************
 * Copyright (c)  2026 Luis Paolo Pepe Barra (@LuisPPB16).
 * All rights reserved.
 * *****************************************************************************
 */

package com.luisppb16.vulnspotter.ui.toolwindow;

/**
 * Factory for the {@link DetailsPanel}. Registered as an {@code applicationService} from the
 * optional {@code vulnspotter-jcef.xml} descriptor, so it is only resolvable when the JCEF plugin
 * is present. {@link #create()} may return {@code null} when JCEF is available on the classpath but
 * not supported at runtime (e.g. a JBR without JCEF binaries), letting the caller fall back to the
 * Swing implementation.
 */
public interface DetailsPanelFactory {

  /**
   * @return a JCEF-backed {@link DetailsPanel}, or {@code null} if JCEF is not usable now.
   */
  DetailsPanel create();
}
