package com.eriqaugustine.ocr.utils;

import java.awt.Color;
import java.util.Random;

/**
 * Utils for COLORS!!!
 */
public class ColorUtils {
   private static ColorGenerator internalColorGen = new ColorGenerator();

   private static final double GOLDEN_RATIO_CONJUGATE = 0.618033988749895;

   private static final double SATURATION = 0.5;

   private static final double BRIGHTNESS = 0.95;

   /**
    * Testing main.
    */
   public static void main(String[] args) {
      for (int i = 0; i < 10; i++) {
         System.out.println(nextColor());
      }
   }

   /**
    * Use the internal color generator to get a new color.
    */
   public static Color nextColor() {
      return internalColorGen.next();
   }

   /**
    * Generates random, readable colors.
    */
   public static class ColorGenerator {
      private Random rand;

      public ColorGenerator() {
         rand = new Random();
      }

      /**
       * Use HSV (HSB) instead of RGB to generate random colors so we can use a
       * constant saturation and value (brightness).
       */
      public Color next() {
         double hue = (rand.nextDouble() + GOLDEN_RATIO_CONJUGATE) % 1;
         return Color.getHSBColor((float)hue, (float)SATURATION, (float)BRIGHTNESS);
      }
   }
}
