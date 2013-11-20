package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import magick.MagickImage;

import java.awt.Dimension;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * A representation of text that appears inside of a bubble.
 * There are a few important things to note when working with text from bubbles:
 *  - The text may be either LTR or Down.
 *    In manga, down is more common.
 *  - There can be multiple "sets" of text per bubble.
 *    This is usually caused when multiple bubbles collide with no border.
 *    It is a fairly common artistic technique.
 *  - Furigana is common.
 * This class attempts to handle all of these and provide a simple interface to the text
 *  (both kanji and kana).
 */
public class BubbleText {
   private enum Direction {
      LTR,
      DOWN
   };

   // TODO(eriq): Order the sets from right to left.
   private List<TextSet> textSets;

   /**
    * Extract the text-parts (not ocr) from |image|.
    * |image| should be the inner portion of a bubble and only contain text.
    */
   public BubbleText(MagickImage image) throws Exception {
      textSets = new ArrayList<TextSet>();

      List<Rectangle> setBoundaries = discoverSets(image);

      //TEST
      if (1 == 1)
         return;

      for (Rectangle setBoundary : setBoundaries) {
         Direction direction = guessDirection(image, setBoundary);
         textSets.add(gridBreakup(image, setBoundary, direction));
      }
   }

   /**
    * Find the sets of text in the bubble.
    */
   private static List<Rectangle> discoverSets(MagickImage image) throws Exception {
      // TODO(eriq): Remove noise before set discovery.

      Dimension dimensions = image.getDimension();

      // First, find all the bounding boxes for non-whitespace objects (text).
      List<Rectangle> boundingRectangles = findBoundingRectangles(image);
      boolean[] checkedRectangles = new boolean[boundingRectangles.size()];

      int[] boundsMap = mapBounds(dimensions, boundingRectangles);

      List<List<Rectangle>> groups = new ArrayList<List<Rectangle>>();

      // While there are still rectangles left to analyze.
      for (int currentRectangle = 0; currentRectangle < checkedRectangles.length;
           currentRectangle++) {
         if (checkedRectangles[currentRectangle]) {
            continue;
         }

         // Make a new group.
         List<Rectangle> group = new ArrayList<Rectangle>();
         Queue<Integer> queue = new LinkedList<Integer>();

         // Add the first rectangle to the queue.
         queue.add(currentRectangle);

         while (!queue.isEmpty()) {
            int rectIndex = queue.remove().intValue();
            Rectangle rect = boundingRectangles.get(rectIndex);

            // Skip already checked.
            if (checkedRectangles[rectIndex]) {
               continue;
            }

            checkedRectangles[rectIndex] = true;

            // Add the rectangle to the group.
            group.add(rect);

            // Check in all adjacent (not diagonal) directions for other rectangles, and add them.
            // Note: Some rectangles can slip through because they are inbetween two other
            //  rectangles. These will get caught and merged later when the bounding
            //  box for each group is negotiated.
            queue.addAll(getAdjacentRectangles(rect, boundsMap, dimensions.width));
         }

         groups.add(group);
      }

      // Get the bounding rectangles for each group.
      List<Rectangle> groupBounds = findGroupBounds(groups);

      return mergeOverlappingGroups(groupBounds);
   }

   /**
    * Find any groups that overlap and merge them.
    */
   private static List<Rectangle> mergeOverlappingGroups(List<Rectangle> groups) {
      List<Rectangle> rtn = new ArrayList<Rectangle>(groups);
      boolean change = false;

      do {
         change = false;

         // No risks or ticks, just break on change.
         for (int i = 0; i < rtn.size(); i++) {
            for (int j = i + 1; j < rtn.size(); j++) {
               if (rtn.get(i).intersects(rtn.get(j))) {
                  change = true;

                  // j is always > i, remove it first.
                  rtn.add(rtn.remove(j).union(rtn.remove(i)));
                  break;
               }
            }

            if (change) {
               break;
            }
         }

      } while (change);

      return rtn;
   }

