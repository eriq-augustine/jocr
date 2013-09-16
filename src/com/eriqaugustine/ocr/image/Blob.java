package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A Blob is a collection of connected pixels.
 * This usually means a body of adjacent black pixels.
 */
public class Blob {
   // The assumed percentage of the image that is consumed by a single blob.
   // Will be used in the initial size of the blob's set.
   public static final double BLOB_COVERAGE = 0.05;

   // The necessary percentage of points on an edge during adjustments.
   public static final double DEFAULT_SIDE_COVERAGE = 0.10;

   private static int nextId = 0;

   private int id;
   private Set<Integer> points;

   private List<Blob> children;

   private int minRow;
   private int maxRow;
   private int minCol;
   private int maxCol;

   private final int imageWidth;
   private final int imageLength;

   public Blob(int imageLength, int imageWidth) {
      this(imageLength, imageWidth, BLOB_COVERAGE);
   }

   public Blob(int imageLength, int imageWidth, double blobCoverage) {
      assert(blobCoverage <= 1);

      id = nextId++;
      points = new HashSet<Integer>((int)(imageLength * blobCoverage));

      minRow = MathUtils.indexToRow(imageLength - 1, imageWidth);
      maxRow = 0;
      minCol = imageWidth - 1;
      maxCol = 0;

      this.imageWidth = imageWidth;
      this.imageLength = imageLength;

      children = new ArrayList<Blob>();
   }

   public void addPoint(int index) {
      points.add(index);

      int row = MathUtils.indexToRow(index, imageWidth);
      int col = MathUtils.indexToCol(index, imageWidth);

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

   public void addChild(Blob kid) {
      children.add(kid);
   }

   public int numChildren() {
      return children.size();
   }

   /**
    * Get the dnesity of the blob.
    * The density is percentage of the circumscribing rectangle covered by the blob.
    */
   public double density(boolean includeKids) {
      return size(includeKids) / (double)(getBoundingSize());
   }

   public double density() {
      return density(false);
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
            int index = MathUtils.rowColToIndex(row, col, imageWidth);

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

   /**
    * Get the boundaries of the blob.
    * The boundaries are defined by a map: {row : [start col, end col]}.
    * Note: This is not a getter, this requires computation.
    */
   public Map<Integer, int[]> getBoundaries() {
      Map<Integer, int[]> boundaries = new HashMap<Integer, int[]>();

      for (Integer index : points) {
         int row = MathUtils.indexToRow(index, imageWidth);
         int col = MathUtils.indexToCol(index, imageWidth);

         if (!boundaries.containsKey(row)) {
            int[] bounds = {col, col};
            boundaries.put(row, bounds);
         } else if (col < boundaries.get(row)[0]) {
            boundaries.get(row)[0] = col;
         } else if (col > boundaries.get(row)[1]) {
            boundaries.get(row)[1] = col;
         }
      }

      return boundaries;
   }

   public int getBoundingWidth() {
      return maxCol - minCol + 1;
   }

   public int getBoundingHeight() {
      return maxRow - minRow + 1;
   }

   public int getBoundingSize() {
      return getBoundingWidth() * getBoundingHeight();
   }

   public int getMinRow() {
      return minRow;
   }

   public int getMaxRow() {
      return maxRow;
   }

   public int getMinCol() {
      return minCol;
   }

   public int getMaxCol() {
      return maxCol;
   }

   public int getImageWidth() {
      return imageWidth;
   }

   public int getImageHeight() {
      return imageLength / imageWidth;
   }

   /**
    * Check to see if a blob contains another.
    * Right now, this is just a quick and dirty bounding box solution.
    */
   public boolean contains(Blob other) {
      return this.getMinRow() < other.getMinRow() &&
             this.getMaxRow() > other.getMaxRow() &&
             this.getMinCol() < other.getMinCol() &&
             this.getMaxCol() > other.getMaxCol();
   }

   /**
    * This is similar to contains(), but much more accurate
    *  (and MUCH more computationally intensive).
    * The edge points of |other| will be obtained.
    * If every edge point is adjacent to |this|, then |other| is surrounded by |this|.
    * |pixels| is the pixels for the image that contains this blob.
    * It is assumed that |pixels| only contains full black and full white.
    * We need |pixels| to calculate the proper edge points.
    */
   public boolean surrounds(Blob other) {
      if (!contains(other)) {
         return false;
      }

      // First get the outline for this blob.
      Map<Integer, List<int[]>> outline = getOutline();

      // Only bother getting the outter columns for |other|.
      int[][] outerCols = other.getOuterColumns();

      for (int i = 0; i < outerCols.length; i++) {
         int otherRow = other.getMinRow() + i;

         // TODO(eriq): This probably shouldn't happen, but I am tired and lazy.
         if (!outline.containsKey(otherRow)) {
            continue;
         }

         boolean contains = false;
         for (int[] colRange : outline.get(otherRow)) {
            // If |other|s column range for this row is bounded by the blob.
            if (colRange[0] <= outerCols[i][0] &&
                colRange[1] >= outerCols[i][1]) {
               contains = true;
               break;
            }
         }

         if (!contains) {
            return false;
         }
      }

      return true;
   }

   public int numSurroundedChildren() {
      int count = 0;

      for (Blob kid : children) {
         if (surrounds(kid)) {
            count++;
         }
      }

      return count;
   }

   /**
    * Get the outmost column at every row.
    * Get these by just scanning each row LTR and RTL.
    */
   public int[][] getOuterColumns() {
      int[][] cols = new int[getBoundingHeight()][];

      for (int row = minRow; row <= maxRow; row++) {
         int firstCol = minCol;
         int lastCol = maxCol;

         for (int col = minCol; col <= maxCol; col++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);
            if (points.contains(index)) {
               firstCol = col;
               break;
            }
         }

         for (int col = maxCol; col >= minCol; col--) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);
            if (points.contains(index)) {
               lastCol = col;
               break;
            }
         }

         cols[row - minRow] = new int[]{firstCol, lastCol};
      }

