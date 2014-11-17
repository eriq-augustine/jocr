package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.GeoUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * This was created before the api, so it does not have a descriptive name.
 */
public class TextExtraction extends TextExtractor {
   private static Logger logger = LogManager.getLogger(TextExtractor.class.getName());

   private static final double STRIPE_MIN_RATIO = 0.66666;
   private static final int WHITE_THRESHOLD = 150;

   /**
    * The ratio between width and height for doing diagonal bound expansion (see gridBreakup()).
    * width * BREAKUP_EXPANSION_RATIO = height
    */
   private static final double BREAKUP_EXPANSION_RATIO = 0.9;

   /**
    * @inheritDoc
    */
   public List<TextSet> extractText(WrapImage image) {
      List<TextSet> rtn = new ArrayList<TextSet>();

      // Breakup the bubbles into sets of texts.
      // Note that a set of text is a collection of text, typically a sentence or so.
      // This does not breakup characters within the set or deal with furigana.
      List<Rectangle> setBoundaries = discoverSets(image);

      // Order the sets from RTL.
      // Doesn't actually matter, but provide some consistency.
      Collections.sort(setBoundaries, new Comparator<Rectangle>(){
         public int compare(Rectangle a, Rectangle b) {
            return b.x - a.x;
         }
      });

      for (Rectangle setBoundary : setBoundaries) {
         WrapImage setImage = image.crop(setBoundary);

         Direction direction = guessDirection(setImage);
         rtn.add(diagonalExpansionGridBreakup(setImage, direction));
      }

      return rtn;
   }

