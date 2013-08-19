package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.CharacterUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import magick.MagickImage;

/**
 * Namespace for images that contain a single character.
 */
public class CharacterImage {
   public static final int DEFAULT_POINT_SIZE = 2;
   public static final double DEFAULT_POINT_DENSITY = 0.75;

   private static final double DEFAULT_OVERLAP_PERCENT = 0.50;

   /**
    * Break up the character into strokes.
    */
   // TODO: Return strokes.
   public static void getStrokes(MagickImage image) throws Exception {
      Dimension dimensions = image.getDimension();

      byte[] pixels = Filters.averageChannels(Filters.bwPixels(image, 200), 3);
      System.out.println(ImageUtils.asciiImage(pixels, dimensions.width, 1) + "\n");

      /*
      boolean[] points = discretizeLines(pixels, dimensions.width);
      System.out.println(ImageUtils.asciiImage(points, dimensions.width / DEFAULT_POINT_SIZE) + "\n");
      */

      /*
      boolean[] outline = getOutline(pixels, dimensions.width);
      System.out.println(ImageUtils.asciiImage(outline, dimensions.width) + "\n");
      */

      /*
      boolean[] points = boxizeLines(pixels, dimensions.width, 2, 1);
      System.out.println(ImageUtils.asciiImage(points, dimensions.width) + "\n");
      */

      List<List<int[]>> hSlices = horizontalCharacterSlices(pixels, dimensions.width);
      System.err.println("Horizontal Slices:");
      for (int row = 0; row < hSlices.size(); row++) {
         for (int slice = 0; slice < hSlices.get(row).size(); slice++) {
            System.err.print(String.format("   [%d, %d]",
                                           hSlices.get(row).get(slice)[0],
                                           hSlices.get(row).get(slice)[1]));
         }
         System.err.println();
      }
      System.err.println();

      List<List<int[]>> lines = slicesToLines(hSlices);
      System.err.println("Vertical Lines:");
      for (int i = 0; i < lines.size(); i++) {
         System.err.println("   Line: " + i);

         for (int[] bounds : lines.get(i)) {
            System.err.println(String.format("      [%d, %d]", bounds[0], bounds[1]));
         }
      }

      /*
      List<List<int[]>> vSlices = verticalCharacterSlices(pixels, dimensions.width);
      System.err.println("Vertical Slices:");
      for (int row = 0; row < vSlices.size(); row++) {
         for (int slice = 0; slice < vSlices.get(row).size(); slice++) {
            System.err.print(String.format("   [%d, %d]",
                                           vSlices.get(row).get(slice)[0],
                                           vSlices.get(row).get(slice)[1]));
         }
         System.err.println();
      }
      System.err.println();
      */

      // TODO(eriq).
   }

   public static List<List<int[]>> slicesToLines(List<List<int[]>> slices) {
      return slicesToLines(slices, DEFAULT_OVERLAP_PERCENT);
   }

   public static List<List<int[]>> slicesToLines(List<List<int[]>> slices, double overlapPercent) {
      List<List<int[]>> lines = new ArrayList<List<int[]>>();

      Set<Integer> usedLineParts = new HashSet<Integer>();

      // Lines that are not yet complete.
      // A line becomes complete either at the end, or when it was not used in a slice.
      List<List<int[]>> lineParts = new ArrayList<List<int[]>>();

      // The line parts that were added in the current slice.
      // These are NOT parts that were appended to.
      List<List<int[]>> newLineParts = new ArrayList<List<int[]>>();

      for (int i = 0; i < slices.size(); i++) {
         for (int[] bounds : slices.get(i)) {
            if (i == 0) {
               List<int[]> newLine = new ArrayList<int[]>();
               newLine.add(bounds);
               newLineParts.add(newLine);
            } else {
               boolean append = false;

               for (int linePartIndex = 0; linePartIndex < lineParts.size(); linePartIndex++) {
                  List<int[]> linePart = lineParts.get(linePartIndex);

                  if (absoluteOverlapPercent(bounds, linePart.get(linePart.size() - 1)) >=
                      overlapPercent) {
                     linePart.add(bounds);
                     usedLineParts.add(new Integer(linePartIndex));
                     append = true;
                     break;
                  }
               }

               if (!append) {
                  List<int[]> newLine = new ArrayList<int[]>();
                  newLine.add(bounds);
                  newLineParts.add(newLine);
               }
            }
         }

         // Remove all the unused line parts from the list.
         // Must be kept in order.
         List<Integer> toRemove = new ArrayList<Integer>();
         for (int j = 0; j < lineParts.size(); j++) {
            if (!usedLineParts.contains(new Integer(j))) {
               toRemove.add(new Integer(j));
            }
         }

         for (int j = toRemove.size() - 1; j >= 0; j--) {
            lines.add(lineParts.remove(toRemove.get(j).intValue()));
         }

         // Move the new line parts to the formal list.
         lineParts.addAll(newLineParts);
         newLineParts.clear();
      }

      // Add all the final line parts.
      lines.addAll(lineParts);

      return lines;
   }

