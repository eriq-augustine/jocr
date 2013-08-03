package com.eriqaugustine.ocr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Blob {
   // The assumed percentage of the image that is consumed by a single blob.
   // Will be used in the initial size of the blob's set.
   public static final double BLOB_COVERAGE = 0.05;

   // The necessary percentage of points on an edge during adjustments.
   public static final double DEFAULT_SIDE_COVERAGE = 0.10;

   private static int nextId = 0;

   private int id;
   private Set<Integer> points;

   private int minRow;
   private int maxRow;
   private int minCol;
   private int maxCol;

   private final int imageWidth;
   private final int imageLength;

   public Blob(int imageLength, int imageWidth) {
      id = nextId++;
      points = new HashSet<Integer>((int)(imageLength * BLOB_COVERAGE));

      minRow = Util.indexToRow(imageLength - 1, imageWidth);
      maxRow = 0;
      minCol = imageWidth - 1;
      maxCol = 0;

      this.imageWidth = imageWidth;
      this.imageLength = imageLength;
   }

   public void addPoint(int index) {
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
      return size() / (double)((maxCol - minCol + 1) * (maxRow - minRow + 1));
   }

   /**
    * Adjust the geometry of the blob to be more regular.
    * This shold be done after the blob is already constructed.
    * This is allowd to add and drop points without consulting the actual image.
    * As a result, blobs may not be disjoint.
    */
   public void geometryAdjust(double sideCoverage) {
      adjustBoundaries(sideCoverage);
   }

   public void geometryAdjust() {
      geometryAdjust(DEFAULT_SIDE_COVERAGE);
   }

   /**
    * Average the edges of the blob to be more rectangular.
    * We will do this by shrinking the circumscribing rectangle until
    *  each side has a significant amount of points on it.
    */
   private void adjustBoundaries(double sideCoverage) {
      // The sides must be fixed all at the same time, or it would affect
      //  the other sides.
      boolean leftDone = false;
      boolean rightDone = false;
      boolean upDone = false;
      boolean downDone = false;

      while (!leftDone || !rightDone || !upDone || !downDone) {
         if (maxRow <= minRow) {
            upDone = true;
            downDone = true;

            if (maxRow < minRow) {
               int temp = minRow;
               minRow = maxRow;
               maxRow = temp;
            }
         }

         if (maxCol <= minCol) {
            leftDone = true;
            rightDone = true;

            if (maxCol < minCol) {
               int temp = minCol;
               minCol = maxCol;
               maxCol = temp;
            }
         }

         if (!upDone) {
            if (adjustEdge(minRow, minRow, minCol, maxCol, sideCoverage)) {
               minRow++;
            } else {
               upDone = true;
            }
         }

         if (!rightDone) {
            if (adjustEdge(minRow, maxRow, maxCol, maxCol, sideCoverage)) {
               maxCol--;
            } else {
               rightDone = true;
            }
         }

         if (!downDone) {
            if (adjustEdge(maxRow, maxRow, minCol, maxCol, sideCoverage)) {
               maxRow--;
            } else {
               downDone = true;
            }
         }

         if (!leftDone) {
            if (adjustEdge(minRow, maxRow, minCol, minCol, sideCoverage)) {
               minCol++;
            } else {
               leftDone = true;
            }
         }
      }
   }

   /**
    * Adjust a single edge.
    * Wither minRow == maxRow || minCol == maxCol.
    * Return false if no adjustment was made.
    */
   private boolean adjustEdge(int firstRow, int lastRow,
                              int firstCol, int lastCol,
                              double sideCoverage) {
      assert(firstRow == lastRow || firstCol == lastCol);

      int length = (lastCol - firstCol) + (lastRow - firstRow) + 1;
      List<Integer> edgePoints = new ArrayList<Integer>(length);

      for (int row = firstRow; row <= lastRow; row++) {
         for (int col = firstCol; col <= lastCol; col++) {
            int index = Util.rowColToIndex(row, col, imageWidth);

            if (points.contains(index)) {
               edgePoints.add(index);
            }
         }
      }

      if (edgePoints.size() / (double)length >= sideCoverage) {
         return false;
      } else {
         for (Integer edgePoint : edgePoints) {
            points.remove(edgePoint);
         }
      }

      return true;
   }

   public int size() {
      return points.size();
   }

   public int getId() {
      return id;
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
