package com.eriqaugustine.ocr;

import java.util.HashSet;
import java.util.Set;

public class Blob {
   // The assumed percentage of the image that is consumed by a single blob.
   // Will be used in the initial size of the blob's set.
   public static final double BLOB_COVERAGE = 0.05;

   private int start;
   private Set<Integer> points;

   private int minRow;
   private int maxRow;
   private int minCol;
   private int maxCol;

   private final int imageWidth;
   private final int imageLength;

   public Blob(int imageLength, int imageWidth) {
      start = -1;
      points = new HashSet<Integer>((int)(imageLength * BLOB_COVERAGE));

      minRow = Util.indexToRow(imageLength - 1, imageWidth);
      maxRow = 0;
      minCol = imageWidth - 1;
      maxCol = 0;

      this.imageWidth = imageWidth;
      this.imageLength = imageLength;
   }

   public void addPoint(int index) {
      if (points.size() == 0) {
         start = index;
      }

      points.add(index);

      int row = Util.indexToRow(index, imageWidth);
      int col = Util.indexToCol(index, imageWidth);

      if (row < minRow) {
         minRow = row;
      }

      if (col < minCol) {
         minCol = col;
      }

      if (row > maxRow) {
         maxRow = row;
      }

      if (col > maxCol) {
         maxCol = col;
      }
   }

   /**
    * Get the dnesity of the blob.
    * The density is percentage of the circumscribing rectangle covered by the blob.
    */
   public double density() {
      //TEST
      System.err.println(start + " -- " + size());
      System.err.println("   " + minCol + " - " + maxCol);
      System.err.println("   " + minRow + " - " + maxRow);
      System.err.println("      " + ((maxCol - minCol + 1) * (maxRow - minRow + 1)));
      System.err.println();

      return size() / (double)((maxCol - minCol + 1) * (maxRow - minRow + 1));
   }

   public int size() {
      return points.size();
   }

   public int getStart() {
      return start;
   }

   public Set<Integer> getPoints() {
      return points;
   }

   /**
      * Check if the the blob is the border blob
      * (the blob the surrounds the initial borders of the image).
      */
   public boolean isBorderBlob() {
      return points.contains(0) &&
             points.contains(imageLength - 1) &&
             points.contains(imageWidth - 1) &&
             points.contains(imageLength - imageWidth + 1);
   }
}
