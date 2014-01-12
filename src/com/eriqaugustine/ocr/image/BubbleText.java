package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.GeoUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import magick.MagickImage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Dimension;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A representation of text that appears inside of a bubble.
 * There are a few important things to note when working with text from bubbles:
 *  - The text may be either LTR or Down.
 *    In manga, down is far more common.
 *  - There can be multiple "sets" of text per bubble.
 *    This is usually caused when multiple bubbles collide with no border.
 *    It is a fairly common artistic technique.
 *  - Furigana is common.
 * This class attempts to handle all of these and provide a simple interface to the text
 *  (both kanji and kana).
 */
public class BubbleText {
   private static Logger logger = LogManager.getLogger(BubbleText.class.getName());

   private enum Direction {
      LTR,
      DOWN
   };

   private List<TextSet> textSets;

   /**
    * Extract the text-parts (not ocr) from |image|.
    * |image| should be the inner portion of a bubble and only contain text.
    */
   private BubbleText(MagickImage image) throws Exception {
      textSets = new ArrayList<TextSet>();

      List<Rectangle> setBoundaries = discoverSets(image);

      // Order the sets from RTL.
      // Doesn't actually matter, but provide some consistency.
      Collections.sort(setBoundaries, new Comparator<Rectangle>(){
         public int compare(Rectangle a, Rectangle b) {
            return b.x - a.x;
         }
      });

      for (Rectangle setBoundary : setBoundaries) {
         // HACK(eriq): ImageMagick fails when you try to crop a crop, so make a copy.
         MagickImage setImage = ImageUtils.copyImage(image.cropImage(setBoundary));

         Direction direction = guessDirection(setImage);
         textSets.add(gridBreakup(setImage, direction));
      }
   }

   // The public interface to get a BubbleText.
   public static BubbleText constructBubbleText(MagickImage image) {
      try {
         BubbleText text = new BubbleText(image);
         return text;
      } catch (Exception ex) {
         logger.error("Unable to construct a BubbleText.", ex);
         return null;
      }
   }