   /**
    * Take in all the groups and merge their bounding boxes.
    */
   private static List<Rectangle> findGroupBounds(List<List<Rectangle>> groups) {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      for (List<Rectangle> group : groups) {
         int minRow = -1;
         int maxRow = -1;
         int minCol = -1;
         int maxCol = -1;

         for (Rectangle rect : group) {
            if (minRow == -1) {
               minRow = rect.y;
               maxRow = rect.y + rect.height;
               minCol = rect.x;
               maxCol = rect.x + rect.width;
            } else {
               if (minRow > rect.y) {
                  minRow = rect.y;
               }

               if (maxRow < rect.y + rect.height) {
                  maxRow = rect.y + rect.height;
               }

               if (minCol > rect.x) {
                  minCol = rect.x;
               }

               if (maxCol < rect.x + rect.width) {
                  maxCol = rect.x + rect.width;
               }
            }
         }

         if (minRow != -1) {
            rtn.add(new Rectangle(minCol, minRow, maxCol - minCol + 1, maxRow - minRow + 1));
         }
      }

      return rtn;
   }

   /**
    * Find all the bounding rectangles that are laterially adjacent to |rect|.
    */
   private static Set<Integer> getAdjacentRectangles(Rectangle rect, int[] boundsMap, int width) {
      Set<Integer> neighbors = new HashSet<Integer>();

      // Horizontal
      for (int row = rect.y; row <= rect.y + rect.height; row++) {
         // Right
         for (int col = rect.x + rect.width + 1; col < width; col++) {
            int val = boundsMap[MathUtils.rowColToIndex(row, col, width)];

            if (val != -1) {
               neighbors.add(new Integer(val));
               break;
            }
         }

         // Left
         for (int col = rect.x - 1; col >= 0; col--) {
            int val = boundsMap[MathUtils.rowColToIndex(row, col, width)];

            if (val != -1) {
               neighbors.add(new Integer(val));
               break;
            }
         }
      }

      // Vertical
      for (int col = rect.x; col <= rect.x + rect.width; col++) {
         // Down
         for (int row = rect.y + rect.height + 1; row < boundsMap.length / width; row++) {
            int val = boundsMap[MathUtils.rowColToIndex(row, col, width)];

            if (val != -1) {
               neighbors.add(new Integer(val));
               break;
            }
         }

         // Up
         for (int row = rect.y - 1; row >= 0; row--) {
            int val = boundsMap[MathUtils.rowColToIndex(row, col, width)];

            if (val != -1) {
               neighbors.add(new Integer(val));
               break;
            }
         }
      }

      return neighbors;
   }

   /**
    * Map the bounding rectangles onto a map of the image.
    * Each map location will hold -1 for nothing, otherwise the index of the bounding rectangle.
    */
   private static int[] mapBounds(Dimension dimensions, List<Rectangle> boundingRectangles) {
      int[] rtn = new int[dimensions.width * dimensions.height];

      for (int i = 0; i < rtn.length; i++) {
         rtn[i] = -1;
      }

      for (int i = 0; i < boundingRectangles.size(); i++) {
         Rectangle rect = boundingRectangles.get(i);

         for (int rowOffset = 0; rowOffset < rect.height; rowOffset++) {
            for (int colOffset = 0; colOffset < rect.width; colOffset++) {
               rtn[MathUtils.rowColToIndex(rect.y + rowOffset,
                                           rect.x + colOffset,
                                           dimensions.width)] = i;
            }
         }
      }

      return rtn;
   }

   /**
    * Find all the rectangles that minimally bound non-whitespace.
    * Don't try to find adjacent rectangles or do any mergeing.
    * Ideally, this will bound each and every character.
    */
   private static List<Rectangle> findBoundingRectangles(MagickImage image) throws Exception {
      byte[] pixels = Filters.averageChannels(Filters.bwPixels(image), 3);
      Dimension dimensions = image.getDimension();

      List<int[]> rowStripes = findStripes(pixels, dimensions.width, true);
      List<int[]> colStripes = findStripes(pixels, dimensions.width, false);

      List<Rectangle> minimalRects = shrinkStripes(pixels, dimensions.width, rowStripes, colStripes);

      return minimalRects;
   }

