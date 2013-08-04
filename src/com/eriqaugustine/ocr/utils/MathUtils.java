package com.eriqaugustine.ocr.utils;

/**
 * Math utilities.
 */
public class MathUtils {
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
}
