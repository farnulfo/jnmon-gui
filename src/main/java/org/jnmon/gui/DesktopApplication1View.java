/*
 * DesktopApplication1View.java
 */
package org.jnmon.gui;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.Task;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import org.jfree.chart.ChartPanel;
import org.jfree.data.xy.XYDataset;
import org.jnmon.NMon;

/**
 * The application's main frame.
 */
public class DesktopApplication1View extends FrameView {

  public DesktopApplication1View(SingleFrameApplication app) {
    super(app);

    initComponents();
    jSectionsList.setListData(new Object[]{});

    // status bar initialization - message timeout, idle icon and busy animation, etc
    ResourceMap resourceMap = getResourceMap();
    int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
    messageTimer = new Timer(messageTimeout, new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        statusMessageLabel.setText("");
      }
    });
    messageTimer.setRepeats(false);
    int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
    for (int i = 0; i < busyIcons.length; i++) {
      busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
    }
    busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
        statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
      }
    });
    idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
    statusAnimationLabel.setIcon(idleIcon);
    progressBar.setVisible(false);

    // connecting action tasks to status bar via TaskMonitor
    TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
    taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

      @Override
      public void propertyChange(java.beans.PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if ("started".equals(propertyName)) {
          if (!busyIconTimer.isRunning()) {
            statusAnimationLabel.setIcon(busyIcons[0]);
            busyIconIndex = 0;
            busyIconTimer.start();
          }
          progressBar.setVisible(true);
          progressBar.setIndeterminate(true);
        } else if ("done".equals(propertyName)) {
          busyIconTimer.stop();
          statusAnimationLabel.setIcon(idleIcon);
          progressBar.setVisible(false);
          progressBar.setValue(0);
        } else if ("message".equals(propertyName)) {
          String text = (String) (evt.getNewValue());
          statusMessageLabel.setText((text == null) ? "" : text);
          messageTimer.restart();
        } else if ("progress".equals(propertyName)) {
          int value = (Integer) (evt.getNewValue());
          progressBar.setVisible(true);
          progressBar.setIndeterminate(false);
          progressBar.setValue(value);
        }
      }
    });
  }

  @Action
  public void showAboutBox() {
    if (aboutBox == null) {
      JFrame mainFrame = DesktopApplication1.getApplication().getMainFrame();
      aboutBox = new DesktopApplication1AboutBox(mainFrame);
      aboutBox.setLocationRelativeTo(mainFrame);
    }
    DesktopApplication1.getApplication().show(aboutBox);
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    mainPanel = new javax.swing.JPanel();
    jSplitPane1 = new javax.swing.JSplitPane();
    jScrollPane2 = new javax.swing.JScrollPane();
    jSectionsList = new javax.swing.JList();
    jPanel1 = new javax.swing.JPanel();
    menuBar = new javax.swing.JMenuBar();
    javax.swing.JMenu fileMenu = new javax.swing.JMenu();
    jMenuItemOpenFile = new javax.swing.JMenuItem();
    javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
    javax.swing.JMenu helpMenu = new javax.swing.JMenu();
    javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
    statusPanel = new javax.swing.JPanel();
    javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
    statusMessageLabel = new javax.swing.JLabel();
    statusAnimationLabel = new javax.swing.JLabel();
    progressBar = new javax.swing.JProgressBar();

    mainPanel.setName("mainPanel"); // NOI18N

    jSplitPane1.setName("jSplitPane1"); // NOI18N

    jScrollPane2.setName("jScrollPane2"); // NOI18N

    jSectionsList.setModel(new javax.swing.AbstractListModel() {
      String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
      public int getSize() { return strings.length; }
      public Object getElementAt(int i) { return strings[i]; }
    });
    jSectionsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    jSectionsList.setName("jSectionsList"); // NOI18N
    jSectionsList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
      public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
        jSectionsListValueChanged(evt);
      }
    });
    jScrollPane2.setViewportView(jSectionsList);

    jSplitPane1.setLeftComponent(jScrollPane2);

    jPanel1.setName("jPanel1"); // NOI18N

    org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(0, 304, Short.MAX_VALUE)
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(0, 197, Short.MAX_VALUE)
    );

    jSplitPane1.setRightComponent(jPanel1);

    org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
    mainPanel.setLayout(mainPanelLayout);
    mainPanelLayout.setHorizontalGroup(
      mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(mainPanelLayout.createSequentialGroup()
        .addContainerGap()
        .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
        .addContainerGap())
    );
    mainPanelLayout.setVerticalGroup(
      mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(mainPanelLayout.createSequentialGroup()
        .addContainerGap()
        .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
        .addContainerGap())
    );

    menuBar.setName("menuBar"); // NOI18N

    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.jnmon.gui.DesktopApplication1.class).getContext().getResourceMap(DesktopApplication1View.class);
    fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
    fileMenu.setName("fileMenu"); // NOI18N

    javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.jnmon.gui.DesktopApplication1.class).getContext().getActionMap(DesktopApplication1View.class, this);
    jMenuItemOpenFile.setAction(actionMap.get("LoadNMONFile")); // NOI18N
    jMenuItemOpenFile.setText(resourceMap.getString("jMenuItemOpenFile.text")); // NOI18N
    jMenuItemOpenFile.setName("jMenuItemOpenFile"); // NOI18N
    fileMenu.add(jMenuItemOpenFile);

    exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
    exitMenuItem.setName("exitMenuItem"); // NOI18N
    fileMenu.add(exitMenuItem);

    menuBar.add(fileMenu);

    helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
    helpMenu.setName("helpMenu"); // NOI18N

    aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
    aboutMenuItem.setName("aboutMenuItem"); // NOI18N
    helpMenu.add(aboutMenuItem);

    menuBar.add(helpMenu);

    statusPanel.setName("statusPanel"); // NOI18N

    statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

    statusMessageLabel.setName("statusMessageLabel"); // NOI18N

    statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

    progressBar.setName("progressBar"); // NOI18N

    org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
    statusPanel.setLayout(statusPanelLayout);
    statusPanelLayout.setHorizontalGroup(
      statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
      .add(statusPanelLayout.createSequentialGroup()
        .addContainerGap()
        .add(statusMessageLabel)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 204, Short.MAX_VALUE)
        .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(statusAnimationLabel)
        .addContainerGap())
    );
    statusPanelLayout.setVerticalGroup(
      statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(statusPanelLayout.createSequentialGroup()
        .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
          .add(statusMessageLabel)
          .add(statusAnimationLabel)
          .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        .add(3, 3, 3))
    );

    setComponent(mainPanel);
    setMenuBar(menuBar);
    setStatusBar(statusPanel);
  }// </editor-fold>//GEN-END:initComponents

  private void jSectionsListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jSectionsListValueChanged
    // TODO add your handling code here:
    boolean isAdjusting = evt.getValueIsAdjusting();
    if (!isAdjusting) {
      ListSelectionModel lsm = jSectionsList.getSelectionModel();
      int firstIndex = lsm.getMinSelectionIndex();
      String sectionName = (String) jSectionsList.getModel().getElementAt(firstIndex);
      Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, sectionName);

      if ("LPAR".equals(sectionName)) {
        ChartPanel chartPanel = nmon.getLPARChartPanel();
        JTabbedPane jTabbedPane = new JTabbedPane();
        jTabbedPane.addTab("LPAR", chartPanel);
        jTabbedPane.addTab("CPU% vs VPs", nmon.getLPAR2ChartPanel());
        jTabbedPane.addTab("Share Pool Utilisation", nmon.getSharePoolUtilisation());
        jSplitPane1.setRightComponent(jTabbedPane);
      } else if ("MEM".equals(sectionName)) {
        JTabbedPane jTabbedPane = new JTabbedPane();
        jTabbedPane.addTab("Memory", nmon.getMemoryRealFree());
        jSplitPane1.setRightComponent(jTabbedPane);
      } else {
        List<String> data = nmon.getSection(sectionName);
        StringBuilder sb = new StringBuilder();
        for (String line : data) {
          sb.append(line);
          sb.append("\n");
        }
        JTextArea jTextArea = new JTextArea();
        jTextArea.setText(sb.toString());
        JScrollPane jScrollPane = new JScrollPane(jTextArea);
        jSplitPane1.setRightComponent(jScrollPane);
      }
    }
  }//GEN-LAST:event_jSectionsListValueChanged

  @Action(block = Task.BlockingScope.APPLICATION)
  public Task LoadNMONFile() {
    return new LoadNMONFileTask(getApplication());
  }

  private class LoadNMONFileTask extends org.jdesktop.application.Task<Object, Void> {

    File file = null;

    LoadNMONFileTask(org.jdesktop.application.Application app) {
      // Runs on the EDT.  Copy GUI state that
      // doInBackground() depends on from parameters
      // to LoadNMONFileTask fields, here.
      super(app);
      JFileChooser jFileChooser = new JFileChooser();
      int returnVal = jFileChooser.showOpenDialog(jPanel1);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        file = jFileChooser.getSelectedFile();
      }
    }

    @Override
    protected Object doInBackground() {
      if (file == null) {
        return null;
      }
      Object result = null;
      try {
        // Your Task's code here.  This method runs
        // on a background thread, so don't reference
        // the Swing GUI from here.
        nmon = new NMon(file);
        result = nmon.createLPARDataSet();
      } catch (IOException ex) {
        Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
      } catch (ParseException ex) {
        Logger.getLogger(DesktopApplication1View.class.getName()).log(Level.SEVERE, null, ex);
      }

      return result; // return your result
    }

    @Override
    protected void succeeded(Object result) {
      // Runs on the EDT.  Update the GUI based on
      // the result computed by doInBackground().
      if (result == null) {
        return;
      }
      jSectionsList.setListData(nmon.getSections().toArray());
    }
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JMenuItem jMenuItemOpenFile;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JList jSectionsList;
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JPanel mainPanel;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JProgressBar progressBar;
  private javax.swing.JLabel statusAnimationLabel;
  private javax.swing.JLabel statusMessageLabel;
  private javax.swing.JPanel statusPanel;
  // End of variables declaration//GEN-END:variables
  private final Timer messageTimer;
  private final Timer busyIconTimer;
  private final Icon idleIcon;
  private final Icon[] busyIcons = new Icon[15];
  private int busyIconIndex = 0;
  private JDialog aboutBox;
  private NMon nmon = null;
}