   /**
    * Take in the stripes, and shrink them to minimal character bounds.
    * Do the same kind of striping process that findStripes() uses,
    * but only on intersection of stripes.
    */
   private static List<Rectangle> shrinkStripes(byte[] pixels, int width, List<int[]> rowStripes,
                                                List<int[]> colStripes) {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      for (int[] rowStripe : rowStripes) {
         for (int[] colStripe : colStripes) {
            // We are NOT guarenteed that there is actual non-whitespace in here.
            // There could even be multiple bounds in a single stripe intersection.
            rtn.addAll(boundText(pixels, width, rowStripe, colStripe));
         }
      }

      return rtn;
   }

   /**
    * Shrink the intersection of a set of stripes to a single bounding box.
    * NOTE(eriq): This does not handle the case where there are overlaps in text.
    *  So, this will not actually find the minimal bounds.
    *  Multiple character may be caught in a single.
    *  But, you are guarenteed that all text will be captured.
    * TODO(eriq): Someday actually find the minimal bounds.
    */
   private static List<Rectangle> boundText(byte[] pixels, int width, int[] rowStripe,
                                            int[] colStripe) {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      int minRow = rowStripe[1];
      int maxRow = rowStripe[0];
      int minCol = colStripe[1];
      int maxCol = colStripe[0];

      for (int row = rowStripe[0]; row < rowStripe[1]; row++) {
         for (int col = colStripe[0]; col < colStripe[1]; col++) {
            if (pixels[MathUtils.rowColToIndex(row, col, width)] == 0) {
               if (row < minRow) {
                  minRow = row;
               }

               if (row > maxRow) {
                  maxRow = row;
               }

               if (col < minCol) {
                  minCol = col;
               }

               if (col > maxCol) {
                  maxCol = col;
               }
            }
         }
      }

      if (minRow < maxRow) {
         rtn.add(new Rectangle(minCol, minRow, maxCol - minCol + 1, maxRow - minRow + 1));
      }

      return rtn;
   }

   /**
    * Find the stripes of non-whitespace.
    */
   private static List<int[]> findStripes(byte[] pixels, int width, boolean horizontal) {
      List<int[]> stripes = new ArrayList<int[]>();

      int stripeStart = -1;

      int outerEnd = horizontal ? pixels.length / width : width;
      int innerEnd = horizontal ? width : pixels.length / width;

      for (int outer = 0; outer < outerEnd; outer++) {
         boolean hasContent = false;

         for (int inner = 0; inner < innerEnd; inner++) {
            int index = horizontal ? MathUtils.rowColToIndex(outer, inner, width) :
                                     MathUtils.rowColToIndex(inner, outer, width);

            if (pixels[index] == 0) {
               hasContent = true;
               break;
            }
         }

         if (stripeStart == -1 && hasContent) {
            stripeStart = outer;
         } else if (stripeStart != -1 && !hasContent) {
            stripes.add(new int[]{stripeStart, outer});
            stripeStart = -1;
         }
      }

      if (stripeStart != -1) {
         stripes.add(new int[]{stripeStart, outerEnd});
      }

      return stripes;
   }

   /**
    * Guess the direction of the text.
    */
   private static Direction guessDirection(MagickImage image, Rectangle setBoundary) {
      // TODO
      return null;
   }

   /**
    * Break an image apart in characters based off of the idea that
    * Japanese characters (whether horozontal or vertical) always
    * fit in constant sized boxes.
    * Therefore, the image can be broken up into a grid and each position
    *  will represent a character, puncuation, or space.
    */
   private static TextSet gridBreakup(MagickImage image, Rectangle setBoundary,
                                      Direction direction) {
      // TODO
      return null;
   }

   /**
    * A container for a "set" of text.
    */
   // TODO(eriq): Define the types. String and rect won't work.
   // TODO(eriq): Breakout into formal class?
   private static class TextSet {
      /**
       * The entire set (including any kanji and furigana).
       */
      public final String fullText;

      /**
       * The set without any furigana.
       */
      public final String noFuriganaText;

      /**
       * The text with any furigana replacing the kanji it covers.
       */
      public final String furiganaReplacementText;

      public TextSet(String fullText, String noFuriganaText, String furiganaReplacementText) {
         // TODO
         this.fullText = null;
         this.noFuriganaText = null;
         this.furiganaReplacementText = null;
      }
   }
}