   /**
    * Given two bounds ([start, end]), give the absolute percentage that they overlap.
    * For the ratio, the larger of the two bounds will be used.
    */
   private static double absoluteOverlapPercent(int[] a, int[] b) {
      assert(a[0] < a[1]);
      assert(b[0] < b[1]);

      int count = 0;
      for (int i = a[0]; i <= a[1]; i++) {
         if (i >= b[0] && i <= b[1]) {
            count++;
         }
      }

      return (double)count / Math.max(a[1] - a[0] + 1, b[1] - b[0] + 1);
   }

   /**
    * Take an image of a character, and slice that image horizontally.
    * Return an array of indexes that represent the start and end of a section
    * of the character in that row (slice).
    * The return would be a List<int[]>[] (to keep with the convention that known sizes
    * are arrays and not lists), but java generic array creation blocks that.
    */
   public static List<List<int[]>> horizontalCharacterSlices(byte[] pixels, int imageWidth) {
      List<List<int[]>> bounds = new ArrayList<List<int[]>>();

      for (int row = 0; row < pixels.length / imageWidth; row++) {
         bounds.add(new ArrayList<int[]>());

         int boundStart = -1;
         for (int col = 0; col < imageWidth; col++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);

            if ((0xFF & pixels[index]) == 0 && boundStart == -1) {
               boundStart = col;
            } else if ((0xFF & pixels[index]) != 0 && boundStart != -1) {
               bounds.get(row).add(new int[]{boundStart, col - 1});
               boundStart = -1;
            }
         }

         if (boundStart != -1) {
            bounds.get(row).add(new int[]{boundStart, imageWidth - 1});
         }
      }

