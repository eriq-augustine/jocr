package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import magick.MagickImage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Namespace for images that only contain text.
 */
public class TextImage {
   private static final double STRIPE_VARIANCE = 0.35;

   private enum Direction {
      LTR,
      DOWN
   };

   /**
    * Break up an image (using gridBreakup()), try to figure out a direction, and
    * then string then images together into a single ordered list.
    */
   @SuppressWarnings("fallthrough")
   public static List<MagickImage> characterBreakup(MagickImage image) throws Exception {
      MagickImage[][] gridImages = gridBreakup(image);

      List<MagickImage> rtn = new ArrayList<MagickImage>();

      if (gridImages.length == 0) {
         return rtn;
      }

      Direction direction = findDirection(gridImages);

      if (direction == Direction.LTR) {
         for (int row = 0; row < gridImages.length; row++) {
            for (int col = 0; col < gridImages[row].length; col++) {
               rtn.add(gridImages[row][col]);
            }

            // Add in a space.
            rtn.add(ImageUtils.emptyImage());
         }
      } else {
         // Remember: Vertical is RTL.
         for (int col = gridImages[0].length - 1; col >= 0; col--) {
            for (int row = 0; row < gridImages.length; row++) {
               rtn.add(gridImages[row][col]);
            }

            // Add in a space.
            rtn.add(ImageUtils.emptyImage());
         }
      }

      return rtn;
   }

   /**
    * Find the reading direction of a grid of text.
    * The idea behind this is simple: go down each reading direction (LTR and DOWN),
    * the direction with the least breaks (spaces) is the reading direction.
    * Trailing spaces are ok.
    * If there are no breaks (spaces), then pick the direction with the longest line
    * (row for LTR, column for DOWN).
    * LTR's baseline is the left column.
    * DOWN's baseline is the top row.
    */
   private static Direction findDirection(MagickImage[][] characterGrid) throws Exception {
      if (characterGrid.length == 0) {
         return Direction.LTR;
      }

      // LTR
      int fullLTRLines = getLTRFullLines(characterGrid);

      // DOWN
      int fullDownLines = getDownFullLines(characterGrid);

      if (MathUtils.doubleEquals((double)fullLTRLines / characterGrid.length,
                                 (double)fullDownLines / characterGrid[0].length)) {
         // Go with whichever direction has the longest line.
         // No need to check all the lines, just the overall size.
         if (characterGrid.length > characterGrid[0].length) {
            return Direction.DOWN;
         }

         return Direction.LTR;
      }

      if ((double)fullDownLines / characterGrid[0].length >
          (double)fullLTRLines / characterGrid.length) {
         return Direction.DOWN;
      }

      return Direction.LTR;
   }

   private static int getLTRFullLines(MagickImage[][] characterGrid) throws Exception {
      int fullLines = 0;
      for (int row = 0; row < characterGrid.length; row++) {
         if (characterGrid[row].length == 0 || ImageUtils.isEmptyImage(characterGrid[row][0])) {
            continue;
         }

         boolean fullLine = true;
         boolean seenSpace = false;
         for (int col = 0; col < characterGrid[row].length; col++) {
            if (ImageUtils.isEmptyImage(characterGrid[row][col])) {
               seenSpace = true;
            } else {
               // A non-empty character after a break.
               if (seenSpace) {
                  fullLine = false;
                  break;
               }
            }
         }

         if (fullLine) {
            fullLines++;
         }
      }

      return fullLines;
   }

   /**
    * The grid better be a rectangle (... so a grid).
    */
   private static int getDownFullLines(MagickImage[][] characterGrid) throws Exception {
      assert(characterGrid.length > 0);

      int fullLines = 0;
      for (int col = 0; col < characterGrid[0].length; col++) {
         if (ImageUtils.isEmptyImage(characterGrid[0][col])) {
            continue;
         }

         boolean fullLine = true;
         boolean seenSpace = false;
         for (int row = 0; row < characterGrid.length; row++) {
            if (ImageUtils.isEmptyImage(characterGrid[row][col])) {
               seenSpace = true;
            } else {
               // A non-empty character after a break.
               if (seenSpace) {
                  fullLine = false;
                  break;
               }
            }
         }

         if (fullLine) {
            fullLines++;
         }
      }

      return fullLines;
   }

