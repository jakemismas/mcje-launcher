/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * A large green play button styled like the official Minecraft launcher.
 */
public class MinecraftPlayButton extends JButton {

    // Green gradient colors (similar to official launcher)
    private static final Color GREEN_TOP = new Color(67, 160, 71);
    private static final Color GREEN_BOTTOM = new Color(46, 125, 50);
    private static final Color GREEN_HOVER_TOP = new Color(85, 180, 89);
    private static final Color GREEN_HOVER_BOTTOM = new Color(56, 142, 60);
    private static final Color GREEN_PRESSED_TOP = new Color(40, 120, 44);
    private static final Color GREEN_PRESSED_BOTTOM = new Color(30, 100, 34);

    private static final Color BORDER_LIGHT = new Color(100, 200, 100, 180);
    private static final Color BORDER_DARK = new Color(20, 80, 20, 180);
    private static final Color TEXT_SHADOW = new Color(30, 80, 30);

    private boolean isHovered = false;
    private boolean isPressed = false;

    public MinecraftPlayButton(String text) {
        super(text);
        init();
    }

    private void init() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(MinecraftFontManager.getLaunchButtonFont(Font.BOLD, 26f));
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
        return new Dimension(420, 100);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        // Disable antialiasing for blocky Minecraft-style edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // Keep text antialiasing for readability
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Choose colors based on state
        Color topColor, bottomColor;
        if (isPressed) {
            topColor = GREEN_PRESSED_TOP;
            bottomColor = GREEN_PRESSED_BOTTOM;
        } else if (isHovered) {
            topColor = GREEN_HOVER_TOP;
            bottomColor = GREEN_HOVER_BOTTOM;
        } else {
            topColor = GREEN_TOP;
            bottomColor = GREEN_BOTTOM;
        }

        // Minecraft-style pixel border thickness
        int borderSize = 3;

        // Draw main button fill with gradient (inside the border)
        GradientPaint gradient = new GradientPaint(0, borderSize, topColor, 0, height - borderSize, bottomColor);
        g2d.setPaint(gradient);
        g2d.fillRect(borderSize, borderSize, width - borderSize * 2, height - borderSize * 2);

        // Draw Minecraft-style 3D border (blocky, no curves)
        // Dark outer edge (black/very dark green)
        g2d.setColor(new Color(20, 40, 20));
        // Top edge
        g2d.fillRect(0, 0, width, borderSize);
        // Bottom edge
        g2d.fillRect(0, height - borderSize, width, borderSize);
        // Left edge
        g2d.fillRect(0, 0, borderSize, height);
        // Right edge
        g2d.fillRect(width - borderSize, 0, borderSize, height);

        // Inner highlight (top-left lighter edge for 3D effect)
        g2d.setColor(BORDER_LIGHT);
        // Top inner highlight
        g2d.fillRect(borderSize, borderSize, width - borderSize * 2, 2);
        // Left inner highlight
        g2d.fillRect(borderSize, borderSize, 2, height - borderSize * 2);

        // Inner shadow (bottom-right darker edge for 3D effect)
        g2d.setColor(BORDER_DARK);
        // Bottom inner shadow
        g2d.fillRect(borderSize, height - borderSize - 2, width - borderSize * 2, 2);
        // Right inner shadow
        g2d.fillRect(width - borderSize - 2, borderSize, 2, height - borderSize * 2);

        // Draw text with shadow - use GlyphVector for precise visual centering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(getFont());
        String text = getText();

        // Create GlyphVector for precise bounds
        GlyphVector gv = getFont().createGlyphVector(g2d.getFontRenderContext(), text);
        Rectangle2D visualBounds = gv.getVisualBounds();

        // Center based on actual visual bounds
        int textX = (int) ((width - visualBounds.getWidth()) / 2 - visualBounds.getX());
        int textY = (int) ((height - visualBounds.getHeight()) / 2 - visualBounds.getY());

        // Text shadow
        g2d.setColor(TEXT_SHADOW);
        g2d.drawString(text, textX + 2, textY + 2);

        // Main text
        g2d.setColor(getForeground());
        g2d.drawString(text, textX, textY);

        g2d.dispose();
    }
}
