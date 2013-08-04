package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.ImageUtils;

import java.awt.Dimension;

import magick.MagickImage;

/**
 * Namespace for images that contain a single character.
 */
public class CharacterImage {
   /**
    * Break up the character into strokes.
    */
   // TODO: Return strokes.
   public static void getStrokes(MagickImage image) throws Exception {
      Dimension dimensions = image.getDimension();

      byte[] pixels = Filters.averageChannels(Filters.bwPixels(image, 200), 3);

      // TODO(eriq).
   }

   /**
    * Get the density of the different regions of the character.
    * Note: Because pixels are atomic, some pixels on the right and bottom edges may be lost.
    *  The alternative to losing pixels would be to have uneven regions.
    */
   public static double[][] getDensityMap(MagickImage image,
                                          int rows, int cols,
                                          int whiteThreshold) throws Exception {
      assert(rows > 0 && cols > 0);

      double[][] densityMap = new double[rows][cols];

      Dimension dimensions = image.getDimension();
      byte[] pixels = Filters.averageChannels(Filters.bwPixels(image), 3);

      int rowDelta = dimensions.height / rows;
      int colDelta = dimensions.width / cols;

      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < cols; col++) {
            densityMap[row][col] =
               ImageUtils.density(pixels, dimensions.width,
                                  row * rowDelta, rowDelta,
                                  col * colDelta, colDelta,
                                  whiteThreshold);
         }
      }

      return densityMap;
   }

   public static double[][] getDensityMap(MagickImage image,
                                          int rows, int cols) throws Exception {
      return getDensityMap(image, rows, cols, ImageUtils.DEFAULT_WHITE_THRESHOLD);
   }
}
