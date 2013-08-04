package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.MathUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import magick.MagickImage;

/**
 * Namespace for images that only contain text.
 */
public class TextImage {
   /**
    * Break an image apart in characters based off of the idea that
    * Japanese characters (whether horozontal or vertical) always
    * fit in constant sized boxes.
    * Therefore, the image can be broken up into a grid and each position
    *  will represent a character, puncuation, or space.
    * TODO(eriq): This can get in trouble for certian sets of characters.
    *  For example, a row with nothing but 'ni's (no uncommon for a single column)
    *  will split the 'ni' into two seperate characters.
    */
   public static MagickImage[][] gridBreakup(MagickImage image) throws Exception {
      Dimension dimensions = image.getDimension();

      // Note: bw pixels pulls out three values for each pixel.
      byte[] pixels = Filters.averageChannels(Filters.bwPixels(image), 3);

      // [[rowStart, rowEnd], ...]
      List<int[]> rows = findRows(pixels, dimensions.width);
      List<int[]> cols = findCols(pixels, dimensions.width);

      MagickImage[][] images = new MagickImage[rows.size()][cols.size()];

      for (int i = 0; i < rows.size(); i++) {
         for (int j = 0; j < cols.size(); j++) {
            Rectangle rect = new Rectangle(cols.get(j)[0],
                                           rows.get(i)[0],
                                           cols.get(j)[1] - cols.get(j)[0] + 1,
                                           rows.get(i)[1] - rows.get(i)[0] + 1);
            images[i][j] = image.cropImage(rect);
         }
      }

      return images;
   }

   /**
    * Assumes that |pixels| is discretized.
    */
   private static List<int[]> findRows(byte[] pixels, int width) {
      List<int[]> rows = new ArrayList<int[]>();
      int rowStart = -1;

      for (int row = 0; row < pixels.length / width; row++) {
         boolean hasContent = false;

         for (int col = 0; col < width; col++) {
            if (pixels[MathUtils.rowColToIndex(row, col, width)] == 0) {
               hasContent = true;
               break;
            }
         }

         if (rowStart == -1 && hasContent) {
            rowStart = row;
         } else if (rowStart != -1 && !hasContent) {
            rows.add(new int[]{rowStart, row});
            rowStart = -1;
         }
      }

      return rows;
   }

   private static List<int[]> findCols(byte[] pixels, int width) {
      List<int[]> cols = new ArrayList<int[]>();
      int colStart = -1;

      for (int col = 0; col < width; col++) {
         boolean hasContent = false;

         for (int row = 0; row < pixels.length / width; row++) {
            if (pixels[MathUtils.rowColToIndex(row, col, width)] == 0) {
               hasContent = true;
               break;
            }
         }

         if (colStart == -1 && hasContent) {
            colStart = col;
         } else if (colStart != -1 && !hasContent) {
            cols.add(new int[]{colStart, col});
            colStart = -1;
         }
      }

      return cols;
   }
}
