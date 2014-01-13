package com.eriqaugustine.ocr.utils;

import com.eriqaugustine.ocr.image.WrapImage;
import static com.eriqaugustine.ocr.image.WrapImage.Pixel;

/**
 * Some utilities for images.
 */
public class ImageUtils {
   public static final int DEFAULT_IMAGE_DIVISION = 3;

   /**
    * Character to use when converting an image to ascii.
    * Each character gets an equal range.
    */
   public static final char[] ASCII_PLACEHOLDERS =
      {'@', '#', '*', '.', ' '};

   /**
    * A testing main.
    */
   public static void main(String[] args) throws Exception {
      /*
      String outDirectory = FileUtils.itterationDir("out", "imageUtils");

      WrapImage image = WrapImage.getStringImage("Hello, World!", false, 50, 50);
      image.write(outDirectory + "/genString.png");
      */

      WrapImage image = WrapImage.getImageFromFile("training/kana/あ_h.png");
      double[] densities = regionDensity(image, 128);

      for (int i = 0; i < densities.length; i++) {
         System.out.println(i + " -- " + densities[i]);
      }
   }

   /**
    * Combine the images into a single image grid.
    * This is intended for characters, so it assumes that all the characters are the same size.
    */
   public static WrapImage gridCombine(WrapImage[] images, int gridWidth) {
      assert(images.length > 0);

      // Give some padding.
      int charWidth = images[0].width() + 30;
      int charHeight = images[0].height() + 30;

      int totalWidth = charWidth * gridWidth;
      int totalHeight = charHeight * (int)Math.ceil(images.length / (double)gridWidth);

      // Not all the images are exactly the same, so be safe.
      WrapImage baseImage = WrapImage.getBlankImage(totalWidth * 2, totalHeight * 2);

      for (int i = 0; i < images.length; i++) {
         int row = i / gridWidth;
         int col = i - (row * gridWidth);

         baseImage = overlayImage(baseImage, images[i], row * charHeight, col * charWidth);
      }

      return baseImage.shrink();
   }

   /**
    * Overlay |overlay| over |base|.
    */
   public static WrapImage overlayImage(WrapImage baseImage, WrapImage overlayImage,
                                        int rowOffset, int colOffset) {
      assert(rowOffset >= 0 && rowOffset < baseImage.height());
      assert(colOffset >= 0 && colOffset < baseImage.width());
      assert(rowOffset + overlayImage.height() < baseImage.height());
      assert(colOffset + overlayImage.width() < baseImage.width());

      Pixel[] basePixels = baseImage.getPixels(true);
      Pixel[] overlayPixels = overlayImage.getPixels(true);

      for (int row = 0; row < overlayImage.height(); row++) {
         for (int col = 0; col < overlayImage.width(); col++) {
            int baseIndex = MathUtils.rowColToIndex(rowOffset + row,
                                                    colOffset + col,
                                                    baseImage.width());
            int overlayIndex = MathUtils.rowColToIndex(row, col, overlayImage.width());

            basePixels[baseIndex] = overlayPixels[overlayIndex];
         }
      }

      return WrapImage.getImageFromPixels(basePixels, baseImage.width(), baseImage.height());
   }

   /**
    * Create an ascii representation of an image.
    */
   public static String asciiImage(Pixel[] pixels, int imageWidth) {
      // One for each pixel plus newlines.
      char[] rtn = new char[pixels.length + pixels.length / imageWidth];
      int numNewlines = 0;

      // Make sure to use num values, not max value.
      double conversionRatio = 256.0 / ASCII_PLACEHOLDERS.length;

      for (int row = 0; row < pixels.length / imageWidth; row++) {
         for (int col = 0; col < imageWidth; col++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);
            rtn[index + numNewlines] =
               ASCII_PLACEHOLDERS[(int)((0xFF & pixels[index].average()) / conversionRatio)];
         }

         rtn[(row * imageWidth) + numNewlines] = '\n';
         numNewlines++;
      }

      return new String(rtn);
   }

   public static String asciiImage(WrapImage image) {
      return asciiImage(image.getPixels(), image.width());
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
    */
   public static double density(boolean[] pixels, int imageWidth,
                                int startRow, int numRows,
                                int startCol, int numCols,
                                int whiteThreshold) {
      int nonWhite = 0;

      for (int row = startRow; row < startRow + numRows; row++) {
         for (int col = startCol; col < startCol + numCols; col++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);

            if (pixels[index]) {
               nonWhite++;
            }
         }
      }

      return (double)nonWhite / (numRows * numCols);
   }

   public static double density(WrapImage image, int whiteThreshold) {
      boolean[] pixels = image.getDiscretePixels(whiteThreshold);

      return density(pixels, image.width(),
                     0, image.height(),
                     0, image.width(),
                     whiteThreshold);
   }

   public static double[] regionDensities(WrapImage image,
                                          int whiteThreshold,
                                          int regionsPerSide) {
      boolean[] pixels = image.getDiscretePixels(whiteThreshold);

      double[] regionDensities = new double[regionsPerSide * regionsPerSide];

      for (int regionRow = 0; regionRow < regionsPerSide; regionRow++) {
         for (int regionCol = 0; regionCol < regionsPerSide; regionCol++) {
            regionDensities[regionRow * regionsPerSide + regionCol] =
                  density(pixels, image.width(),
                          regionRow * image.height() / regionsPerSide,
                          image.height() / regionsPerSide,
                          regionCol * image.width() / regionsPerSide,
                          image.width() / regionsPerSide,
                          whiteThreshold);
         }
      }

      return regionDensities;
   }

   public static double[] regionDensity(WrapImage image,
                                        int whiteThreshold) throws Exception {
      return regionDensities(image, whiteThreshold, DEFAULT_IMAGE_DIVISION);
   }
}