   /**
    * Find the sets of text in the bubble.
    * Remember, a text set is a set of related text (typically a sentence or two).
    * This does not deal with individual characters or furigana.
    */
   private List<Rectangle> discoverSets(WrapImage image) {
      // TODO(eriq): Remove noise before set discovery.

      // First, find all the bounding boxes for non-whitespace objects (text).
      List<Rectangle> boundingRectangles = findBoundingRectangles(image);
      boolean[] checkedRectangles = new boolean[boundingRectangles.size()];

      int[] boundsMap = mapBounds(image.width(), image.height(), boundingRectangles);

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
            queue.addAll(getAdjacentRectangles(rect, boundsMap, image.width()));
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
   private List<Rectangle> mergeOverlappingGroups(List<Rectangle> groups) {
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
    * (Build a minimum bounding box that contains all the rectangles).
    */
   private List<Rectangle> findGroupBounds(List<List<Rectangle>> groups) {
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
   private Set<Integer> getAdjacentRectangles(Rectangle rect, int[] boundsMap, int width) {
      Set<Integer> neighbors = new HashSet<Integer>();

      // Horizontal
      for (int row = rect.y; row < rect.y + rect.height; row++) {
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
      for (int col = rect.x; col < rect.x + rect.width; col++) {
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
   private int[] mapBounds(int width, int height, List<Rectangle> boundingRectangles) {
      int[] rtn = new int[width * height];

      for (int i = 0; i < rtn.length; i++) {
         rtn[i] = -1;
      }

      for (int i = 0; i < boundingRectangles.size(); i++) {
         Rectangle rect = boundingRectangles.get(i);

         for (int rowOffset = 0; rowOffset < rect.height; rowOffset++) {
            for (int colOffset = 0; colOffset < rect.width; colOffset++) {
               rtn[MathUtils.rowColToIndex(rect.y + rowOffset,
                                           rect.x + colOffset,
                                           width)] = i;
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
   private List<Rectangle> findBoundingRectangles(WrapImage image) {
      boolean[] pixels = image.getDiscretePixels(WHITE_THRESHOLD);

      List<int[]> rowStripes = findStripes(pixels, image.width(), true);
      List<int[]> colStripes = findStripes(pixels, image.width(), false);

      List<Rectangle> minimalRects = shrinkStripes(pixels, image.width(), rowStripes, colStripes);

      return minimalRects;
   }

   /**
    * Take in the stripes, and shrink them to minimal character bounds.
    * Do the same kind of striping process that findStripes() uses,
    * but only on intersection of stripes.
    */
   private List<Rectangle> shrinkStripes(boolean[] pixels, int width,
                                                List<int[]> rowStripes,
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
   private List<Rectangle> boundText(boolean[] pixels, int width,
                                            int[] rowStripe,
                                            int[] colStripe) {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      int minRow = rowStripe[1];
      int maxRow = rowStripe[0];
      int minCol = colStripe[1];
      int maxCol = colStripe[0];

      for (int row = rowStripe[0]; row < rowStripe[1]; row++) {
         for (int col = colStripe[0]; col < colStripe[1]; col++) {
            if (pixels[MathUtils.rowColToIndex(row, col, width)]) {
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
    * Returns: A list of arrays ([start, end] (inclusive)).
    */
   private List<int[]> findStripes(boolean[] pixels, int width, boolean horizontal) {
      List<int[]> stripes = new ArrayList<int[]>();

      int stripeStart = -1;

      int outerEnd = horizontal ? pixels.length / width : width;
      int innerEnd = horizontal ? width : pixels.length / width;

      for (int outer = 0; outer < outerEnd; outer++) {
         boolean hasContent = false;

         for (int inner = 0; inner < innerEnd; inner++) {
            int index = horizontal ? MathUtils.rowColToIndex(outer, inner, width) :
                                     MathUtils.rowColToIndex(inner, outer, width);

            if (pixels[index]) {
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
   private Direction guessDirection(WrapImage image) {
      if (image.height() * 3 > image.width()) {
         return Direction.DOWN;
      }

      return Direction.LTR;
   }

   /**
    * Break up an image into smaller images that each represent a single character.
    * This method will focus around growing bounds diagonally.
    * The general idea is as follows:
    *  - Bound blobs.
    *  - Merge overlapping bounding boxes.
    *  - Start from the top-left most (bounded) pixel.
    *    - Begin expanding diagonally, until one of the following:
    *      - The current bounding box is circumscribed and no other bounding box is in contact.
    *      - A wall is hit. Continue expanding in the other direction.
    *      - Another bounding box is hit. Merge these two bounding boxes and restart the expansion.
    *  - Continue until there are no more bounding boxes.
    * TODO(eriq): Use direction.
    */
   private TextSet diagonalExpansionGridBreakup(WrapImage image, Direction direction) {
      // Get the bounding boxes.
      List<Rectangle> boundingBoxes = findBoundingRectangles(image);

      // Merge the bounding boxes.
      boundingBoxes = mergeBounds(boundingBoxes);

      // Order the bounding boxes (vertical, then horizontal; top left is first).
      Collections.sort(boundingBoxes, new Comparator<Rectangle>() {
         public int compare(Rectangle a, Rectangle b) {
            if (a.y != b.y) {
               return a.y - b.y;
            }

            return a.x - b.x;
         }
      });

      List<Rectangle> completeBounds = new ArrayList<Rectangle>();
      while (!boundingBoxes.isEmpty()) {
         Rectangle boundingBox = boundingBoxes.remove(0);

         boolean done = false;
         while (!done) {
            done = true;

            // We choose different starting sides depending on if the expansion ratio is greater than 1.
            // Make sure to round up.
            assert(BREAKUP_EXPANSION_RATIO > 0);

            int width;
            int height;
            if (BREAKUP_EXPANSION_RATIO >= 1) {
               width = boundingBox.width;
               height = (int)(0.5 + (width * BREAKUP_EXPANSION_RATIO));
            } else {
               height = boundingBox.height;
               width = (int)(0.5 + (height / BREAKUP_EXPANSION_RATIO));
            }

            // Clip out-of-bounds.
            width = (width >= image.width()) ? image.width() - 1 : width;
            height = (height >= image.height()) ? image.height() - 1 : height;

            Rectangle expandedBounds = new Rectangle(boundingBox.x, boundingBox.y, width, height);

            // Check to see if the final bounding box overlaps any other bounds.
            for (int i = 0; i < boundingBoxes.size(); i++) {
               if (expandedBounds.intersects(boundingBoxes.get(i))) {
                  // Merge them, remove the target from the available bounding boxes, and restart the expansion..
                  // Note that we are using the original bounding box and not the expanded one for the union so
                  // that we do not grow the new bounds any larger than necessary.
                  boundingBox = boundingBox.union(boundingBoxes.remove(i));
                  done = false;
                  break;
               }
            }
         }

         completeBounds.add(boundingBox);
      }

      WrapImage blackoutImage = WrapImage.getImageWithBlackouts(image.width(), image.height(), completeBounds);

      // Apply the old grid breakup.
      // TextSet blackoutSets = gridBreakup(blackoutImage, direction);
      TextSet blackoutSets = gridBreakupOld(blackoutImage, direction);

      return blackoutSets.swapImage(image);
   }

   /**
    * Merge overlapping bounds.
    * TODO(eriq): We can go faster.
    */
   private List<Rectangle> mergeBounds(List<Rectangle> bounds) {
      List<Rectangle> rtn = new ArrayList<Rectangle>(bounds);

      boolean done = false;
      while (!done) {
         done = true;

         for (int i = 0; i < rtn.size(); i++) {
            for (int j = i + 1; j < rtn.size(); j++) {
               if (rtn.get(i).intersects(rtn.get(j))) {
                  rtn.add(i, rtn.get(i).union(rtn.get(j)));
                  rtn.remove(i);
                  rtn.remove(j);
                  done = false;
                  break;
               }
            }

            if (!done) {
               break;
            }
         }
      }

      return rtn;
   }

   /**
    * Break an image apart in characters based off of the idea that
    * Japanese characters (whether horozontal or vertical) always
    * fit in constant sized boxes.
    * Therefore, the image can be broken up into a grid and each position
    *  will represent a character, puncuation, or space.
    * TODO(eriq): This totally ignores direction. (marked with TODO(eriq): Direction).
    */
   private TextSet gridBreakup(WrapImage image, Direction direction) {
      // TODO(eriq): Right now this is only for vertical.
      //  Horizontal should be the same, but with the major and minor
      //  axis switched.

      boolean[] pixels = image.getDiscretePixels(WHITE_THRESHOLD);

      // TODO(eriq): Direction
      List<int[]> majorStripes = findStripes(pixels, image.width(), false);

      // Merge the stripes going RtL.
      // TODO(eriq): Direction
      List<int[]> mergedStripes = mergeStripes(majorStripes, getMeanStripeSize(majorStripes), false);
      double meanMajorStripeSize = getMeanStripeSize(mergedStripes);

      List<Rectangle> mainCharacters = new ArrayList<Rectangle>();

      // Map a characters index to the furigana associated with it.
      Map<Rectangle, List<Rectangle>> furiganaMapping = new HashMap<Rectangle, List<Rectangle>>();

      for (int[] majorStripe : mergedStripes) {
         // TODO(eriq): Direction
         List<int[]> minorStripes = findMinorStripes(pixels, image.width(), majorStripe, meanMajorStripeSize, true);
         double meanMinorStripeSize = getMeanStripeSize(minorStripes);

         for (int[] minorStripe : minorStripes) {
            // TODO(eriq): Direction. Rectangle(x, y, width, height).
            Rectangle fullCharacter = new Rectangle(majorStripe[0], minorStripe[0],
                                                    majorStripe[1] - majorStripe[0] + 1,
                                                    minorStripe[1] - minorStripe[0] + 1);

            // Mask out the pixels that are not in this character.
            boolean[] fullCharacherPixels = new boolean[pixels.length];
            for (int index = 0; index < pixels.length; index++) {
               int row = MathUtils.indexToRow(index, image.width());
               int col = MathUtils.indexToCol(index, image.width());

               if (col >= majorStripe[0] && col <= majorStripe[1]
                   && row >= minorStripe[0] && row <= minorStripe[1]) {
                  fullCharacherPixels[index] = pixels[index];
               } else {
                  fullCharacherPixels[index] = false;
               }
            }

            // Look for any furigana.
            // TODO(eriq): Direction
            List<int[]> fullCharacterStripes = findStripes(fullCharacherPixels, image.width(), false);

            // TODO(eriq): Direction
            // TODO(eriq): Consider using the mean from the furi stripes.
            // Go the opposite direction as normal (LtR for vertical) and merge.
            // If a normal character is bisected, it should get merged with furigana left off and merged with itself.
            fullCharacterStripes = mergeStripes(fullCharacterStripes, meanMinorStripeSize, true);

            // Character without furigana.
            // TODO(eriq): Direction
            Rectangle mainCharacter = new Rectangle(fullCharacterStripes.get(0)[0], minorStripe[0],
                                                    fullCharacterStripes.get(0)[1] - fullCharacterStripes.get(0)[0] + 1,
                                                    minorStripe[1] - minorStripe[0] + 1);

            // There is furigana for this character.
            if (fullCharacterStripes.size() > 1) {
               int[] furiMajorStripe = new int[2];
               furiMajorStripe[0] = fullCharacterStripes.get(1)[0];
               furiMajorStripe[1] = fullCharacterStripes.get(fullCharacterStripes.size() - 1)[1];

               // Mask out the pixels that are not in this furigana stripe.
               boolean[] furiCharacherPixels = new boolean[pixels.length];
               for (int index = 0; index < pixels.length; index++) {
                  int row = MathUtils.indexToRow(index, image.width());
                  int col = MathUtils.indexToCol(index, image.width());

                  if (col >= majorStripe[0] && col <= majorStripe[1]
                     && row >= minorStripe[0] && row <= minorStripe[1]) {
                     furiCharacherPixels[index] = pixels[index];
                  } else {
                     furiCharacherPixels[index] = false;
                  }
               }

               // Split multiple furigana characters.
               // TODO(eriq): Should we merge these? (forward?)
               // TODO(eriq): Direction
               List<int[]> furiMinorStripes = findStripes(furiCharacherPixels, image.width(), true);

               // furiMinorStripes = mergeStripes(furiMinorStripes, getMeanStripeSize(furiMinorStripes), true);

               List<Rectangle> furiCharacters = new ArrayList<Rectangle>();
               for (int[] furiMinorStripe : furiMinorStripes) {
                  // TODO(eriq): Direction
                  furiCharacters.add(new Rectangle(furiMajorStripe[0], furiMinorStripe[0],
                                                   furiMajorStripe[1] - furiMajorStripe[0] + 1,
                                                   furiMinorStripe[1] - furiMinorStripe[0] + 1));
               }

               // Map the furigana to the character.
               furiganaMapping.put(mainCharacter, furiCharacters);
            }

            mainCharacters.add(mainCharacter);
         }
      }

      List<Rectangle> ordered = orderCharacters(mainCharacters);

      return new TextSet(image, ordered, furiganaMapping);
   }

   /**
    * Discover the proper stripes along the minor axis.
    * Do not let the name fool you, this is not a trivial method.
    * We are finding "minor" stripes because it is along the minor axis.
    * This is supposed to be called only after a proper stripe in the major
    * axis is discovered (|majorStripe|).
    * So, we assume that the strip of text we get represents only a single row or column of text.
    */
   private List<int[]> findMinorStripes(boolean[] oldPixels, int imageWidth, int[] majorStripe, double meanMajorStripeSize, boolean horizontal) {
      // TODO(eriq): This is a simplified, test version.
      //  See notes for full version.

      // Mask out any pixels not in this stripe.
      // TODO(eriq): Direction
      boolean[] pixels = new boolean[oldPixels.length];
      for (int index = 0; index < pixels.length; index++) {
         int col = MathUtils.indexToCol(index, imageWidth);
         if (col >= majorStripe[0] && col <= majorStripe[1]) {
            pixels[index] = oldPixels[index];
         } else {
            pixels[index] = false;
         }
      }

      // TODO(eriq): Direction.
      List<int[]> minorStripes = findStripes(pixels, imageWidth, true);

      // Merge the stripes going Top to Bottom.
      // TODO(eriq): Direction
      // List<int[]> mergedStripes = mergeStripes(minorStripes, meanMajorStripeSize, true);
      List<int[]> mergedStripes = mergeStripes(minorStripes, getMeanStripeSize(minorStripes), true);

      return mergedStripes;
   }

   /**
    * Just find the arithmetic mean of the size of the given stripes.
    */
   private double getMeanStripeSize(List<int[]> stripes) {
      double[] stripeSizes = new double[stripes.size()];
      for (int i = 0; i < stripes.size(); i++) {
         stripeSizes[i] = stripes.get(i)[1] - stripes.get(i)[0] + 1;
      }

      return MathUtils.mean(stripeSizes);
   }

   /**
    * Merge small stripes into larger stripes.
    * The idea here is that splitting the text into stripes has
    * the potential to bisect characters and we want to re-include those.
    * If |forward| is true, then |stripes| will be iterated forwards when
    * merging backwards if false.
    */
   private List<int[]> mergeStripes(List<int[]> oldStripes, double meanStripeSize, boolean forward) {
      // Get a copy.
      List<int[]> stripes = new ArrayList<int[]>(oldStripes);

      int start;
      int end;
      int delta;

      boolean merged = true;
      while (merged) {
         merged = false;

         // Note that we do not want to hit the last stripe because it
         //  may be the target of a merge.
         if (forward) {
            start = 0;
            end = stripes.size() - 1;
            delta = 1;
         } else {
            start = stripes.size() - 1;
            end = 0;
            delta = -1;
         }

         for (int i = start; i != end; i += delta) {
            int[] stripe = stripes.get(i);
            if ((stripe[1] - stripe[0] + 1) < (meanStripeSize * STRIPE_MIN_RATIO)) {
               // Merge this strip in with the "next" one.
               // (Use the delta to determine what "next" means.)
               stripes.get(i + delta)[0] = Math.min(stripes.get(i + delta)[0], stripe[0]);
               stripes.get(i + delta)[1] = Math.max(stripes.get(i + delta)[1], stripe[1]);

               // Remove this stripe.
               stripes.remove(i);

               // Only process one merge at a time because they might cascade.
               merged = true;
               break;
            }
         }
      }

      return stripes;
   }

   // An old version that ignores direction.
   private TextSet gridBreakupOld(WrapImage image, Direction direction) {
      List<Rectangle> boundingRects = findBoundingRectangles(image);

      List<Rectangle> furiganaCandidates = getFuriganaCandidates(boundingRects);

      // Merge back in any furigana candidates that actually look like parts of characters.
      // TODO

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
            furiganaMapping.get(closest).addAll(splitFurigana(image.crop(candidate), candidate));
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
   private List<Rectangle> splitFurigana(WrapImage furiganaImage,
                                                Rectangle furiganaBounds) {
      List<Rectangle> rtn = new ArrayList<Rectangle>();

      boolean[] pixels = furiganaImage.getDiscretePixels(WHITE_THRESHOLD);

      // Assert that |furiganaImage| is local while |furiganaBounds| is global.
      assert(furiganaImage.width() == furiganaBounds.width);

      int expectedHeight = furiganaImage.width();

      List<int[]> rowStripes = findStripes(pixels, furiganaImage.width(), true);

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
   private List<Rectangle> orderCharacters(List<Rectangle> characters) {
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
   private List<List<Rectangle>> findVerticalStacks(List<Rectangle> rects) {
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
   private Rectangle getMostVerticalOverlapping(Rectangle target,
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
    * Do this by looking at the width of the characters.
    * Any character that is half the size of the median character is a candidate.
    */
   private List<Rectangle> getFuriganaCandidates(List<Rectangle> characters) {
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
}
