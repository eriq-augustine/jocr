package com.eriqaugustine.ocr.utils;

import java.util.Arrays;

/**
 * Math utilities.
 */
public class MathUtils {
   private static final double DEFAULT_EPSILON = 0.0001;

   /**
    * Convert a 1D index to the row the index is on.
    */
   public static int indexToRow(int index, int width) {
      return index / width;
   }

   /**
    * Convert a 1D index to the col the index is on.
    */
   public static int indexToCol(int index, int width) {
      return index - (indexToRow(index, width) * width);
   }

   public static int rowColToIndex(int row, int col, int width) {
      return row * width + col;
   }

   public static int indexOffset(int index, int rowOffset, int colOffset, int imageWidth) {
      int row = indexToRow(index, imageWidth) + rowOffset;
      int col = indexToCol(index, imageWidth) + colOffset;
      return rowColToIndex(row, col, imageWidth);
   }

   public static boolean inBounds(int row, int col, int width, int length) {
      return row >= 0 && row < length / width &&
             col >= 0 && col < width;
   }

   public static boolean doubleEquals(double a, double b, double epsilon) {
      return Math.abs(a - b) <= epsilon;
   }

   public static boolean doubleEquals(double a, double b) {
      return doubleEquals(a, b, DEFAULT_EPSILON);
   }

   /**
    * |vals| is const.
    */
   public static double mean(double[] vals) {
      if (vals.length == 0) {
         return Double.NaN;
      }

      double sum = 0;
      for (double val : vals) {
         sum += val;
      }
      return sum / vals.length;
   }

   /**
    * |vals| is const.
    */
   public static double median(double[] vals) {
      if (vals.length == 0) {
         return Double.NaN;
      }

      double[] copy = Arrays.copyOf(vals, vals.length);
      return nonConstMedian(copy);
   }

   /**
    * Use if you don't care if |vals| is altered.
    * Guarenteed to be faster than median().
    */
   public static double nonConstMedian(double[] vals) {
      if (vals.length == 0) {
         return Double.NaN;
      }

      Arrays.sort(vals);

      if (vals.length % 2 == 1) {
         return vals[vals.length / 2];
      } else {
         return (vals[vals.length / 2] + vals[vals.length / 2 - 1]) / 2;
      }
   }
}
