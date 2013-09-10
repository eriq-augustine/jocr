package com.eriqaugustine.ocr.utils;

import com.eriqaugustine.ocr.image.Filters;
import com.eriqaugustine.ocr.utils.MathUtils;

import java.awt.Dimension;
import java.awt.Rectangle;

import magick.MagickImage;

/**
 * Some utilities for images.
 */
public class ImageUtils {
   public static final int DEFAULT_WHITE_THRESHOLD = 150;

   /**
    * Character to use when converting an image to ascii.
    * Each character gets an equal range.
    */
   public static final char[] ASCII_PLACEHOLDERS =
      {'@', '#', '*', '.', ' '};

   public static MagickImage scaleImage(MagickImage image, int newCols, int newRows)
         throws Exception {
      return image.scaleImage(newCols, newRows);
   }

   /**
    * Make an empty white image.
    */
   public static byte[] blankPixels(int imageLength) {
      byte[] rtn = new byte[imageLength];

      for (int i = 0; i < imageLength; i++) {
         rtn[i] = (byte)0xFF;
      }

      return rtn;
   }

   /**
    * Create an ascii representation of an image.
    */
   public static String asciiImage(byte[] pixels, int imageWidth, int numChannels) {
      if (numChannels > 1) {
         pixels = Filters.averageChannels(pixels, numChannels);
      }

      String rtn = "";
      // Make sure to use num values, not max value.
      double conversionRatio = 256.0 / ASCII_PLACEHOLDERS.length;

      for (int row = 0; row < pixels.length / imageWidth; row++) {
         for (int col = 0; col < imageWidth; col++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);
            rtn += ASCII_PLACEHOLDERS[(int)((0xFF & pixels[index]) / conversionRatio)];
         }
         rtn += "\n";
      }

      return rtn;
   }

   /**
    * Create an ascii representation of an image.
    * This one assumes points instead of pixels.
    */
   public static String asciiImage(boolean[] points, int imageWidth) {
      return asciiImage(discreteToPixels(points), imageWidth, 1);
   }

   public static byte[] discreteToPixels(boolean[] discrete) {
      byte[] pixels = new byte[discrete.length];

      for (int i = 0; i < discrete.length; i++) {
         pixels[i] = discrete[i] ? 0 : (byte)0xFF;
      }

      return pixels;
   }

   /**
    * Get the density of a region of an image.
    * Density is the number of non-white pixels over the size of the region.
    * |pixels| must be a single channel.
    */
   public static double density(byte[] pixels, int imageWidth,
                                int startRow, int numRows,
                                int startCol, int numCols,
                                int whiteThreshold) {
      int nonWhite = 0;

      for (int row = startRow; row < startRow + numRows; row++) {
         for (int col = startCol; col < startCol + numCols; col++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);

            if ((0xFF & pixels[index]) < whiteThreshold) {
               nonWhite++;
            }
         }
      }

      return (double)nonWhite / (numRows * numCols);
   }

   public static double density(MagickImage image, int whiteThreshold) throws Exception {
      Dimension dimensions = image.getDimension();
      byte[] pixels = Filters.averageChannels(Filters.bwPixels(image, whiteThreshold), 3);

      return density(pixels, dimensions.width,
                     0, dimensions.width,
                     0, dimensions.height,
                     whiteThreshold);
   }

   /**
    * Shrink an image so that there no bordering whitespace.
    * If the image is all whitespace, then a zero size image will be returned.
    */
   public static MagickImage shrinkImage(MagickImage image, int whiteThreshold) throws Exception {
      Dimension dimensions = image.getDimension();
      byte[] pixels = new byte[dimensions.width * dimensions.height * 3];

      image.dispatchImage(0, 0,
                          dimensions.width, dimensions.height,
                          "RGB",
                          pixels);

      byte[] meanPixels = Filters.averageChannels(pixels, 3);

      int minRow = scanRows(meanPixels, dimensions.width, whiteThreshold,
                            0, dimensions.height - 1, 1);
      int maxRow = scanRows(meanPixels, dimensions.width, whiteThreshold,
                            dimensions.height - 1, 0, -1);
      int minCol = scanCols(meanPixels, dimensions.width, whiteThreshold,
                            0, dimensions.width - 1, 1);
      int maxCol = scanCols(meanPixels, dimensions.width, whiteThreshold,
                            dimensions.width - 1, 0, -1);

      if (minRow == -1 || maxRow == -1 || minCol == -1 || maxCol == -1) {
         return image.cropImage(new Rectangle(0, 0, 1, 1));
      }

      byte[] newPixels = new byte[(maxCol - minCol + 1) * (maxRow - minRow + 1) * 3];
      for (int row = minRow; row <= maxRow; row++) {
         for (int col = minCol; col <= maxCol; col++) {
            int baseFullIndex = MathUtils.rowColToIndex(row, col, dimensions.width) * 3;
            int baseShrinkIndex = MathUtils.rowColToIndex(row - minRow, col - minCol, maxCol - minCol + 1) * 3;

            newPixels[baseShrinkIndex + 0] = pixels[baseFullIndex + 0];
            newPixels[baseShrinkIndex + 1] = pixels[baseFullIndex + 1];
            newPixels[baseShrinkIndex + 2] = pixels[baseFullIndex + 2];
         }
      }
      MagickImage newImage = new MagickImage();
      newImage.constituteImage(maxCol - minCol + 1, maxRow - minRow + 1,
                               "RGB",
                               newPixels);

      return newImage;
   }

   public static MagickImage shrinkImage(MagickImage image) throws Exception {
      return shrinkImage(image, DEFAULT_WHITE_THRESHOLD);
   }

   /**
    * Find the first occurance of a non-white pixel.
    * Will return a number between rowStart and rowEnd (inclusivley) or -1.
    * If there is no occurance of a non-white pixel, then -1 is returned.
    */
   private static int scanRows(byte[] pixels, int width, int whiteThreshold,
                               int rowStart, int rowEnd, int rowStep) {
      for (int row = rowStart; row != rowEnd + rowStep; row += rowStep) {
         for (int col = 0; col < width; col++) {
            int index = MathUtils.rowColToIndex(row, col, width);
            if ((0xFF & pixels[index]) < whiteThreshold) {
               return row;
            }
         }
      }

      return -1;
   }

   private static int scanCols(byte[] pixels, int width, int whiteThreshold,
                               int colStart, int colEnd, int colStep) {
      for (int col = colStart; col != colEnd + colStep; col += colStep) {
         for (int row = 0; row < pixels.length / width; row++) {
            int index = MathUtils.rowColToIndex(row, col, width);
            if ((0xFF & pixels[index]) < whiteThreshold) {
               return col;
            }
         }
      }

      return -1;
   }
}
