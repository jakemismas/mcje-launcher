/*
 * SKCraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher;

import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.launcher.dialog.ConfigurationDialog;
import com.skcraft.launcher.dialog.ProgressDialog;
import com.skcraft.launcher.launch.LaunchListener;
import com.skcraft.launcher.launch.LaunchOptions;
import com.skcraft.launcher.launch.LaunchOptions.UpdatePolicy;
import com.skcraft.launcher.swing.*;
import com.skcraft.launcher.ui.*;
import com.skcraft.launcher.util.SharedLocale;
import com.skcraft.launcher.util.SwingExecutor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;

import static com.skcraft.launcher.util.SharedLocale.tr;

/**
 * A Minecraft-themed launcher frame with screenshot carousel background,
 * modpack list on the left, and centered play button.
 */
@Log
public class FancyLauncherFrame extends JFrame {

    private static final int SIDEBAR_WIDTH_EXPANDED = 234;
    private static final int SIDEBAR_WIDTH_COLLAPSED = 92;
    private static final int BUTTON_BAR_HEIGHT = 80;
    private static final int SIDEBAR_PADDING = 6;
    private static final int SIDEBAR_ANIMATION_STEP = 15;
    private static final int SIDEBAR_ANIMATION_DELAY = 10;

    private final Launcher launcher;

    @Getter
    private final InstanceTable instancesTable = new InstanceTable();
    private final InstanceTableModel instancesModel;
    @Getter
    private final JScrollPane instanceScroll = new JScrollPane(instancesTable);

    private JPanel backgroundPanel;

    private MinecraftPlayButton launchButton;
    private MinecraftIconButton selfUpdateButton;
    private JButton settingsButton;

    private JLayeredPane layeredPane;
    private Timer autoRefreshTimer;
    private Timer sidebarAnimationTimer;
    private int currentSidebarWidth = SIDEBAR_WIDTH_COLLAPSED;
    private boolean sidebarExpanded = false;

    public FancyLauncherFrame(@NonNull Launcher launcher) {
        super(tr("launcher.title", launcher.getVersion()));

        this.launcher = launcher;
        instancesModel = new InstanceTableModel(launcher.getInstances());

        // Pre-load fonts before UI renders to avoid delay
        MinecraftFontManager.preload();
        UIFontManager.preload();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 560);
        setMinimumSize(new Dimension(700, 460));
        setResizable(false);
        setLocationRelativeTo(null);

        // Explicitly disable LAF window decorations - use native Windows title bar
        getRootPane().setWindowDecorationStyle(JRootPane.NONE);

        SwingHelper.setFrameIcon(this, Launcher.class, "icon.png");

        initComponents();
        startAutoRefresh();

        // Enable dark title bar on Windows
        WindowsDarkTitleBar.enable(this);