   /**
    * Break an image apart in characters based off of the idea that
    * Japanese characters (whether horozontal or vertical) always
    * fit in constant sized boxes.
    * Therefore, the image can be broken up into a grid and each position
    *  will represent a character, puncuation, or space.
    */
   public static MagickImage[][] gridBreakup(MagickImage image) throws Exception {
      MagickImage shrinkImage = ImageUtils.shrinkImage(image);
      Dimension dimensions = shrinkImage.getDimension();

      if (dimensions.width == 0 && dimensions.height == 0) {
         return new MagickImage[0][0];
      }

      // Note: bw pixels pulls out three values for each pixel.
      byte[] pixels = Filters.averageChannels(Filters.bwPixels(shrinkImage), 3);

      // [[rowStart, rowEnd], ...]
      List<int[]> rows = findStripes(pixels, dimensions.width, true);
      List<int[]> cols = findStripes(pixels, dimensions.width, false);

      rows = normalizeStripes(rows);
      cols = normalizeStripes(cols);

      MagickImage[][] images = new MagickImage[rows.size()][cols.size()];

      for (int i = 0; i < rows.size(); i++) {
         for (int j = 0; j < cols.size(); j++) {
            Rectangle rect = new Rectangle(cols.get(j)[0],
                                           rows.get(i)[0],
                                           cols.get(j)[1] - cols.get(j)[0] + 1,
                                           rows.get(i)[1] - rows.get(i)[0] + 1);
            images[i][j] = ImageUtils.shrinkImage(shrinkImage.cropImage(rect));
         }
      }

      return images;
   }

   private static List<int[]> normalizeStripes(List<int[]> stripes) {
      // Note(eriq): Widening the stripes hurt the first and last line too much.
      // stripes = widenStripes(stripes);

      double[] widths = stripeWidths(stripes);
      double average = MathUtils.median(widths);

      List<int[]> newStripes = new ArrayList<int[]>();
      int stripeStart = -1;

      for (int i = 0; i < stripes.size(); i++) {
         if (stripeStart == -1) {
            double width = widths[i];

            // Note: Don't abs because we don't care if the partition is bigger.
            if ((average - width) / average <= STRIPE_VARIANCE) {
               newStripes.add(stripes.get(i));
            } else {
               stripeStart = i;
            }
         } else {
            double width = stripes.get(i)[1] - stripes.get(stripeStart)[0];

            if ((average - width) / average <= STRIPE_VARIANCE) {
               newStripes.add(new int[]{stripes.get(stripeStart)[0], stripes.get(i)[1]});
               stripeStart = -1;
            }
         }
      }

      if (stripeStart != -1) {
         newStripes.add(new int[]{stripes.get(stripeStart)[0],
                                  stripes.get(stripes.size() - 1)[1]});
      }

      return newStripes;
   }

   /**
    * Widen stripes so that they are all touching.
    * This should consume some whitespace and make finding small ones easier.
    */
   private static List<int[]> widenStripes(List<int[]> stripes) {
      List<int[]> wideStripes = new ArrayList<int[]>();

      for (int i = 0; i < stripes.size(); i++) {
         int start;
         if (i == 0) {
            start = stripes.get(i)[0];
         } else {
            start = (stripes.get(i)[0] + stripes.get(i - 1)[1]) / 2;
         }

         int end;
         if (i == stripes.size() - 1) {
            end = stripes.get(i)[1];
         } else {
            end = (stripes.get(i + 1)[0] + stripes.get(i)[1]) / 2;
         }

         wideStripes.add(new int[]{start, end});
      }

      return wideStripes;
   }

   // HACK(eriq): Using doubles and not ints because I am lazy and don't want to
   // write multiple different math utils.
   private static double[] stripeWidths(List<int[]> stripes) {
      double[] widths = new double[stripes.size()];

      for (int i = 0; i < stripes.size(); i++) {
         widths[i] = stripes.get(i)[1] - stripes.get(i)[0];
      }

      return widths;
   }

   private static List<int[]> findStripes(byte[] pixels, int width, boolean horizontal) {
      List<int[]> stripes = new ArrayList<int[]>();

      // Note: The image has already been shrunk, so the first and last rows/cols MUST be
      // boundaries.
      int stripeStart = 0;

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
}