      return bounds;
   }

   public static List<List<int[]>> verticalCharacterSlices(byte[] pixels, int imageWidth) {
      List<List<int[]>> bounds = new ArrayList<List<int[]>>();

      for (int col = 0; col < imageWidth; col++) {
         bounds.add(new ArrayList<int[]>());

         int boundStart = -1;
         for (int row = 0; row < pixels.length / imageWidth; row++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);

            if ((0xFF & pixels[index]) == 0 && boundStart == -1) {
               boundStart = row;
            } else if ((0xFF & pixels[index]) != 0 && boundStart != -1) {
               bounds.get(col).add(new int[]{boundStart, row - 1});
               boundStart = -1;
            }
         }

         if (boundStart != -1) {
            bounds.get(col).add(new int[]{boundStart, pixels.length / imageWidth - 1});
         }
      }

      return bounds;
   }

   /**
    * Does the same as discretizeLines(), except the resulting image is the same size.
    */
   public static boolean[] boxizeLines(byte[] pixels, int imageWidth,
                                       int pointSize, double pointDensity) {
      boolean[] points = new boolean[pixels.length];

      for (int row = 0; row < pixels.length / imageWidth; row++) {
         for (int col = 0; col < imageWidth; col++) {
            int pixelCount = 0;

            for (int pointRowOffset = 0; pointRowOffset < pointSize; pointRowOffset++) {
               for (int pointColOffset = 0; pointColOffset < pointSize; pointColOffset++) {
                  int index = MathUtils.rowColToIndex(row + pointRowOffset,
                                                      col + pointColOffset,
                                                      imageWidth);

                  if (index < pixels.length && pixels[index] == 0) {
                     pixelCount++;
                  }
               }
            }

            if (pixelCount / (double)(pointSize * pointSize) >= pointDensity) {
               points[MathUtils.rowColToIndex(row, col, imageWidth)] = true;
            }
         }
      }

      return points;
   }

   public static boolean[] boxizeLines(byte[] pixels, int imageWidth) {
      return boxizeLines(pixels, imageWidth, DEFAULT_POINT_SIZE, DEFAULT_POINT_DENSITY);
   }

   /**
    * Get the outlines for an image, only border pixels willbe shown.
    * |pixels| is assumed to be bw.
    */
   public static boolean[] getOutline(byte[] pixels, int imageWidth) {
      boolean[] outline = new boolean[pixels.length];
      int[][] neighborOffsets = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

      for (int row = 0; row < pixels.length / imageWidth; row++) {
         for (int col = 0; col < imageWidth; col++) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);
            boolean border = false;

            // Only consider occupied pixels.
            if ((pixels[index] & 0xFF) == 0xFF) {
               continue;
            }

            // Any pixel touching an edge is automatically a border.
            if (row == 0 || row == pixels.length / imageWidth - 1 ||
                col == 0 || col == imageWidth - 1) {
               border = true;
            } else {
               for (int[] neighborOffset : neighborOffsets) {
                  int newRow = row + neighborOffset[0];
                  int newCol = col + neighborOffset[1];
                  int newIndex = MathUtils.rowColToIndex(newRow, newCol, imageWidth);

                  // Not enough to check index bounds because it could be on vertical edge.
                  // If the pixel touches any whitespace, it is a border.
                  if (newRow >= 0 && newRow < pixels.length / imageWidth &&
                      newCol >= 0 && newCol < imageWidth &&
                      (pixels[newIndex] & 0xFF) == 0xFF) {
                     border = true;
                     break;
                  }
               }
            }

            if (border) {
               outline[index] = true;
            }
         }
      }

      return outline;
   }

   /**
    * Turns |pixels| into a more defined set of points.
    * A point can be a single pixel, or a box of pixels.
    * Assumes that |pixels| is bw.
    * |pointSize| is the length of one of the sides of the box.
    * The resulting image will be
    * (|imageWidth| / |pointSize|) x (|pixels|.length / |imageWidth| / |pointSize|)
    */
   public static boolean[] discretizeLines(byte[] pixels, int imageWidth,
                                           int pointSize, double pointDensity) {
      int newWidth = imageWidth / pointSize;
      int newHeight = pixels.length / imageWidth / pointSize;

      boolean[] points = new boolean[newWidth * newHeight];

      for (int row = 0; row < newHeight; row++) {
         for (int col = 0; col < newWidth; col++) {
            int pixelCount = 0;

            for (int pointRowOffset = 0; pointRowOffset < pointSize; pointRowOffset++) {
               for (int pointColOffset = 0; pointColOffset < pointSize; pointColOffset++) {
                  int index = MathUtils.rowColToIndex((row * pointSize) + pointRowOffset,
                                                      (col * pointSize) + pointColOffset,
                                                      imageWidth);

                  if (index < pixels.length && pixels[index] == 0) {
                     pixelCount++;
                  }
               }
            }

            if (pixelCount / (double)(pointSize * pointSize) >= pointDensity) {
               points[MathUtils.rowColToIndex(row, col, newWidth)] = true;
            }
         }
      }

      return points;
   }

   public static boolean[] discretizeLines(byte[] pixels, int imageWidth) {
      return discretizeLines(pixels, imageWidth, DEFAULT_POINT_SIZE, DEFAULT_POINT_DENSITY);
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
