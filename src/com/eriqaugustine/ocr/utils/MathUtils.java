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
    * Log base 2.
    */
   public static double log2(double val) {
      return Math.log(val) / Math.log(2);
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

   public static double min(double[] vals) {
      if (vals.length == 0) {
         return Double.NaN;
      }

      double[] range = range(vals);
      return range[0];
   }

   public static double max(double[] vals) {
      if (vals.length == 0) {
         return Double.NaN;
      }

      double[] range = range(vals);
      return range[1];
   }

   /**
    * Get the range of an array.
    * will return null if the array is empty.
    * Will return a double[2] (min at 0, max at 1).
    */
   public static double[] range(double[] vals) {
      if (vals.length == 0) {
         return null;
      }

      double[] rtn = {vals[0], vals[0]};

      for (int i = 1; i < vals.length; i++) {
         if (rtn[0] > vals[i]) {
            rtn[0] = vals[i];
         } else if (rtn[1] < vals[i]) {
            rtn[1] = vals[i];
         }
      }

      return rtn;
   }

   /**
    * |vals| is const.
    */
   public static double variance(double[] vals) {
      if (vals.length == 0) {
         return Double.NaN;
      }

      double mean = mean(vals);

      double[] deviations = new double[vals.length];

      for (int i = 0; i < vals.length; i++) {
         deviations[i] = Math.pow(vals[i] - mean, 2);
      }

      return mean(deviations);
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

   // TEST
   public static void main(String[] args) {
      double[] test = {
         8.855947, 13.944415, 15.279197, 4.588160, 13.861727, 3.319060, 13.071810,
         2.722658, 12.490826, 2.379061, 12.345440, 2.085090, 11.962393, 1.717368,
         11.672187, 1.625521, 11.570918, 1.480610, 11.367888, 1.332107};
      rank(test);
   }

   /**
    * Get back an array that gives a rank [0, N) for each location in the array.
    * The largest value will get a rank of 0.
    * Ties will get an arbitrary ordering.
    */
   public static int[] rank(double[] vals) {
      double[] sorted = Arrays.copyOf(vals, vals.length);

      int[] rtn = new int[vals.length];
      for (int i = 0; i < vals.length; i++) {
         rtn[i] = -1;
      }

      Arrays.sort(sorted);

      int count = 0;
      for (int i = sorted.length - 1; i >= 0; i--) {
         // Find the matching value in |vals| that has not been used yet.
         for (int j = 0; j < vals.length; j++) {
            if (doubleEquals(sorted[i], vals[j]) && rtn[j] == -1) {
               rtn[j] = count++;
               break;
            }
         }
      }

      return rtn;
   }
}
