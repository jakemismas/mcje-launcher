/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Custom window control panel with minimize and close buttons for undecorated windows.
 * Also handles window dragging.
 */
public class WindowControlPanel extends JPanel {

    private static final Color BUTTON_NORMAL = new Color(60, 60, 60, 200);
    private static final Color BUTTON_HOVER = new Color(80, 80, 80, 220);
    private static final Color BUTTON_CLOSE_HOVER = new Color(200, 60, 60, 220);
    private static final Color BUTTON_PRESSED = new Color(40, 40, 40, 220);

    private final JFrame parentFrame;
    private Point dragOffset;

    public WindowControlPanel(JFrame parent) {
        this.parentFrame = parent;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 2));

        // Minimize button
        WindowButton minimizeBtn = new WindowButton("_", false);
        minimizeBtn.addActionListener(e -> {
            parentFrame.setState(Frame.ICONIFIED);
        });

        // Close button
        WindowButton closeBtn = new WindowButton("X", true);
        closeBtn.addActionListener(e -> {
            parentFrame.dispatchEvent(new WindowEvent(parentFrame, WindowEvent.WINDOW_CLOSING));
        });

        add(minimizeBtn);
        add(closeBtn);

        setupDragListener();
    }

    private void setupDragListener() {
        // Allow dragging the window from this panel
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point currentLocation = parentFrame.getLocation();
                    parentFrame.setLocation(
                            currentLocation.x + e.getX() - dragOffset.x,
                            currentLocation.y + e.getY() - dragOffset.y
                    );
                }
            }
        });
    }

    /**
     * Adds window drag functionality to another component.
     */
    public void addDragSupport(Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });

        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point screenPoint = e.getLocationOnScreen();
                    parentFrame.setLocation(
                            screenPoint.x - dragOffset.x,
                            screenPoint.y - dragOffset.y
                    );
                }
            }
        });
    }

    /**
     * Custom styled button for window controls.
     */
    private static class WindowButton extends JButton {
        private final boolean isCloseButton;
        private boolean isHovered = false;
        private boolean isPressed = false;

        public WindowButton(String text, boolean isCloseButton) {
            super(text);
            this.isCloseButton = isCloseButton;

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(30, 24));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Choose background color
            Color bgColor;
            if (isPressed) {
                bgColor = BUTTON_PRESSED;
            } else if (isHovered) {
                bgColor = isCloseButton ? BUTTON_CLOSE_HOVER : BUTTON_HOVER;
            } else {
                bgColor = BUTTON_NORMAL;
            }

            g2d.setColor(bgColor);
            g2d.fillRoundRect(0, 0, width, height, 4, 4);

            // Draw text
            g2d.setFont(getFont());
            g2d.setColor(getForeground());
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (width - fm.stringWidth(getText())) / 2;
            int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(getText(), textX, textY);

            g2d.dispose();
        }
    }
}
