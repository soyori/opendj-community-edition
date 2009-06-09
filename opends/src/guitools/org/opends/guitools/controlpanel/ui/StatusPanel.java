/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2008-2009 Sun Microsystems, Inc.
 */

package org.opends.guitools.controlpanel.ui;

import static org.opends.messages.AdminToolMessages.*;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.opends.guitools.controlpanel.datamodel.BackendDescriptor;
import org.opends.guitools.controlpanel.datamodel.BaseDNDescriptor;
import org.opends.guitools.controlpanel.datamodel.BaseDNTableModel;
import org.opends.guitools.controlpanel.datamodel.ConnectionHandlerDescriptor;
import org.opends.guitools.controlpanel.datamodel.ConnectionHandlerTableModel;
import org.opends.guitools.controlpanel.datamodel.ServerDescriptor;
import org.opends.guitools.controlpanel.event.ConfigurationChangeEvent;
import org.opends.guitools.controlpanel.event.ScrollPaneBorderListener;
import org.opends.guitools.controlpanel.ui.components.LabelWithHelpIcon;
import org.opends.guitools.controlpanel.ui.renderer.BaseDNCellRenderer;
import org.opends.guitools.controlpanel.ui.renderer.CustomCellRenderer;
import org.opends.guitools.controlpanel.util.Utilities;
import org.opends.guitools.controlpanel.util.ViewPositions;
import org.opends.messages.Message;
import org.opends.messages.MessageBuilder;
import org.opends.server.types.DN;
import org.opends.server.types.OpenDsException;

/**
 * The panel displaying the general status of the server (started/stopped),
 * basic configuration (base DNs, connection listeners) and that allows to start
 * and stop the server.
 *
 */
class StatusPanel extends StatusGenericPanel
{
  private static final long serialVersionUID = -6493442314639004717L;
  // The placeholder where we display errors.
  private JLabel serverStatus;
  private LabelWithHelpIcon currentConnections;
  private JLabel hostName;
  private JLabel administrativeUsers;
  private JLabel installPath;
  private JLabel instancePath;
  private JLabel opendsVersion;
  private LabelWithHelpIcon javaVersion;
  private JLabel adminConnector;
  private JLabel dbTableEmpty;
  private JLabel connectionHandlerTableEmpty;
  private JLabel lInstancePath;
  private JButton stopButton;
  private JButton startButton;
  private JButton restartButton;
  private BaseDNTableModel dbTableModelWithReplication;
  private BaseDNTableModel dbTableModelWithoutReplication;
  private JTable noReplicatedBaseDNsTable;
  private JTable replicationBaseDNsTable;

  private ConnectionHandlerTableModel connectionHandlerTableModel;
  private JTable connectionHandlersTable;

  /**
   * Default constructor.
   *
   */
  public StatusPanel()
  {
    super();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    createErrorPane();
    gbc.insets = new Insets(20, 20, 10, 20);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    add(errorPane, gbc);
    JPanel inScrollPanel = new JPanel(new GridBagLayout());
    inScrollPanel.setOpaque(false);
    gbc.gridy ++;
    gbc.weighty = 1.0;
    JScrollPane scroll = Utilities.createBorderLessScrollBar(inScrollPanel);
    gbc.insets = new Insets(0, 0, 0, 0);
    add(scroll, gbc);
    ScrollPaneBorderListener.createFullBorderListener(scroll);

    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(15, 10, 0, 10);
    gbc.weighty = 0.0;
    inScrollPanel.add(createServerStatusPanel(), gbc);

    gbc.gridy ++;
    inScrollPanel.add(createServerDetailsPanel(), gbc);

//  To compensate titled border.
    gbc.insets.left += 3;
    gbc.insets.right += 3;
    gbc.gridy ++;
    inScrollPanel.add(createListenersPanel(), gbc);

    gbc.gridy ++;
    gbc.insets.bottom = 20;
    inScrollPanel.add(createBackendsPanel(), gbc);

    gbc.gridy ++;
    gbc.weighty = 1.0;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.fill = GridBagConstraints.VERTICAL;
    inScrollPanel.add(Box.createVerticalGlue(), gbc);
  }

  /**
   * {@inheritDoc}
   */
  public Component getPreferredFocusComponent()
  {
    if (startButton.isVisible())
    {
      return startButton;
    }
    else
    {
      return stopButton;
    }
  }