   public List<TextSet> getTextSets() {
      return textSets;
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

      List<Rectangle> minimalRects = shrinkStripes(pixels, dimensions.width,
                                                   rowStripes, colStripes);

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
    * Look at the dimensions of the image and pick the longer side
    *  (with a significant bias towards vertical).
    * TODO(eriq): This is a pretty naive method.
    */
   private static Direction guessDirection(MagickImage image) throws Exception {
      Dimension dimensions = image.getDimension();

      if (dimensions.height * 3 > dimensions.width) {
         return Direction.DOWN;
      }

      return Direction.LTR;
   }

   /**
    * Break an image apart in characters based off of the idea that
    * Japanese characters (whether horozontal or vertical) always
    * fit in constant sized boxes.
    * Therefore, the image can be broken up into a grid and each position
    *  will represent a character, puncuation, or space.
    * TODO(eriq): This totally ignores direction.
    */
   private static TextSet gridBreakup(MagickImage image, Direction direction) throws Exception {
      List<Rectangle> boundingRects = findBoundingRectangles(image);


      List<Rectangle> furiganaCandidates = getFuriganaCandidates(boundingRects);

      // All the characters.
      List<Rectangle> allCharacters = new ArrayList<Rectangle>(boundingRects);

      // Characters minus furigana.
      List<Rectangle> fullCharacters = new ArrayList<Rectangle>(boundingRects);
      fullCharacters.removeAll(furiganaCandidates);

      // Map a characters index to the furigana associated with it.
      Map<Rectangle, List<Rectangle>> furiganaMapping = new HashMap<Rectangle, List<Rectangle>>();
      // Note(eriq): This does not cover all combinations because failed candidates will
      //  get added back into the full pool.
      for (Rectangle candidate : furiganaCandidates) {
         // If the candidate is really close to another character, then attach it.
         // TODO(eriq): This only considers vertical.
         Rectangle closest = getMostVerticalOverlapping(candidate, fullCharacters);

         if (closest == null) {
            // Add the failed candidate back to the pool.
            fullCharacters.add(candidate);
            continue;
         }

         // Look right of the full character.
         if (candidate.x - (closest.x + closest.width) < closest.width / 2) {
            if (!furiganaMapping.containsKey(closest)) {
               furiganaMapping.put(closest, new ArrayList<Rectangle>());
            }
            furiganaMapping.get(closest).addAll(splitFurigana(image.cropImage(candidate),
                                                              candidate));
         } else {
            // Add the failed candidate back to the pool.
            fullCharacters.add(candidate);
         }
      }

      List<Rectangle> ordered = orderCharacters(fullCharacters);

      return new TextSet(image, ordered, furiganaMapping);
   }

   /**
    * Potentially split a furigana into the individual kana.
    * Furigana is special because multiple kana will uaually get in the same initial bounds
    *  (since it is boarderd by a kangi).
    * In addition, furigana should always (probably) have only one row/col.
    * Tactic:
    *  - Assume the the kana will be squareish, and use the width as the potential height.
    *  - Find the horizontal stripes.
    *  - Grow the current section starting from the top.
    *     - On every addition, see if it is better to add the section or not
    *       (better = closer to desired height).
    *     - If better, add it. If not, then commit the current one as a kana, and build a new one.
    * We need to go through all this trouble because of kana like 'こ'.
    * Note: |furiganaImage| is an image that only contains the furigana's bounds.
    *  BUT!, |furiganaBounds| is a GLOBAL rectangle that bounds the furigana.
    *  The return is expected to be a global rectangle, not local to just the furigana.
    * TODO(eriq): Does not consider direction.
    */
   private static List<Rectangle> splitFurigana(MagickImage furiganaImage,
                                                Rectangle furiganaBounds) throws Exception {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      byte[] pixels = Filters.averageChannels(Filters.bwPixels(furiganaImage), 3);
      Dimension dimensions = furiganaImage.getDimension();

      // Assert that |furiganaImage| is local while |furiganaBounds| is global.
      assert(dimensions.width == furiganaBounds.width);

      int expectedHeight = dimensions.width;

      List<int[]> rowStripes = findStripes(pixels, dimensions.width, true);

      int currentStart = 0;
      int currentEnd = 0;
      while (rowStripes.size() > 0) {
         int[] currentStripe = rowStripes.remove(0);

         int currentDifference = Math.abs(expectedHeight - (currentEnd - currentStart + 1));
         int newDifference = Math.abs(expectedHeight - (currentStripe[1] - currentStart + 1));

         if (newDifference < currentDifference) {
            // Add the current stripe.
            currentEnd = currentStripe[1];
         } else {
            // The kana is done.
            // Use |furiganaBounds| to offset this rectangle to the global image.
            rtn.add(new Rectangle(furiganaBounds.x, furiganaBounds.y + currentStart,
                                  furiganaBounds.width, currentEnd - currentStart + 1));

            currentStart = currentStripe[0];
            currentEnd = currentStripe[1];
         }
      }

      if (currentStart != currentEnd) {
         rtn.add(new Rectangle(furiganaBounds.x, furiganaBounds.y + currentStart,
                               furiganaBounds.width, currentEnd - currentStart + 1));
      }

      return rtn;
   }

   /**
    * Take all the bounding boxes for characters and order them in the proper reading order.
    */
   private static List<Rectangle> orderCharacters(List<Rectangle> characters) {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      // Find the vertical stacks (character with vertical overlap).
      List<List<Rectangle>> stacks = findVerticalStacks(characters);

      // Order within vertical stacks (top to bottom).
      for (List<Rectangle> stack : stacks) {
         Collections.sort(stack, new Comparator<Rectangle>(){
            public int compare(Rectangle a, Rectangle b) {
               return a.y - b.y;
            }
         });
      }

      // Order stacks horizontally.
      // Ensure no horizontal overlap.
      Collections.sort(stacks, new Comparator<List<Rectangle>>(){
         public int compare(List<Rectangle> a, List<Rectangle> b) {
            int minA = -1;
            for (Rectangle rect : a) {
               if (minA == -1 || minA > rect.x) {
                  minA = rect.x;
               }
            }

            int minB = -1;
            for (Rectangle rect : b) {
               if (minB == -1 || minB > rect.x) {
                  minB = rect.x;
               }
            }

            // RTL
            return minB - minA;
         }
      });

      // Add
      for (List<Rectangle> stack : stacks) {
         rtn.addAll(stack);
      }

      return rtn;
   }

   /**
    * Split the characters up by vertical stack.
    * Destructive towards rects.
    */
   private static List<List<Rectangle>> findVerticalStacks(List<Rectangle> rects) {
      List<List<Rectangle>> rtn = new ArrayList<List<Rectangle>>();

      while (rects.size() > 0) {
         List<Rectangle> newStack = new ArrayList<Rectangle>();
         newStack.add(rects.remove(0));

         boolean change = false;
         do {
            change = false;

            for (int i = 0; i < newStack.size(); i++) {
               Rectangle stackRect = newStack.get(i);

               for (int j = rects.size() - 1; j >= 0; j--) {
                  if (GeoUtils.hasHorizontalOverlap(rects.get(j), stackRect)) {
                     change = true;

                     // Don't bother breaking, can keep going.
                     newStack.add(rects.remove(j));
                  }
               }
            }
         } while(change);

         rtn.add(newStack);
      }

      return rtn;
   }

   /**
    * Find the most vertically overlapping rectangle.
    * Note that the characters DO NOT have to horizontally overlap.
    * Return null if there are no overlaps.
    */
   private static Rectangle getMostVerticalOverlapping(Rectangle target,
                                                       List<Rectangle> candidates) {
      int bestOverlap = 0;
      int bestIndex = -1;

      for (int i = 0; i < candidates.size(); i++) {
         Rectangle candidate = candidates.get(i);

         int top = Math.max(target.y, candidate.y);
         int bottom = Math.min(target.y + target.height, candidate.y + candidate.height);

         if (bottom - top > bestOverlap) {
            bestOverlap = bottom - top;
            bestIndex = i;
         }
      }

      if (bestIndex == -1) {
         return null;
      }

      return candidates.get(bestIndex);
   }

   /**
    * Find the character (bounding boxes) of potential furigana.
    * Do this my looking at the width of the characters.
    * Any character that is half the size of the median character is a candidate.
    */
   private static List<Rectangle> getFuriganaCandidates(List<Rectangle> characters) {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      double[] widths = new double[characters.size()];
      for (int i = 0; i < characters.size(); i++) {
         widths[i] = characters.get(i).width;
      }

      // Use median plz.
      double medianWidth = MathUtils.median(widths);

      for (Rectangle character : characters) {
         if (character.width < medianWidth / 2.0) {
            rtn.add(character);
         }
      }

      return rtn;
   }

   /**
    * A container for a "set" of text.
    */
   // TODO(eriq): Breakout into formal class?
   public static class TextSet {
      /**
       * The entire set image (hopefully, the kanji and its covering furigana in the same image).
       */
      public final MagickImage baseImage;

      /**
       * The set without any furigana.
       */
      public final List<MagickImage> noFuriganaText;

      /**
       * The text with any furigana replacing the kanji it covers.
       */
      public final List<MagickImage> furiganaReplacementText;

      public TextSet(MagickImage image,
                     List<Rectangle> fullText,
                     Map<Rectangle, List<Rectangle>> furiganaMapping) throws Exception {
         baseImage = ImageUtils.copyImage(image);

         noFuriganaText = new ArrayList<MagickImage>();
         for (Rectangle rect : fullText) {
            noFuriganaText.add(baseImage.cropImage(rect));
         }

         furiganaReplacementText = new ArrayList<MagickImage>();
         for (Rectangle rect : fullText) {
            if (furiganaMapping.containsKey(rect)) {
               for (Rectangle furiRect : furiganaMapping.get(rect)) {
                  furiganaReplacementText.add(baseImage.cropImage(furiRect));
               }
            } else {
               furiganaReplacementText.add(baseImage.cropImage(rect));
            }
         }
      }
   }
}
