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
   private static final double STRIPE_VARIANCE = 0.20;

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

   // TODO(eriq): This can get in trouble with a small number of stripes.
   private static List<int[]> normalizeStripes(List<int[]> stripes) {
      stripes = widenStripes(stripes);

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