  /**
   * {@inheritDoc}
   */
  public boolean requiresBorder()
  {
    return false;
  }

  private void recalculateSizes()
  {
    Utilities.updateTableSizes(replicationBaseDNsTable);
    Utilities.updateTableSizes(noReplicatedBaseDNsTable);
    Utilities.updateTableSizes(connectionHandlersTable);
  }

  /**
   * {@inheritDoc}
   */
  public Message getTitle()
  {
    return INFO_CTRL_PANEL_STATUS_PANEL_TITLE.get();
  }

  /**
   * {@inheritDoc}
   */
  public void configurationChanged(final ConfigurationChangeEvent ev)
  {
    if (SwingUtilities.isEventDispatchThread())
    {
      updateContents(ev.getNewDescriptor());
    }
    else
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        /**
         * {@inheritDoc}
         */
        public void run()
        {
          updateContents(ev.getNewDescriptor());
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  public void okClicked()
  {
  }

  /**
   * Updates the contents of the panel with the provided ServerDescriptor.
   * @param desc the ServerDescriptor.
   */
  public void updateContents(ServerDescriptor desc)
  {
    JScrollPane scroll = Utilities.getContainingScroll(this);
    ViewPositions pos;
    if (scroll != null)
    {
      pos = Utilities.getViewPositions(scroll);
    }
    else
    {
      pos = Utilities.getViewPositions(this);
    }

    Collection<OpenDsException> exceptions = desc.getExceptions();
    if (exceptions.size() == 0)
    {
      boolean errorPaneVisible = false;
      if (desc.getStatus() == ServerDescriptor.ServerStatus.STARTED)
      {
        if (!desc.isAuthenticated())
        {
          errorPaneVisible = true;
          MessageBuilder mb = new MessageBuilder();
          mb.append(
              INFO_CTRL_PANEL_AUTH_REQUIRED_TO_BROWSE_MONITORING_SUMMARY.
              get());
          mb.append("<br><br>"+getAuthenticateHTML());
          Message title =
            INFO_CTRL_PANEL_AUTHENTICATION_REQUIRED_SUMMARY.get();
          updateErrorPane(errorPane, title,
              ColorAndFontConstants.errorTitleFont,
              mb.toMessage(), ColorAndFontConstants.defaultFont);
        }
      }
      else if (desc.getStatus() ==
        ServerDescriptor.ServerStatus.NOT_CONNECTED_TO_REMOTE)
      {
        errorPaneVisible = true;
        MessageBuilder mb = new MessageBuilder();
        mb.append(INFO_CTRL_PANEL_CANNOT_CONNECT_TO_REMOTE_DETAILS.get(
            desc.getHostname()));
        mb.append("<br><br>"+getAuthenticateHTML());
        Message title =
          INFO_CTRL_PANEL_CANNOT_CONNECT_TO_REMOTE_SUMMARY.get();
        updateErrorPane(errorPane, title,
            ColorAndFontConstants.errorTitleFont,
            mb.toMessage(), ColorAndFontConstants.defaultFont);
      }
      if (errorPane.isVisible() != errorPaneVisible)
      {
        errorPane.setVisible(errorPaneVisible);
      }
    }
    else
    {
      ArrayList<Message> msgs = new ArrayList<Message>();
      for (OpenDsException oe : exceptions)
      {
        msgs.add(oe.getMessageObject());
      }
      Message title = ERR_CTRL_PANEL_ERROR_READING_CONFIGURATION_SUMMARY.get();
      MessageBuilder mb = new MessageBuilder();
      for (Message error : msgs)
      {
        if (mb.length() > 0)
        {
          mb.append("<br>");
        }
        mb.append(error);
      }
      if (desc.getStatus() == ServerDescriptor.ServerStatus.STARTED)
      {
        if (!desc.isAuthenticated())
        {
          mb.append("<br>");
          mb.append(
     INFO_CTRL_PANEL_AUTH_REQUIRED_TO_BROWSE_MONITORING_SUMMARY.
     get());
          mb.append("<br><br>"+getAuthenticateHTML());
        }
      }
      else if (desc.getStatus() == ServerDescriptor.ServerStatus.STARTED)
      {
        mb.append("<br>");
        mb.append(INFO_CTRL_PANEL_CANNOT_CONNECT_TO_REMOTE_DETAILS.get(
          desc.getHostname()));
        mb.append("<br><br>"+getAuthenticateHTML());
      }
      updateErrorPane(errorPane, title, ColorAndFontConstants.errorTitleFont,
          mb.toMessage(), ColorAndFontConstants.defaultFont);

      if (!errorPane.isVisible())
      {
        errorPane.setVisible(true);
      }
    }

    serverStatus.setText(getStatusLabel(desc));

    boolean isRunning = desc.getStatus() ==
      ServerDescriptor.ServerStatus.STARTED;
    boolean isAuthenticated = desc.isAuthenticated();
    boolean isLocal = desc.isLocal();

    startButton.setVisible(desc.getStatus() ==
      ServerDescriptor.ServerStatus.STOPPED  && isLocal);
    restartButton.setVisible(isRunning  && isLocal);
    stopButton.setVisible(isRunning && isLocal);

    if (!isRunning)
    {
      if (isLocal)
      {
        Utilities.setNotAvailableBecauseServerIsDown(currentConnections);
      }
      else
      {
        Utilities.setTextValue(currentConnections,
            INFO_NOT_AVAILABLE_SHORT_LABEL.get().toString());
      }
    }
    else if (!isAuthenticated)
    {
      Utilities.setNotAvailableBecauseAuthenticationIsRequired(
          currentConnections);
    }
    else
    {
      int nConnections = desc.getOpenConnections();
      if (nConnections >= 0)
      {
        Utilities.setTextValue(currentConnections,
            String.valueOf(nConnections));
      }
      else
      {
        Utilities.setNotAvailable(currentConnections);
      }
    }

    hostName.setText(desc.getHostname());

    Set<DN> rootUsers = desc.getAdministrativeUsers();

    SortedSet<String> sortedRootUsers = new TreeSet<String>();
    for (DN dn : rootUsers)
    {
      try
      {
        sortedRootUsers.add(Utilities.unescapeUtf8(dn.toString()));
      }
      catch (Throwable t)
      {
        throw new IllegalStateException("Unexpected error: "+t, t);
      }
    }

    if (rootUsers.size() > 0)
    {
      String htmlString = "<html>"+Utilities.applyFont(
          Utilities.getStringFromCollection(sortedRootUsers, "<br>"),
          administrativeUsers.getFont());
      administrativeUsers.setText(htmlString);
    }
    else
    {
      administrativeUsers.setText(
          INFO_NOT_AVAILABLE_SHORT_LABEL.get().toString());
    }
    File install = desc.getInstallPath();
    if (install != null)
    {
      installPath.setText(install.getAbsolutePath());
    }
    else
    {
      installPath.setText(INFO_NOT_AVAILABLE_SHORT_LABEL.get().toString());
    }

    File instance = desc.getInstancePath();

    if (instance != null)
    {
      instancePath.setText(instance.getAbsolutePath());
    }
    else
    {
      instancePath.setText(INFO_NOT_AVAILABLE_SHORT_LABEL.get().toString());
    }

    boolean sameInstallAndInstance;
    try
    {
      if (instance != null)
      {
        sameInstallAndInstance = instance.getCanonicalFile().equals(install);
      }
      else
      {
        sameInstallAndInstance = install == null;
      }
    }
    catch (IOException ioe)
    {
      // Best effort
      sameInstallAndInstance = instance.getAbsoluteFile().equals(install);
    }
    instancePath.setVisible(!sameInstallAndInstance);
    lInstancePath.setVisible(!sameInstallAndInstance);

    if (desc.getOpenDSVersion() != null)
    {
      opendsVersion.setText(desc.getOpenDSVersion());
    }
    else
    {
      opendsVersion.setText(INFO_NOT_AVAILABLE_SHORT_LABEL.get().toString());
    }

    if (!isRunning)
    {
      if (isLocal)
      {
        Utilities.setNotAvailableBecauseServerIsDown(javaVersion);
      }
      else
      {
        Utilities.setTextValue(javaVersion,
            INFO_NOT_AVAILABLE_SHORT_LABEL.get().toString());
      }
    }
    else if (!isAuthenticated)
    {
      Utilities.setNotAvailableBecauseAuthenticationIsRequired(javaVersion);
    }
    else
    {
      String jVersion = desc.getJavaVersion();
      if (jVersion != null)
      {
        Utilities.setTextValue(javaVersion, jVersion);
      }
      else
      {
        Utilities.setNotAvailable(javaVersion);
      }
    }

    adminConnector.setText(
        getAdminConnectorStringValue(desc.getAdminConnector()));

    Set<BaseDNDescriptor> baseDNs = new HashSet<BaseDNDescriptor>();

    for (BackendDescriptor backend : desc.getBackends())
    {
      if (!backend.isConfigBackend())
      {
        baseDNs.addAll(backend.getBaseDns());
      }
    }

    boolean oneReplicated = false;
    for (BaseDNDescriptor baseDN : baseDNs)
    {
      if (baseDN.getType() == BaseDNDescriptor.Type.REPLICATED)
      {
        oneReplicated = true;
        break;
      }
    }

    boolean hasBaseDNs = baseDNs.size() > 0;

    replicationBaseDNsTable.setVisible(oneReplicated && hasBaseDNs);
    replicationBaseDNsTable.getTableHeader().setVisible(
        oneReplicated && hasBaseDNs);
    noReplicatedBaseDNsTable.setVisible(!oneReplicated && hasBaseDNs);
    noReplicatedBaseDNsTable.getTableHeader().setVisible(
        !oneReplicated && hasBaseDNs);
    dbTableEmpty.setVisible(!hasBaseDNs);

    dbTableModelWithReplication.setData(baseDNs, desc.getStatus(),
        desc.isAuthenticated());
    dbTableModelWithoutReplication.setData(baseDNs, desc.getStatus(),
        desc.isAuthenticated());

    Set<ConnectionHandlerDescriptor> connectionHandlers =
      desc.getConnectionHandlers();
    connectionHandlerTableModel.setData(connectionHandlers);

    boolean hasConnectionHandlers = connectionHandlers.size() > 0;
    connectionHandlersTable.setVisible(hasConnectionHandlers);
    connectionHandlersTable.getTableHeader().setVisible(hasConnectionHandlers);
    connectionHandlerTableEmpty.setVisible(!hasConnectionHandlers);

    recalculateSizes();

    Utilities.updateViewPositions(pos);
  }

  /**
   * Creates the server status subsection panel.
   * @return the server status subsection panel.
   */
  private JPanel createServerStatusPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    p.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(0, 0, 0, 0);

    p.setBorder(Utilities.makeTitledBorder(
        INFO_CTRL_PANEL_SERVER_STATUS_TITLE_BORDER.get()));
    JPanel auxPanel = new JPanel(new GridBagLayout());
    auxPanel.setOpaque(false);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    JLabel l = Utilities.createPrimaryLabel(
        INFO_CTRL_PANEL_SERVER_STATUS_LABEL.get());
    auxPanel.add(l, gbc);

    JPanel statusPanel = new JPanel(new GridBagLayout());
    statusPanel.setOpaque(false);
    gbc.gridwidth = 6;
    gbc.weightx = 0.0;
    serverStatus = Utilities.createDefaultLabel();
    statusPanel.add(serverStatus, gbc);
    gbc.gridwidth--;

    stopButton = Utilities.createButton(INFO_STOP_BUTTON_LABEL.get());
    stopButton.setOpaque(false);
    stopButton.addActionListener(new ActionListener()
    {
      /**
       * {@inheritDoc}
       */
      public void actionPerformed(ActionEvent ev)
      {
        stopServer();
      }
    });
    gbc.insets.left = 10;
    statusPanel.add(stopButton, gbc);

    gbc.gridwidth--;
    gbc.insets.left = 10;
    startButton = Utilities.createButton(INFO_START_BUTTON_LABEL.get());
    startButton.setOpaque(false);
    statusPanel.add(startButton, gbc);
    startButton.addActionListener(new ActionListener()
    {
      /**
       * {@inheritDoc}
       */
      public void actionPerformed(ActionEvent ev)
      {
        startServer();
      }
    });

    gbc.gridwidth--;
    gbc.insets.left = 5;
    restartButton = Utilities.createButton(INFO_RESTART_BUTTON_LABEL.get());
    restartButton.setOpaque(false);
    restartButton.addActionListener(new ActionListener()
    {
      /**
       * {@inheritDoc}
       */
      public void actionPerformed(ActionEvent ev)
      {
        restartServer();
      }
    });
    statusPanel.add(restartButton, gbc);

    gbc.gridwidth = GridBagConstraints.RELATIVE;
    gbc.weightx = 1.0;
    gbc.insets.left = 0;
    statusPanel.add(Box.createHorizontalGlue(), gbc);

    int maxButtonHeight = 0;
    maxButtonHeight = Math.max(maxButtonHeight,
        (int)startButton.getPreferredSize().getHeight());
    maxButtonHeight = Math.max(maxButtonHeight,
        (int)restartButton.getPreferredSize().getHeight());
    maxButtonHeight = Math.max(maxButtonHeight,
        (int)stopButton.getPreferredSize().getHeight());

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 0.0;
    statusPanel.add(Box.createVerticalStrut(maxButtonHeight), gbc);

    gbc.weightx = 1.0;
    gbc.insets.left = 5;
    auxPanel.add(statusPanel, gbc);

    gbc.insets.left = 0;
    gbc.weightx = 0.0;
    gbc.insets.top = 5;
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    l = Utilities.createPrimaryLabel(
        INFO_CTRL_PANEL_OPEN_CONNECTIONS_LABEL.get());
    auxPanel.add(l, gbc);
    currentConnections = new LabelWithHelpIcon(Message.EMPTY, null);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets.left = 5;
    auxPanel.add(currentConnections, gbc);

    gbc.insets.top = 2;
    gbc.insets.right = 5;
    gbc.insets.left = 5;
    gbc.insets.bottom = 5;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    p.add(auxPanel, gbc);

    restartButton.setVisible(false);
    stopButton.setVisible(false);
    return p;
  }


  /**
   * Creates the server details subsection panel.
   * @return the server details subsection panel.
   */
  private JPanel createServerDetailsPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    p.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;

    p.setBorder(Utilities.makeTitledBorder(
        INFO_CTRL_PANEL_SERVER_DETAILS_TITLE_BORDER.get()));
    JPanel auxPanel = new JPanel(new GridBagLayout());
    auxPanel.setOpaque(false);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.weightx = 0.0;
    JLabel[] leftLabels =
      {
        Utilities.createPrimaryLabel(
            INFO_CTRL_PANEL_HOST_NAME_LABEL.get()),
        Utilities.createPrimaryLabel(
            INFO_CTRL_PANEL_ADMINISTRATIVE_USERS_LABEL.get()),
        Utilities.createPrimaryLabel(
            INFO_CTRL_PANEL_INSTALLATION_PATH_LABEL.get()),
        Utilities.createPrimaryLabel(
            INFO_CTRL_PANEL_INSTANCE_PATH_LABEL.get()),
        Utilities.createPrimaryLabel(
            INFO_CTRL_PANEL_OPENDS_VERSION_LABEL.get()),
        Utilities.createPrimaryLabel(
            INFO_CTRL_PANEL_JAVA_VERSION_LABEL.get()),
        Utilities.createPrimaryLabel(
            INFO_CTRL_PANEL_ADMIN_CONNECTOR_LABEL.get())
      };
    lInstancePath = leftLabels[3];

    hostName = Utilities.createDefaultLabel();
    administrativeUsers = Utilities.createDefaultLabel();
    installPath = Utilities.createDefaultLabel();
    instancePath = Utilities.createDefaultLabel();
    opendsVersion = Utilities.createDefaultLabel();
    javaVersion = new LabelWithHelpIcon(Message.EMPTY, null);
    adminConnector = Utilities.createDefaultLabel();

    JComponent[] rightLabels =
      {
        hostName, administrativeUsers, installPath, instancePath, opendsVersion,
        javaVersion, adminConnector
      };


    for (int i=0; i<leftLabels.length; i++)
    {
      gbc.insets.left = 0;
      if (i != 0)
      {
        gbc.insets.top = 10;
      }
      gbc.gridwidth = GridBagConstraints.RELATIVE;
      auxPanel.add(leftLabels[i], gbc);

      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.insets.left = 5;
      auxPanel.add(rightLabels[i], gbc);
    }

    gbc.insets.top = 2;
    gbc.insets.right = 5;
    gbc.insets.left = 5;
    gbc.insets.bottom = 5;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    p.add(auxPanel, gbc);

    return p;
  }

