/*******************************************************************************
 * Copyright (c) 2011 Nils Hartmann
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nils Hartmann - initial API and implementation
 ******************************************************************************/
package nh.scr;

import java.io.PrintStream;
import java.text.MessageFormat;

import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.config.ScrConfiguration;
import org.apache.felix.scr.impl.helper.Logger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Nils Hartmann (nils@nilshartmann.net)
 * 
 */
public class ComponentConfigurationServiceTracker extends ServiceTracker<ComponentMetadata, ComponentHolder> implements
    Logger {

  // the configuration
  private ScrConfiguration  m_configuration;

  // global component registration
  private ComponentRegistry m_componentRegistry;

  /**
   * @param context
   * @param clazz
   * @param customizer
   */
  public ComponentConfigurationServiceTracker(BundleContext context) {
    super(context, ComponentMetadata.class, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
   */
  @Override
  public ComponentHolder addingService(ServiceReference<ComponentMetadata> reference) {
    ComponentMetadata metadata = context.getService(reference);

    try {
      // check and reserve the component name
      m_componentRegistry.checkComponentName(metadata.getName());

      // validate the component metadata
      metadata.validate(this);

      // Request creation of the component manager
      ComponentHolder holder = m_componentRegistry.createComponentHolder(this, metadata);

      // register the component after validation
      m_componentRegistry.registerComponentHolder(metadata.getName(), holder);
      m_managers.add(holder);

      // enable the component
      if (metadata.isEnabled()) {
        holder.enableComponents();
      }
    } catch (Throwable t) {
      // There is a problem with this particular component, we'll log the error
      // and proceed to the next one
      log(LogService.LOG_ERROR, "Cannot register Component", metadata, t);

      // make sure the name is not reserved any more
      m_componentRegistry.unregisterComponentHolder(metadata.getName());
    }

  }

  /**
   * Returns <code>true</code> if logging for the given level is enabled.
   */
  @Override
  public boolean isLogEnabled(int level) {
    return m_configuration.getLogLevel() >= level;
  }

  /**
   * Method to actually emit the log message. If the LogService is available, the message will be logged through the
   * LogService. Otherwise the message is logged to stdout (or stderr in case of LOG_ERROR level messages),
   * 
   * @param level
   *          The log level to log the message at
   * @param pattern
   *          The <code>java.text.MessageFormat</code> message format string for preparing the message
   * @param arguments
   *          The format arguments for the <code>pattern</code> string.
   * @param ex
   *          An optional <code>Throwable</code> whose stack trace is written, or <code>null</code> to not log a stack
   *          trace.
   */
  @Override
  public void log(int level, String pattern, Object[] arguments, ComponentMetadata metadata, Throwable ex) {
    if (isLogEnabled(level)) {
      final String message = MessageFormat.format(pattern, arguments);
      log(level, message, metadata, ex);
    }
  }

  /**
   * Method to actually emit the log message. If the LogService is available, the message will be logged through the
   * LogService. Otherwise the message is logged to stdout (or stderr in case of LOG_ERROR level messages),
   * 
   * @param level
   *          The log level to log the message at
   * @param message
   *          The message to log
   * @param ex
   *          An optional <code>Throwable</code> whose stack trace is written, or <code>null</code> to not log a stack
   *          trace.
   */
  @Override
  public void log(int level, String message, ComponentMetadata metadata, Throwable ex) {
    if (isLogEnabled(level)) {
      // prepend the metadata name to the message
      if (metadata != null) {
        message = "[" + metadata.getName() + "] " + message;
      }

      PrintStream out = (level == LogService.LOG_ERROR) ? System.err : System.out;

      // level as a string
      StringBuffer buf = new StringBuffer();
      switch (level) {
      case (LogService.LOG_DEBUG):
        buf.append("DEBUG: ");
        break;
      case (LogService.LOG_INFO):
        buf.append("INFO : ");
        break;
      case (LogService.LOG_WARNING):
        buf.append("WARN : ");
        break;
      case (LogService.LOG_ERROR):
        buf.append("ERROR: ");
        break;
      default:
        buf.append("UNK  : ");
        break;
      }

      // // bundle information
      // if (bundle != null) {
      // buf.append(bundle.getSymbolicName());
      // buf.append(" (");
      // buf.append(bundle.getBundleId());
      // buf.append("): ");
      // }

      // the message
      buf.append(message);

      // keep the message and the stacktrace together
      synchronized (out) {
        out.println(buf);
        if (ex != null) {
          ex.printStackTrace(out);
        }
      }
    }
  }

}
