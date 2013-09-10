package com.eriqaugustine.ocr.utils;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PixelPacket;

import java.awt.GraphicsEnvironment;

/**
 * Utilities for character-based operations.
 */
public class CharacterUtils {
   public static final String DEFAULT_FONT_FAMILY = "IPAGothic";
   public static final String[] FONTS = new String[]{
      // "Baekmuk Batang",
      // "Bitstream Vera Serif",
      "IPAGothic",
      "IPAMincho",
      "NanumMyeongjo",
      // "Nimbus Roman No9 L",
      // "Tinos"
   };


   //TEST
   //public static final int DEFAULT_FONT_SIZE = 64;
   public static final int DEFAULT_FONT_SIZE = 128;

   /**
    * Generate an image for |character|.
    * The image will have a white backgrond with |character| in black.
    */
   public static MagickImage generateCharacter(char character,
                                               boolean shrink,
                                               int fontSize,
                                               String fontFamily) throws Exception {
      int sideLength = fontSize;

      MagickImage image = new MagickImage();
      byte[] pixels = new byte[sideLength * sideLength * 3];
      for (int i = 0; i < pixels.length; i++) {
         pixels[i] = (byte)0xFF;
      }
      image.constituteImage(sideLength, sideLength, "RGB", pixels);

      ImageInfo drawInfo = new ImageInfo();
      DrawInfo draw = new DrawInfo(drawInfo);

      draw.setOpacity(0);
      draw.setGeometry("+0+0");
      draw.setGravity(magick.GravityType.CenterGravity);

      draw.setFill(new PixelPacket(0, 0, 0, 0));
      draw.setPointsize(fontSize);
      draw.setFont(fontFamily);
      draw.setText("" + character);

      image.annotateImage(draw);

      if (shrink) {
         return ImageUtils.shrinkImage(image);
      }

      return image;
   }

   public static MagickImage generateCharacter(char character,
                                               boolean shrink) throws Exception {
      return generateCharacter(character, shrink, DEFAULT_FONT_SIZE, DEFAULT_FONT_FAMILY);
   }

   public static String[] getAvailableFonts() {
      GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
      return env.getAvailableFontFamilyNames();
   }
}