  /**
   * Creates the server listeners subsection panel.
   * @return the server listeners subsection panel.
   */
  private JPanel createListenersPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    p.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel l = Utilities.createTitleLabel(
        INFO_CTRL_PANEL_CONNECTION_HANDLERS.get());
    p.add(l, gbc);

    connectionHandlerTableModel = new ConnectionHandlerTableModel();
    connectionHandlersTable =
      Utilities.createSortableTable(connectionHandlerTableModel,
        new CustomCellRenderer());

    gbc.insets.top = 5;
    p.add(connectionHandlersTable.getTableHeader(), gbc);
    gbc.insets.top = 0;
    p.add(connectionHandlersTable, gbc);

    connectionHandlerTableEmpty =
      Utilities.createPrimaryLabel(
          INFO_CTRL_PANEL_NO_CONNECTION_HANDLER_FOUND.get());
    connectionHandlerTableEmpty.setHorizontalAlignment(SwingConstants.CENTER);
    gbc.insets.top = 5;
    p.add(connectionHandlerTableEmpty, gbc);
    connectionHandlerTableEmpty.setVisible(false);

    return p;
  }

  /**
   * Creates the server backends subsection panel.
   * @return the server backends subsection panel.
   */
  private JPanel createBackendsPanel()
  {
    JPanel p = new JPanel(new GridBagLayout());
    p.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel l = Utilities.createTitleLabel(INFO_CTRL_PANEL_DATA_SOURCES.get());
    p.add(l, gbc);

    dbTableModelWithReplication = new BaseDNTableModel(true);
    dbTableModelWithoutReplication = new BaseDNTableModel(false);
    noReplicatedBaseDNsTable =
      Utilities.createSortableTable(dbTableModelWithoutReplication,
        new BaseDNCellRenderer());
    noReplicatedBaseDNsTable.setVisible(false);
    noReplicatedBaseDNsTable.getTableHeader().setVisible(false);
    replicationBaseDNsTable =
      Utilities.createSortableTable(dbTableModelWithReplication,
        new BaseDNCellRenderer());

    gbc.insets.top = 5;
    p.add(noReplicatedBaseDNsTable.getTableHeader(), gbc);
    gbc.insets.top = 0;
    p.add(noReplicatedBaseDNsTable, gbc);

    gbc.insets.top = 5;
    p.add(replicationBaseDNsTable.getTableHeader(), gbc);
    gbc.insets.top = 0;
    p.add(replicationBaseDNsTable, gbc);
    replicationBaseDNsTable.setVisible(true);
    replicationBaseDNsTable.getTableHeader().setVisible(true);

    gbc.insets.top = 5;
    dbTableEmpty = Utilities.createPrimaryLabel(
        INFO_CTRL_PANEL_NO_DATA_SOURCES_FOUND.get());
    dbTableEmpty.setHorizontalAlignment(SwingConstants.CENTER);
    gbc.anchor = GridBagConstraints.CENTER;
    p.add(dbTableEmpty, gbc);
    dbTableEmpty.setVisible(false);
    return p;
  }

  /**
   * Returns an string representation of the administration connector.
   * @param adminConnector the administration connector.
   * @return an string representation of the administration connector.
   */
  private static String getAdminConnectorStringValue(
      ConnectionHandlerDescriptor adminConnector)
  {
    if (adminConnector != null)
    {
      return INFO_CTRL_PANEL_ADMIN_CONNECTOR_DESCRIPTION.get(
          adminConnector.getPort()).toString();
    }
    else
    {
      return INFO_NOT_AVAILABLE_SHORT_LABEL.get().toString();
    }
  }

  private String getStatusLabel(ServerDescriptor desc)
  {
    Message status;
    switch (desc.getStatus())
    {
    case STARTED:
      status = INFO_SERVER_STARTED_LABEL.get();
      break;

    case STOPPED:
      status = INFO_SERVER_STOPPED_LABEL.get();
      break;

    case STARTING:
      status = INFO_SERVER_STARTING_LABEL.get();
      break;

    case STOPPING:
      status = INFO_SERVER_STOPPING_LABEL.get();
      break;

    case NOT_CONNECTED_TO_REMOTE:
      status = INFO_SERVER_NOT_CONNECTED_TO_REMOTE_STATUS_LABEL.get();
      break;

    case UNKNOWN:
      status = INFO_SERVER_UNKNOWN_STATUS_LABEL.get();
      break;

    default:
      throw new IllegalStateException("Unknown status: "+desc.getStatus());
    }
    return status.toString();
  }
}