        SwingUtilities.invokeLater(this::loadInstances);
    }

    private void startAutoRefresh() {
        // Auto-refresh instance list every 60 seconds
        autoRefreshTimer = new Timer(60000, e -> {
            // Silent refresh - don't show progress dialog
            // Remember current selection before refresh
            int selectedRow = instancesTable.getSelectedRow();
            launcher.getInstanceTasks().reloadInstances(this).addListener(() -> {
                instancesModel.update();
                // Restore selection after refresh (or select first if previous selection invalid)
                if (instancesTable.getRowCount() > 0) {
                    int rowToSelect = (selectedRow >= 0 && selectedRow < instancesTable.getRowCount())
                            ? selectedRow : 0;
                    instancesTable.setRowSelectionInterval(rowToSelect, rowToSelect);
                    instancesTable.repaint();
                }
            }, SwingExecutor.INSTANCE);
        });
        autoRefreshTimer.start();
    }

    private JPanel contentPanel;
    private JPanel controlsPanel;
    private JPanel sidebar;
    private JPanel buttonBar;

    private void initComponents() {
        // Create the layered pane for z-ordering
        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null); // We'll handle positioning manually
        setContentPane(layeredPane);

        // Layer 0: Static background image
        backgroundPanel = new FancyBackgroundPanel();
        layeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);

        // Layer 1: Content panel (modpack list)
        contentPanel = createContentPanel();
        layeredPane.add(contentPanel, JLayeredPane.PALETTE_LAYER);

        // Layer 2: Controls (window controls + button bar)
        controlsPanel = createControlsPanel();
        layeredPane.add(controlsPanel, JLayeredPane.MODAL_LAYER);

        // Handle resizing - this also sets initial bounds
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateLayout();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                updateLayout();
            }
        });

        // Set initial bounds immediately (don't wait for resize event)
        SwingUtilities.invokeLater(this::updateLayout);

        // Setup table
        instancesTable.setModel(instancesModel);
        instancesTable.setRowHeight(ModpackCellRenderer.getRowHeight());
        ModpackCellRenderer cellRenderer = new ModpackCellRenderer();
        instancesTable.setDefaultRenderer(Object.class, cellRenderer);
        instancesTable.setDefaultRenderer(String.class, cellRenderer);
        instancesTable.setDefaultRenderer(ImageIcon.class, cellRenderer);
        instancesTable.setShowGrid(false);
        instancesTable.setIntercellSpacing(new Dimension(0, 4));
        instancesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instancesTable.setTableHeader(null); // Hide table header
        // Hide icon column (column 0) - we render everything in column 1
        instancesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        instancesTable.getColumnModel().getColumn(0).setMinWidth(0);
        instancesTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        SwingHelper.removeOpaqueness(instancesTable);
        SwingHelper.removeOpaqueness(instanceScroll);
        instanceScroll.setBorder(BorderFactory.createEmptyBorder());
        instanceScroll.getViewport().setOpaque(false);

        // Double-click to launch
        instancesTable.addMouseListener(new DoubleClickToButtonAdapter(launchButton));

        // Select first row when data loads
        instancesModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (instancesTable.getRowCount() > 0) {
                    instancesTable.setRowSelectionInterval(0, 0);
                    instancesTable.repaint(); // Ensure selection border is drawn
                }
            }
        });

        // Right-click context menu
        instancesTable.addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(MouseEvent e) {
                int index = instancesTable.rowAtPoint(e.getPoint());
                Instance selected = null;
                if (index >= 0) {
                    instancesTable.setRowSelectionInterval(index, index);
                    selected = launcher.getInstances().get(index);
                }
                popupInstanceMenu(e.getComponent(), e.getX(), e.getY(), selected);
            }
        });

        // Track pressed state for visual feedback (inverted border effect)
        instancesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = instancesTable.rowAtPoint(e.getPoint());
                ModpackCellRenderer.setPressedRow(row);
                instancesTable.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ModpackCellRenderer.setPressedRow(-1);
                instancesTable.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ModpackCellRenderer.setPressedRow(-1);
                instancesTable.repaint();
            }
        });
    }

    private void updateLayout() {
        Dimension size = getContentPane().getSize();
        if (size.width == 0 || size.height == 0) {
            // Use frame size minus insets if content pane has no size yet
            Insets insets = getInsets();
            size = new Dimension(getWidth() - insets.left - insets.right,
                                 getHeight() - insets.top - insets.bottom);
        }
        if (size.width <= 0 || size.height <= 0) {
            return; // Can't layout with no size
        }

        // Update layered pane children
        backgroundPanel.setBounds(0, 0, size.width, size.height);
        contentPanel.setBounds(0, 0, size.width, size.height);
        controlsPanel.setBounds(0, 0, size.width, size.height);

        // Update sidebar with current animated width (extends to bottom)
        int sidebarHeight = size.height - 20;
        if (sidebar != null && sidebarHeight > 0) {
            sidebar.setBounds(10, 10, currentSidebarWidth, sidebarHeight);
        }

        // Update button bar
        if (buttonBar != null) {
            buttonBar.setBounds(0, size.height - BUTTON_BAR_HEIGHT, size.width, BUTTON_BAR_HEIGHT);
            updateButtonBarLayout();
        }

        // Force repaint
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private void updateButtonBarLayout() {
        if (buttonBar == null) return;
        int width = buttonBar.getWidth();
        int height = buttonBar.getHeight();
        if (width <= 0 || height <= 0) return;

        int centerY = (height - 40) / 2;
        int playBtnWidth = 300;
        int playBtnHeight = 70;

        // Center play button
        int playX = (width - playBtnWidth) / 2;
        int playY = (height - playBtnHeight) / 2;
        launchButton.setBounds(playX, playY, playBtnWidth, playBtnHeight);

        // Right buttons
        selfUpdateButton.setBounds(width - 60, centerY, 40, 40);
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            public boolean isOpaque() {
                return false;
            }
        };

        // Left sidebar with modpack list (light gray background)
        sidebar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(60, 60, 60, 200));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        sidebar.setOpaque(false);
        // Add padding inside sidebar between background edge and content
        sidebar.setBorder(BorderFactory.createEmptyBorder(SIDEBAR_PADDING, SIDEBAR_PADDING, SIDEBAR_PADDING, SIDEBAR_PADDING));
        sidebar.add(instanceScroll, BorderLayout.CENTER);

        // Settings button at the bottom of sidebar
        settingsButton = createSettingsButton();
        sidebar.add(settingsButton, BorderLayout.SOUTH);

        // Add hover listeners for sidebar animation
        MouseAdapter sidebarHoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                expandSidebar();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Check if mouse is still within sidebar bounds
                Point p = e.getPoint();
                SwingUtilities.convertPointToScreen(p, e.getComponent());
                Point sidebarLoc = sidebar.getLocationOnScreen();
                Rectangle sidebarBounds = new Rectangle(sidebarLoc.x, sidebarLoc.y,
                        sidebar.getWidth(), sidebar.getHeight());
                if (!sidebarBounds.contains(p)) {
                    collapseSidebar();
                }
            }
        };
        sidebar.addMouseListener(sidebarHoverAdapter);
        instancesTable.addMouseListener(sidebarHoverAdapter);
        instanceScroll.addMouseListener(sidebarHoverAdapter);
        settingsButton.addMouseListener(sidebarHoverAdapter);

        panel.add(sidebar);

        return panel;
    }

    private JButton createSettingsButton() {
        JButton button = new JButton("Settings") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Draw hover/pressed background
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(60, 60, 60, 220));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(50, 50, 50, 220));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }

                // Draw icon (52x52 to match modpack icons)
                Icon icon = getIcon();
                if (icon != null) {
                    int iconX = (getWidth() - icon.getIconWidth()) / 2; // Center when collapsed
                    if (getWidth() > 100) { // Expanded - left align
                        iconX = 10;
                    }
                    int iconY = (getHeight() - icon.getIconHeight()) / 2;
                    icon.paintIcon(this, g2d, iconX, iconY);
                }

                // Only draw text when expanded (width > 100)
                if (getWidth() > 100) {
                    g2d.setFont(getFont());
                    g2d.setColor(getForeground());
                    FontMetrics fm = g2d.getFontMetrics();
                    int textX = 70; // 10 + 52 + 8 padding
                    int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(getText(), textX, textY);
                }

                g2d.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(currentSidebarWidth, 72); // Match modpack row height for 64x64 icon
            }
        };

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(Color.WHITE);
        button.setFont(MinecraftFontManager.getMinecraftFont(Font.PLAIN, 14f));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);

        // Load settings icon (52x52 to match modpack icons)
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/com/skcraft/launcher/icons/options.png"));
            Image scaled = icon.getImage().getScaledInstance(52, 52, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            log.warning("Failed to load settings icon");
        }

        button.addActionListener(e -> showOptions());

        return button;
    }

    private void expandSidebar() {
        if (sidebarExpanded) return;
        sidebarExpanded = true;

        if (sidebarAnimationTimer != null && sidebarAnimationTimer.isRunning()) {
            sidebarAnimationTimer.stop();
        }

        sidebarAnimationTimer = new Timer(SIDEBAR_ANIMATION_DELAY, e -> {
            if (currentSidebarWidth < SIDEBAR_WIDTH_EXPANDED) {
                currentSidebarWidth = Math.min(currentSidebarWidth + SIDEBAR_ANIMATION_STEP, SIDEBAR_WIDTH_EXPANDED);
                updateLayout();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        sidebarAnimationTimer.start();
    }

    private void collapseSidebar() {
        if (!sidebarExpanded) return;
        sidebarExpanded = false;

        if (sidebarAnimationTimer != null && sidebarAnimationTimer.isRunning()) {
            sidebarAnimationTimer.stop();
        }

        sidebarAnimationTimer = new Timer(SIDEBAR_ANIMATION_DELAY, e -> {
            if (currentSidebarWidth > SIDEBAR_WIDTH_COLLAPSED) {
                currentSidebarWidth = Math.max(currentSidebarWidth - SIDEBAR_ANIMATION_STEP, SIDEBAR_WIDTH_COLLAPSED);
                updateLayout();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        sidebarAnimationTimer.start();
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            public boolean isOpaque() {
                return false;
            }
        };

        // Button bar (bottom)
        buttonBar = createButtonBar();
        panel.add(buttonBar);

        return panel;
    }

    private JPanel createButtonBar() {
        // Transparent button bar (no background)
        JPanel bar = new JPanel(null) {
            @Override
            public boolean isOpaque() {
                return false;
            }
        };

        // Create buttons
        selfUpdateButton = new MinecraftIconButton("/com/skcraft/launcher/icons/update.png",
                SharedLocale.tr("launcher.updateLauncher"));

        launchButton = new MinecraftPlayButton(SharedLocale.tr("launcher.launch"));

        // Self-update visibility
        selfUpdateButton.setVisible(launcher.getUpdateManager().getPendingUpdate());
        launcher.getUpdateManager().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("pendingUpdate")) {
                    selfUpdateButton.setVisible((Boolean) evt.getNewValue());
                }
            }
        });

        // Add buttons
        bar.add(launchButton);
        bar.add(selfUpdateButton);

        // Wire up actions
        selfUpdateButton.addActionListener(e -> {
            launcher.getUpdateManager().performUpdate(FancyLauncherFrame.this);
        });

        launchButton.addActionListener(e -> launch());

        return bar;
    }

    private void loadInstances() {
        ObservableFuture<InstanceList> future = launcher.getInstanceTasks().reloadInstances(this);

        future.addListener(() -> {
            instancesModel.update();
            if (instancesTable.getRowCount() > 0) {
                instancesTable.setRowSelectionInterval(0, 0);
                instancesTable.repaint(); // Ensure selection border is drawn
            }
            requestFocus();
        }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(this, future, SharedLocale.tr("launcher.checkingTitle"),
                SharedLocale.tr("launcher.checkingStatus"));
        SwingHelper.addErrorDialogCallback(this, future);
    }

    private void showOptions() {
        ConfigurationDialog configDialog = new ConfigurationDialog(this, launcher);
        configDialog.setVisible(true);
    }

    private void launch() {
        int selectedRow = instancesTable.getSelectedRow();
        if (selectedRow < 0) return;

        Instance instance = launcher.getInstances().get(selectedRow);

        LaunchOptions options = new LaunchOptions.Builder()
                .setInstance(instance)
                .setListener(new LaunchListenerImpl(this))
                .setUpdatePolicy(UpdatePolicy.UPDATE_IF_SESSION_ONLINE)
                .setWindow(this)
                .build();
        launcher.getLaunchSupervisor().launch(options);
    }

    private void popupInstanceMenu(Component component, int x, int y, final Instance selected) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        if (selected != null) {
            menuItem = new JMenuItem(!selected.isLocal() ? tr("instance.install") : tr("instance.launch"));
            menuItem.addActionListener(e -> launch());
            popup.add(menuItem);

            if (selected.isLocal()) {
                popup.addSeparator();

                menuItem = new JMenuItem(SharedLocale.tr("instance.openFolder"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        FancyLauncherFrame.this, selected.getContentDir(), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openSaves"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        FancyLauncherFrame.this, new File(selected.getContentDir(), "saves"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openResourcePacks"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        FancyLauncherFrame.this, new File(selected.getContentDir(), "resourcepacks"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openScreenshots"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        FancyLauncherFrame.this, new File(selected.getContentDir(), "screenshots"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.copyAsPath"));
                menuItem.addActionListener(e -> {
                    File dir = selected.getContentDir();
                    dir.mkdirs();
                    SwingHelper.setClipboard(dir.getAbsolutePath());
                });
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openSettings"));
                menuItem.addActionListener(e -> {
                    com.skcraft.launcher.dialog.InstanceSettingsDialog.open(this, selected);
                    instancesModel.update(); // Refresh in case icon changed
                });
                popup.add(menuItem);

                popup.addSeparator();

                if (!selected.isUpdatePending()) {
                    menuItem = new JMenuItem(SharedLocale.tr("instance.forceUpdate"));
                    menuItem.addActionListener(e -> {
                        selected.setUpdatePending(true);
                        launch();
                        instancesModel.update();
                    });
                    popup.add(menuItem);
                }

                menuItem = new JMenuItem(SharedLocale.tr("instance.hardForceUpdate"));
                menuItem.addActionListener(e -> confirmHardUpdate(selected));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.deleteFiles"));
                menuItem.addActionListener(e -> confirmDelete(selected));
                popup.add(menuItem);
            }

            popup.addSeparator();
        }

        menuItem = new JMenuItem(SharedLocale.tr("launcher.refreshList"));
        menuItem.addActionListener(e -> loadInstances());
        popup.add(menuItem);

        popup.show(component, x, y);
    }

    private void confirmDelete(Instance instance) {
        if (!SwingHelper.confirmDialog(this,
                tr("instance.confirmDelete", instance.getTitle()), SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().delete(this, instance);
        future.addListener(this::loadInstances, SwingExecutor.INSTANCE);
    }

    private void confirmHardUpdate(Instance instance) {
        if (!SwingHelper.confirmDialog(this, SharedLocale.tr("instance.confirmHardUpdate"),
                SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().hardUpdate(this, instance);
        future.addListener(() -> {
            launch();
            instancesModel.update();
        }, SwingExecutor.INSTANCE);
    }

    @Override
    public void dispose() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        if (sidebarAnimationTimer != null) {
            sidebarAnimationTimer.stop();
        }
        super.dispose();
    }

    private static class LaunchListenerImpl implements LaunchListener {
        private final WeakReference<FancyLauncherFrame> frameRef;
        private final Launcher launcher;

        private LaunchListenerImpl(FancyLauncherFrame frame) {
            this.frameRef = new WeakReference<>(frame);
            this.launcher = frame.launcher;
        }

        @Override
        public void instancesUpdated() {
            FancyLauncherFrame frame = frameRef.get();
            if (frame != null) {
                frame.instancesModel.update();
            }
        }

        @Override
        public void gameStarted() {
            FancyLauncherFrame frame = frameRef.get();
            if (frame != null) {
                frame.dispose();
            }
        }

        @Override
        public void gameClosed() {
            Window newLauncherWindow = launcher.showLauncherWindow();
            launcher.getUpdateManager().checkForUpdate(newLauncherWindow);
        }
    }
}
