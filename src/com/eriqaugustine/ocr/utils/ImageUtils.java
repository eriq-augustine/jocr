package com.eriqaugustine.ocr.utils;

import com.eriqaugustine.ocr.image.WrapImage;
import static com.eriqaugustine.ocr.image.WrapImage.Pixel;

import java.util.ArrayList;
import java.util.List;

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

   /**
    * Get the peripheral (edge) points for an image.
    * The image will be scaled to |scaleSize| x |scaleSize| before processing.
    * |numLaters| is the number of foreground objects a ray is allowed to pass through before it is finally stopped.
    *  Each peripheral point along the way will be returned.
    * The number of points returned will always be the same size.
    * If there are no more peripheral points in some scanning direction, then a null will be interted in-place of the points.
    */
   public static List<Integer> getPeripheralPoints(
         WrapImage image,
         int scaleSize,
         int numLayers,
         boolean includeDiagonal) {
      image = image.copy();
      image.scale(scaleSize, scaleSize);

      boolean[] discretePixels = image.getDiscretePixels();

      return getPeripheralPoints(discretePixels, scaleSize, numLayers, includeDiagonal);
   }

   /**
    * Get the peripheral points for an already discrete set of pixels.
    * This should be used if the caller needs the discrete pixels for other work and does not want
    * want to recreate them.
    * |discretePixels| must be a |imageSideLength| x |imageSideLength| square.
    */
   public static List<Integer> getPeripheralPoints(
         boolean[] discretePixels,
         int imageSideLength,
         int numLayers,
         boolean includeDiagonal) {
      assert(discretePixels.length == imageSideLength * imageSideLength);

      int numScanDirections = includeDiagonal ? 8 : 4;
      int numPeripherals = imageSideLength * numScanDirections * numLayers;

      List<Integer> peripherals = new ArrayList<Integer>(numPeripherals);

      int half = (int)(Math.ceil(imageSideLength / 2.0));
      int last = imageSideLength - 1;


      // Layers
      for (int i = 0; i < numLayers; i++) {
         // Horizontal LTR
         peripherals.addAll(scan(discretePixels, imageSideLength, i, true, true));

         // Horizontal RTL
         peripherals.addAll(scan(discretePixels, imageSideLength, i, true, false));

         // Vertial Down
         peripherals.addAll(scan(discretePixels, imageSideLength, i, false, true));

         // Vertical Up
         peripherals.addAll(scan(discretePixels, imageSideLength, i, false, false));

         // Diagnals
         if (includeDiagonal) {
            // Top Left to Bottom Right
            peripherals.addAll(diagonalScan(discretePixels, imageSideLength, i, 0, half, 0,
                                                                                0, half, 0,
                                                                                1, 1));

            // Top Right to Bottom Left
            peripherals.addAll(diagonalScan(discretePixels, imageSideLength, i, last - half, last, 0,
                                                                                0, half, last,
                                                                                1, -1));

            // Bottom Left to Top Right
            peripherals.addAll(diagonalScan(discretePixels, imageSideLength, i, 0, half, last,
                                                                                last - half, last, 0,
                                                                                -1, 1));

            // Bottom Right to Top Left
            peripherals.addAll(diagonalScan(discretePixels, imageSideLength, i, last - half, last, last,
                                                                                last - half, last, last,
                                                                                -1, -1));
         }
      }

      assert(peripherals.size() == numPeripherals);

      return peripherals;
   }

   /**
    * Scan a direction and get all of the peripheral (edge) points.
    * |layerNumber| is the number of solid bodies to pass through before the final
    * peripheral edge. 0 means that it will pass through no bodies.
    * There will be a result for EVERY row/col that is scanned.
    * If a row/col has no peripheral point, then a null will be the result.
    */
   private static List<Integer> scan(boolean[] image,
                                     int imageWidth,
                                     int layerNumber,
                                     boolean horizontal,
                                     boolean forward) {
      assert(layerNumber >= 0);

      // The end points should be out of bounds.
      int outerStart, outerEnd, outerDelta;
      int innerStart, innerEnd, innerDelta;
      List<Integer> peripherals;

      if (horizontal) {
         // row
         outerStart = 0;
         outerEnd = image.length / imageWidth;
         outerDelta = 1;

         // col
         innerStart = forward ? 0 : imageWidth - 1;
         innerEnd = forward ? imageWidth : -1;
         innerDelta = forward ? 1 : -1;
      } else {
         // col
         outerStart = 0;
         outerEnd = imageWidth;
         outerDelta = 1;

         // row
         innerStart = forward ? 0 : image.length / imageWidth - 1;
         innerEnd = forward ? image.length / imageWidth : -1;
         innerDelta = forward ? 1 : -1;
      }

      peripherals = new ArrayList<Integer>(outerEnd);

      for (int outer = outerStart; outer != outerEnd; outer += outerDelta) {
         int currentLayerCount = 0;
         boolean inBody = false;
         boolean found = false;

         for (int inner = innerStart; inner != innerEnd; inner += innerDelta) {
            int index = horizontal ? MathUtils.rowColToIndex(outer, inner, imageWidth) :
                                     MathUtils.rowColToIndex(inner, outer, imageWidth);

            if (image[index] && !inBody) {
               inBody = true;

               if (currentLayerCount == layerNumber) {
                  peripherals.add(new Integer(index));
                  found = true;
                  break;
               }
            } else if (!image[index] && inBody) {
               inBody = false;
               currentLayerCount++;
            }
         }

         if (!found) {
            peripherals.add(null);
         }
      }

      assert(peripherals.size() == outerEnd);

      return peripherals;
   }

   /**
    * Do a diagonal scan and get the peripheral points.
    * See scan(), this function follows all the same conventions.
    * The very short diagonals (for ex, the one length ones at the corners)
    * are not very useful. So, only the longest diagonals are used.
    * This is why we need all the additional parameters.
    */
   private static List<Integer> diagonalScan(boolean[] image,
                                             int imageWidth,
                                             int layerNumber,
                                             int colStart, int colStop, int baseRow,
                                             int rowStart, int rowStop, int baseCol,
                                             int rowDelta, int colDelta) {
      List<Integer> peripherals = new ArrayList<Integer>();

      int[][] startPoints = new int[colStop - colStart + rowStop - rowStart][];
      int count = 0;
      for (int col = colStart; col < colStop; col++) {
         startPoints[count] = new int[]{baseRow, col};
         count++;
      }
      for (int row = rowStart; row < rowStop; row++) {
         startPoints[count] = new int[]{row, baseCol};
         count++;
      }

      for (int[] startPoint : startPoints) {
         int diagonalRow = startPoint[0];
         int diagonalCol = startPoint[1];

         int currentLayer = 0;
         boolean inBody = false;
         boolean found = false;

         while (diagonalRow >= 0 && diagonalRow < image.length / imageWidth &&
                diagonalCol >= 0 && diagonalCol < imageWidth) {
            int index = MathUtils.rowColToIndex(diagonalRow, diagonalCol, imageWidth);

            if (image[index] && !inBody) {
               inBody = true;

               if (currentLayer == layerNumber) {
                  peripherals.add(new Integer(index));
                  found = true;
                  break;
               }
            } else if (!image[index] && inBody) {
               inBody = false;
               currentLayer++;
            }

            diagonalRow += rowDelta;
            diagonalCol += colDelta;
         }

         if (!found) {
            peripherals.add(null);
         }
      }

      return peripherals;
   }
}