      return cols;
   }

   /**
    * Get the outline of this blob.
    * To get the outline, take the first point on the minRow.
    * Then look at the adjacent points.
    * If a point touches a spot that is not in the blob, then it is part of the outline.
    * @return {row: [[begin, end], ... (ordered)]}
    *  ie. "For this row, the blob begins here and ends here,
    *  then starts again here and so on."
    * TODO(eriq): If this needs to be called more than once, cache or something.
    */
   public Map<Integer, List<int[]>> getOutline() {
      Map<Integer, List<Integer>> border = new HashMap<Integer, List<Integer>>();
      for (int row = minRow; row <= maxRow; row++) {
         border.put(new Integer(row), new ArrayList<Integer>());
      }

      Set<Integer> visited = new HashSet<Integer>();
      Stack<Integer> candidates = new Stack<Integer>();

      for (int col = minCol; col <= maxCol; col++) {
         int index = MathUtils.rowColToIndex(minRow, col, imageWidth);
         if (points.contains(new Integer(index))) {
            candidates.push(new Integer(index));
            break;
         }
      }

      // The offsets for possible candidates.
      // [[row, col], ...]
      int[][] candidateOrientations = new int[][]{
         new int[]{-1, 0},
         new int[]{-1, 1},
         new int[]{0, 1},
         new int[]{1, 1},
         new int[]{1, 0},
         new int[]{1, -1},
         new int[]{0, -1},
         new int[]{-1, -1},
      };

      // Eventhough candidates can come from every 45 angle,
      // we should only look at the cardinal direction for the border.
      // This is to prevent two points right next to eachother from getting
      // promoted to a border (one candidate was a diagnal one).
      int[][] borderOrientations = new int[][]{
         new int[]{-1, 0},
         new int[]{0, 1},
         new int[]{1, 0},
         new int[]{0, -1},
      };

      while (!candidates.empty()) {
         int candidate = candidates.pop().intValue();

         // A double add can happen if a point is in the stack, but a neighbor
         //  add it to the stack before getting checked.
         if (visited.contains(candidate)) {
            continue;
         }

         visited.add(candidate);

         int row = MathUtils.indexToRow(candidate, imageWidth);
         int col = MathUtils.indexToCol(candidate, imageWidth);

         for (int[] offsets : borderOrientations) {
            int offsetRow = row + offsets[0];
            int offsetCol = col + offsets[1];

            int index = MathUtils.rowColToIndex(offsetRow,
                                                offsetCol,
                                                imageWidth);
            if (!points.contains(index)) {
               border.get(row).add(col);

               for (int[] candidateOffsets : candidateOrientations) {
                  int candidateRow = row + candidateOffsets[0];
                  int candidateCol = col + candidateOffsets[1];

                  int candidateIndex =
                        MathUtils.rowColToIndex(candidateRow,
                                                candidateCol,
                                                imageWidth);

                  if (!visited.contains(candidateIndex) &&
                      points.contains(candidateIndex)) {
                     candidates.push(candidateIndex);
                  }
               }

               break;
            }
         }
      }

      return collapseBorderPoints(border);
   }

   /**
    * Collapse border to be a list of ranges that represent the blob's border.
    * See getOutline()'s comment.
    * Right now, |border| is: {row: [col, ... (unordered)]}.
    * It needs to be: {row: [[begin, end], ... (ordered)]}.
    * WARNING: The values in |border| will get changed.
    */
   private Map<Integer, List<int[]>> collapseBorderPoints(Map<Integer, List<Integer>> border) {
      Map<Integer, List<int[]>> rtn = new HashMap<Integer, List<int[]>>();

      for (Map.Entry<Integer, List<Integer>> entry : border.entrySet()) {
         List<Integer> sortedCols = entry.getValue();

         if (sortedCols.size() == 0) {
            continue;
         }

         Collections.sort(sortedCols);

         List<int[]> ranges = new ArrayList<int[]>();

         // TODO(eriq): This is VERY wrong!
         // There are some cases that are very hard to distinguish between.
         // This is because horizontal lines (which may not be just on the
         // top or bottom) generate a border point for every point on the
         // horizontal. This becomes problematic when ranges are brought into
         // the picture. Should the range be extended from the horizontal set
         // to the single point on the other size.
         // Ex: [45, 46, 47, 112]
         //  Does it go from 45 - 112 ?
         //                  45 - 47, 112 ?
         // Not to mention, that this does not handle multiple range cases
         // without horizontals like: [45, 66, 77, 88].
         //  This could be: 45 - 66, 77 - 88
         //              or 45, 66 - 77, 88
         // This method needs more support from getOutline().
         ranges.add(new int[]{sortedCols.get(0),
                              sortedCols.get(sortedCols.size() - 1)});

         rtn.put(entry.getKey(), ranges);
      }

      return rtn;
   }

   /**
    * Get the average distance between the sides of the Blobs.
    * If |this| does not contain |other|, then return -1;
    */
   public double avgContainingDistance(Blob other) {
      if (!contains(other)) {
         return -1;
      }

      // No need for abs, because |other| is contained.
      return (other.getMinRow() - this.getMinRow() +
              this.getMaxRow() - other.getMaxRow() +
              other.getMinCol() - this.getMinCol() +
              this.getMaxCol() - other.getMaxCol()) / 4.0;
   }

   public int size(boolean includeKids) {
      int size = points.size();

      if (includeKids) {
         for (Blob kid : children) {
            size += kid.size(true);
         }
      }

      return size;
   }

   public int size() {
      return size(false);
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
