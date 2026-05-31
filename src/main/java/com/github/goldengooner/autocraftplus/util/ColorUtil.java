package com.github.goldengooner.autocraftplus.util;

import org.bukkit.ChatColor;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");

    /**
     * Translates message strings containing legacy color codes, HEX colors, and gradients.
     *
     * @param message the message to translate
     * @return the colored and formatted message
     */
    public static String translate(String message) {
        if (message == null) return null;

        // 1. Translate Gradients
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (gradientMatcher.find()) {
            String color1Str = gradientMatcher.group(1);
            String color2Str = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            gradientMatcher.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(text, color1Str, color2Str)));
        }
        gradientMatcher.appendTail(sb);
        message = sb.toString();

        // 2. Translate HEX colors (&#AABBCC)
        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        StringBuffer hexSb = new StringBuffer();
        while (hexMatcher.find()) {
            String hexCode = hexMatcher.group(1);
            StringBuilder replacement = new StringBuilder(String.valueOf(ChatColor.COLOR_CHAR) + "x");
            for (char c : hexCode.toCharArray()) {
                replacement.append(ChatColor.COLOR_CHAR).append(c);
            }
            hexMatcher.appendReplacement(hexSb, replacement.toString());
        }
        hexMatcher.appendTail(hexSb);
        message = hexSb.toString();

        // 3. Translate legacy colors (&)
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String applyGradient(String text, String color1, String color2) {
        Color c1 = Color.decode(color1);
        Color c2 = Color.decode(color2);
        int length = text.length();
        if (length == 0) return "";
        if (length == 1) return getHexChar(text.charAt(0), c1);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
            int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
            int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
            builder.append(getHexChar(text.charAt(i), new Color(r, g, b)));
        }
        return builder.toString();
    }

    private static String getHexChar(char c, Color color) {
        String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        StringBuilder builder = new StringBuilder(String.valueOf(ChatColor.COLOR_CHAR) + "x");
        for (char h : hex.toCharArray()) {
            builder.append(ChatColor.COLOR_CHAR).append(h);
        }
        builder.append(c);
        return builder.toString();
    }
}
