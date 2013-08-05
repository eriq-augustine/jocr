package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.CharacterUtils;

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
    * Get the distance (DISsimilarity) bewteen two density maps.
    * Distance is currently measured using MSE.
    */
   public static double densityMapDistance(double[][] a, double[][] b) {
      assert(a.length == b.length);

      double sumSquareError = 0;
      int count = 0;

      for (int i = 0; i < a.length; i++) {
         assert(a[i].length == b[i].length);

         for (int j = 0; j < a[i].length; j++) {
            sumSquareError += Math.pow(a[i][j] - b[i][j], 2);
            count++;
         }
      }

      return sumSquareError / count;
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

      if (rowDelta == 0 || colDelta == 0) {
         return null;
      }

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

   /**
    * Generate an image for every character in the string.
    * The index of the entry represents the character associated with it.
    */
   public static MagickImage[] generateFontImages(String characters) throws Exception {
      MagickImage[] images = new MagickImage[characters.length()];

      for (int i = 0; i < characters.length(); i++) {
         images[i] = CharacterUtils.generateCharacter(characters.charAt(i),
                                                      true);
      }

      return images;
   }

   /**
    * Get the density maps for the output of generateFontImages().
    */
   public static double[][][] getFontDensityMaps(String characters,
                                                 int mapRows,
                                                 int mapCols) throws Exception {
      MagickImage[] characterImages = generateFontImages(characters);
      double[][][] maps = new double[characterImages.length][][];

      for (int i = 0; i < characterImages.length; i++) {
         maps[i] = getDensityMap(characterImages[i], mapRows, mapCols);
      }

      return maps;
   }
}
