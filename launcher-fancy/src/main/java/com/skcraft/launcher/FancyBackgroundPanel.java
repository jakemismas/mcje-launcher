/*
 * SKCraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher;

import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * A background panel that displays a static image, scaled to fill the panel.
 */
@Log
public class FancyBackgroundPanel extends JPanel {

    private static final Color FALLBACK_COLOR = new Color(30, 30, 30);
    private BufferedImage backgroundImage;

    public FancyBackgroundPanel() {
        setOpaque(true);
        loadBackgroundImage();
    }

    private void loadBackgroundImage() {
        // Try multiple possible file extensions
        String[] possiblePaths = {
            "/com/skcraft/launcher/mcje-background.png",
            "/com/skcraft/launcher/mcje-background.jpg",
            "/com/skcraft/launcher/mcje-background.jpeg"
        };

        for (String path : possiblePaths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    backgroundImage = ImageIO.read(is);
                    if (backgroundImage != null) {
                        log.info("Loaded background image: " + path);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warning("Failed to load background: " + path);
            }
        }

        log.warning("Background image not found, using fallback color");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        int width = getWidth();
        int height = getHeight();

        if (backgroundImage != null) {
            // Scale image to fill panel while maintaining aspect ratio (cover)
            double imgRatio = (double) backgroundImage.getWidth() / backgroundImage.getHeight();
            double panelRatio = (double) width / height;

            int drawWidth, drawHeight, drawX, drawY;

            if (panelRatio > imgRatio) {
                // Panel is wider - fit width, crop height
                drawWidth = width;
                drawHeight = (int) (width / imgRatio);
                drawX = 0;
                drawY = (height - drawHeight) / 2;
            } else {
                // Panel is taller - fit height, crop width
                drawHeight = height;
                drawWidth = (int) (height * imgRatio);
                drawX = (width - drawWidth) / 2;
                drawY = 0;
            }

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(backgroundImage, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            // Fallback to solid color
            g2d.setColor(FALLBACK_COLOR);
            g2d.fillRect(0, 0, width, height);
        }

        g2d.dispose();
    }
}
