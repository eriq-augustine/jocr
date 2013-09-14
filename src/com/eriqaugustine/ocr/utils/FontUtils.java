package com.eriqaugustine.ocr.utils;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class FontUtils {
   public static final int DEFAULT_FONT_SIZE = 24;

   public static final String[] FONTS = new String[]{
      "Baekmuk Batang",
      "Bitstream Vera Serif",
      "IPAGothic",
      "IPAMincho",
      "NanumMyeongjo",
   };

   public static void main(String[] args) {
      /*
      String text = "アンタにこのセンスは わからないわ";

      for (String font : FONTS) {
         showFontDialog(String.format("%s:\n%s", font, text), font);
      }
      */

      //getFontName("fonts/DFMimP5_0.ttf");
      //getFontName("fonts/DFMimP5_0.ttf");
      getFontName("/home/eriq/jocr/fonts/A-OTF-MidashiMinPr5-MA31.ttf");
   }

   public static void getFontName(String fontPath) {
      Set<String> initialFonts = new HashSet<String>(Arrays.asList(getAvailableFonts()));
      if (!registerFont(fontPath)) {
         System.out.println("Unable to register font: " + fontPath);
         return;
      }
      Set<String> newFonts = new HashSet<String>(Arrays.asList(getAvailableFonts()));

      newFonts.removeAll(initialFonts);

      System.out.println("New Fonts:");
      for (String font : newFonts) {
         System.out.println("   " + font);
      }
   }

   public static void showFontDialog(String str, String fontName) {
      showFontDialog(str, fontName, DEFAULT_FONT_SIZE);
   }

   public static void showFontDialog(String str, String fontName, int fontSize) {
      Font font = new Font(fontName, Font.PLAIN, 24);

      JLabel label = new JLabel(str);
      label.setFont(font);
      JOptionPane.showMessageDialog(null, label);
   }

   public static boolean registerFont(String fontPath) {
      try {
         //TEST
         File file = new File(fontPath);
         System.out.println("Exists: " + file.exists());

         GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
         return ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File(fontPath)));
      } catch (Exception ex) {
         // TODO(eriq): Better logging.
         ex.printStackTrace(System.err);
         return false;
      }
   }

   public static String[] getAvailableFonts() {
      GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
      return env.getAvailableFontFamilyNames();
   }

   public static void printAvailableFonts() {
      String[] fonts = getAvailableFonts();
      System.out.println("Available Fonts:");
      for (String font : fonts) {
         System.out.println("   " + font);
      }
   }
}
