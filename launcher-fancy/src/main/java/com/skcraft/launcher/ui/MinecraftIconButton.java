/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.ui;

import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * An icon-only button with hover effects, styled for the Minecraft-themed launcher.
 */
@Log
public class MinecraftIconButton extends JButton {

    private static final Color BACKGROUND_NORMAL = new Color(60, 60, 60, 200);
    private static final Color BACKGROUND_HOVER = new Color(80, 80, 80, 220);
    private static final Color BACKGROUND_PRESSED = new Color(40, 40, 40, 220);
    private static final Color BORDER_COLOR = new Color(100, 100, 100, 150);

    private BufferedImage iconImage;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean transparentBackground = false;

    public MinecraftIconButton(String iconPath) {
        this(iconPath, null, false);
    }

    public MinecraftIconButton(String iconPath, String tooltip) {
        this(iconPath, tooltip, false);
    }

    public MinecraftIconButton(String iconPath, String tooltip, boolean transparentBackground) {
        super();
        this.transparentBackground = transparentBackground;
        loadIcon(iconPath);
        if (tooltip != null) {
            setToolTipText(tooltip);
        }
        init();
    }

    /**
     * Set whether this button should have a transparent background (icon only).
     */
    public void setTransparentBackground(boolean transparent) {
        this.transparentBackground = transparent;
        repaint();
    }

    private static final int ICON_SIZE = 24; // Target icon size

    private void loadIcon(String iconPath) {
        try (InputStream is = getClass().getResourceAsStream(iconPath)) {
            if (is != null) {
                BufferedImage original = ImageIO.read(is);
                // Scale icon to fit button if needed
                if (original.getWidth() != ICON_SIZE || original.getHeight() != ICON_SIZE) {
                    iconImage = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = iconImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(original, 0, 0, ICON_SIZE, ICON_SIZE, null);
                    g2d.dispose();
                } else {
                    iconImage = original;
                }
            } else {
                log.warning("Icon not found: " + iconPath);
            }
        } catch (Exception e) {
            log.warning("Failed to load icon: " + iconPath);
        }
    }

    private void init() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Disable Substance painting
        putClientProperty("substancelaf.internal.Widget.buttonNeverPainted", Boolean.TRUE);

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
    public Dimension getPreferredSize() {
        return new Dimension(40, 40);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int arc = 6;

        // Only draw background if not transparent
        if (!transparentBackground) {
            // Draw background based on state
            Color bgColor;
            if (isPressed) {
                bgColor = BACKGROUND_PRESSED;
            } else if (isHovered) {
                bgColor = BACKGROUND_HOVER;
            } else {
                bgColor = BACKGROUND_NORMAL;
            }

            g2d.setColor(bgColor);
            g2d.fillRoundRect(0, 0, width, height, arc, arc);

            // Draw border
            g2d.setColor(BORDER_COLOR);
            g2d.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
        } else if (isHovered || isPressed) {
            // For transparent buttons, show subtle hover effect
            g2d.setColor(new Color(255, 255, 255, isPressed ? 40 : 25));
            g2d.fillRoundRect(0, 0, width, height, arc, arc);
        }

        // Draw icon centered
        if (iconImage != null) {
            int iconX = (width - iconImage.getWidth()) / 2;
            int iconY = (height - iconImage.getHeight()) / 2;
            g2d.drawImage(iconImage, iconX, iconY, null);
        } else {
            // Draw placeholder if no icon
            g2d.setColor(Color.WHITE);
            g2d.fillRect(width / 2 - 8, height / 2 - 8, 16, 16);
        }

        g2d.dispose();
    }
}
