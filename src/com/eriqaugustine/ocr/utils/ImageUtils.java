package com.eriqaugustine.ocr.utils;

import com.eriqaugustine.ocr.image.Filters;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PixelPacket;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Some utilities for images.
 */
public class ImageUtils {
   public static final int DEFAULT_WHITE_THRESHOLD = 150;

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

      MagickImage image = generateString("Hello, World!", false, 50, 50);
      image.setFileName(outDirectory + "/genString.png");
      image.writeImage(new ImageInfo());
      */

      ImageInfo info = new ImageInfo("training/kana/あ_h.png");
      MagickImage image = new MagickImage(info);
      double[] densities = regionDensity(image, 128);

      for (int i = 0; i < densities.length; i++) {
         System.out.println(i + " -- " + densities[i]);
      }
   }

   /**
    * Combine the images into a single image grid.
    * This is intended for characters, so it assumes that all the characters are the same size.
    */
   public static MagickImage gridCombine(MagickImage[] images, int gridWidth) throws Exception {
      assert(images.length > 0);

      Dimension characterDimensions = images[0].getDimension();

      // Give some padding.
      int charWidth = characterDimensions.width + 30;
      int charHeight = characterDimensions.height + 30;

      int totalWidth = charWidth * gridWidth;
      int totalHeight = charHeight * (int)Math.ceil(images.length / (double)gridWidth);

      // Not all the images are exactly the same, so be safe.
      MagickImage baseImage = blankImage(totalWidth * 2, totalHeight * 2);

      for (int i = 0; i < images.length; i++) {
         int row = i / gridWidth;
         int col = i - (row * gridWidth);

         overlayImage(baseImage, images[i],
                      row * charHeight,
                      col * charWidth);
      }

      return shrinkImage(baseImage);
   }

   /**
    * Overlay |overlay| over |base|.
    */
   public static MagickImage overlayImage(MagickImage baseImage, MagickImage overlayImage,
                                          int rowOffset, int colOffset) throws Exception {
      Dimension baseDimensions = baseImage.getDimension();
      Dimension overlayDimensions = overlayImage.getDimension();

      assert(rowOffset >= 0 && rowOffset < baseDimensions.height);
      assert(colOffset >= 0 && colOffset < baseDimensions.width);
      assert(rowOffset + overlayDimensions.height < baseDimensions.height);
      assert(colOffset + overlayDimensions.width < baseDimensions.width);

      byte[] basePixels = new byte[baseDimensions.width * baseDimensions.height * 3];
      baseImage.dispatchImage(0, 0,
                              baseDimensions.width, baseDimensions.height,
                              "RGB",
                              basePixels);

      byte[] overlayPixels = new byte[overlayDimensions.width * overlayDimensions.height * 3];
      overlayImage.dispatchImage(0, 0,
                                 overlayDimensions.width, overlayDimensions.height,
                                 "RGB",
                                 overlayPixels);

      for (int row = 0; row < overlayDimensions.height; row++) {
         for (int col = 0; col < overlayDimensions.width; col++) {
            // Make sure to offset for the channels.
            int baseIndex = 3 * MathUtils.rowColToIndex(rowOffset + row,
                                                        colOffset + col,
                                                        baseDimensions.width);
            int overlayIndex = 3 * MathUtils.rowColToIndex(row, col, overlayDimensions.width);

            basePixels[baseIndex + 0] = overlayPixels[overlayIndex + 0];
            basePixels[baseIndex + 1] = overlayPixels[overlayIndex + 1];
            basePixels[baseIndex + 2] = overlayPixels[overlayIndex + 2];
         }
      }

      baseImage.constituteImage(baseDimensions.width, baseDimensions.height,
                                "RGB",
                                basePixels);

      return baseImage;
   }

   /**
    * Generate an image that contains the text from |content|.
    * ImageMagick is strange and easiest way to get word wrap is to use the "caption" feature.
    * The same tactic as CharacterUtils.generateCharacter() is not used.
    */
   public static MagickImage generateString(String content, boolean shrink,
                                            int maxWidth, int maxHeight) throws Exception {
      ImageInfo info = new ImageInfo("caption: " + content);
      info.setSize(String.format("%dx%d", maxWidth, maxHeight));
      info.setFont("Arial");

      MagickImage image = new MagickImage(info);

      if (shrink) {
         return shrinkImage(image);
      }

      return image;
   }

   public static MagickImage scaleImage(MagickImage image, int newCols, int newRows)
         throws Exception {
      return image.scaleImage(newCols, newRows);
   }

   /**
    * Make an empty image.
    * Right now 1x1 images are "empty".
    */
   public static MagickImage emptyImage() throws Exception {
      MagickImage rtn = new MagickImage();
      rtn.constituteImage(1, 1, "R", new byte[]{(byte)0xFF});
      return rtn;
   }

   /**
    * Return true if the given image is empty, usually representing a space.
    */
   public static boolean isEmptyImage(MagickImage image) throws Exception {
      return image.getDimension().width == 1;
   }

   /**
    * Make an empty white image.
    */
   public static MagickImage blankImage(int imageWidth, int imageHeight) throws Exception {
      byte[] pixels = blankPixels(imageWidth * imageHeight);
      MagickImage rtn = new MagickImage();
      rtn.constituteImage(imageWidth, imageHeight, "R", pixels);
      return rtn;
   }

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

   public static String asciiImage(MagickImage image) throws Exception {
      Dimension dimensions = image.getDimension();
      byte[] pixels = new byte[dimensions.width * dimensions.height * 3];

      image.dispatchImage(0, 0,
                          dimensions.width, dimensions.height,
                          "RGB",
                          pixels);

      return asciiImage(pixels, dimensions.width, 3);
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
                     0, dimensions.height,
                     0, dimensions.width,
                     whiteThreshold);
   }

   public static double[] regionDensities(MagickImage image,
                                          int whiteThreshold,
                                          int regionsPerSide) throws Exception {
      Dimension dimensions = image.getDimension();
      byte[] pixels = Filters.averageChannels(Filters.bwPixels(image, whiteThreshold), 3);

      double[] regionDensities = new double[regionsPerSide * regionsPerSide];

      for (int regionRow = 0; regionRow < regionsPerSide; regionRow++) {
         for (int regionCol = 0; regionCol < regionsPerSide; regionCol++) {
            regionDensities[regionRow * regionsPerSide + regionCol] =
                  density(pixels, dimensions.width,
                          regionRow * dimensions.height / regionsPerSide,
                          dimensions.height / regionsPerSide,
                          regionCol * dimensions.width / regionsPerSide,
                          dimensions.width / regionsPerSide,
                          whiteThreshold);
         }
      }

      return regionDensities;
   }

   public static double[] regionDensity(MagickImage image,
                                        int whiteThreshold) throws Exception {
      return regionDensities(image, whiteThreshold, DEFAULT_IMAGE_DIVISION);
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
            int baseShrinkIndex = MathUtils.rowColToIndex(row - minRow,
                                                          col - minCol,
                                                          maxCol - minCol + 1) * 3;

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
