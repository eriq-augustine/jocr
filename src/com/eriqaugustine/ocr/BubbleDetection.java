package com.eriqaugustine.ocr;

import com.eriqaugustine.ocr.Filters;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PaintMethod;
import magick.PixelPacket;

public class BubbleDetection {
   public static final int DEFAULT_MIN_BLOB_SIZE = 2000;

   // The minimum density for a blob on the first pass.
   public static final double DEFAULT_MIN_BLOB_DENSITY_1 = 0.40;

   // Second pass.
   public static final double DEFAULT_MIN_BLOB_DENSITY_2 = 0.70;

   public static Map<Integer, Blob> getBubbles(MagickImage image) throws Exception {
      image = Filters.bw(image, 40).edgeImage(3);

      Dimension dimensions = image.getDimension();

      // Note: bw pixels pulls out three values for each pixel.
      byte[] pixels = Filters.bwPixels(image);

      // {blob identifier -> blob size}
      Map<Integer, Blob> blobs = getBlobs(dimensions.width, pixels);

      return blobs;
   }

   /**
    * Color all the text bubbles.
    * It is assumed that |image| has been edged, and that it has
    *  only two colors, true black and true white.
    * White pixels are edges.
    */
   public static MagickImage fillBubbles(MagickImage image) throws Exception {
      Map<Integer, Blob> bubbles = getBubbles(image);

      Dimension dimensions = image.getDimension();
      byte[] pixels = new byte[dimensions.width * dimensions.height * 3];

      image.dispatchImage(0, 0,
                          dimensions.width, dimensions.height,
                          "RGB",
                          pixels);

      // Fill the blobs.
      fillBlobs(pixels, bubbles);

      MagickImage newImage = new MagickImage();
      newImage.constituteImage(dimensions.width, dimensions.height,
                               "RGB",
                               pixels);

      return newImage;
   }

   /**
    * Modify |pixels| to fill in all the blobs as red.
    */
   private static void fillBlobs(byte[] pixels, Map<Integer, Blob> blobs) {
      for (Blob blob : blobs.values()) {
         for (Integer index : blob.getPoints()) {
            int pixelIndex = index.intValue() * 3;

            // Mark the blobs as red.
            pixels[pixelIndex + 0] = (byte)0xFF;
            pixels[pixelIndex + 1] = 0;
            pixels[pixelIndex + 2] = 0;
         }
      }
   }

   /**
    * Get the large blobs.
    * Note that |pixels| is three times the length of the image because
    *  it has RGB.
    */
   private static Map<Integer, Blob> getBlobs(int width, byte[] pixels) {
      Map<Integer, Blob> blobs = new HashMap<Integer, Blob>();

      boolean[] visited = new boolean[pixels.length / 3];
      Queue<Integer> toVisit = new LinkedList<Integer>();

      // All the offsets to check for blobs.
      // left, right, up, down
      int[] offsets = {-1, 1,
                       -1 * width, 1 * width};

      // Fill the visited pixels with edges.
      for (int i = 0; i < visited.length; i++) {
         if ((0xFF & pixels[i * 3]) == 255) {
            visited[i] = true;
         }
      }

      // Depth-first w.r.t. blobs.
      for (int i = 0; i < visited.length; i++) {
         if (!visited[i]) {
            // Keep track of the dimensions of the blob for density calculations.
            Blob blob = new Blob(visited.length, width);

            toVisit.add(new Integer(i));
            visited[i] = true;

            blob.addPoint(i);

            while (!toVisit.isEmpty()) {
               int index = toVisit.remove().intValue();

               // Check all neighbors
               // No need to check color, only visited is necessary
               //  since the edges have already been marked as visited.
               for (int offset : offsets) {
                  int newIndex = index + offset;
                  if (inBoundsAdjacent(index, newIndex, width, visited.length) &&
                      !visited[newIndex]) {
                     toVisit.add(newIndex);

                     // Mark as visited a little early so that it is not added multiple times.
                     visited[newIndex] = true;

                     blob.addPoint(newIndex);
                  }
               }
            }

            // Do a quick check for size and border before shifting geometry.
            if (blob.size() > DEFAULT_MIN_BLOB_SIZE &&
                !blob.isBorderBlob() &&
                blob.density() >= DEFAULT_MIN_BLOB_DENSITY_1) {
               blob.gemoetryAdjust();

               // Recheck the size after the geometry adjust.
               if (blob.size() > DEFAULT_MIN_BLOB_SIZE &&
                   blob.density() >= DEFAULT_MIN_BLOB_DENSITY_2) {
                  blobs.put(blob.getId(), blob);
               }
            }
         }
      }

      return blobs;
   }

   // Endure that |index| is adjacent to |base|.
   // Assume both of these live in a 1D vector representing a 2D plain.
   private static boolean inBoundsAdjacent(int base, int index,
                                           int width, int length) {
      int baseRowStart = base / width * width;

      return index >= 0 && index < length &&
             // Vertical
             ((Math.abs(index - base) == width) ||
             // Horozontal, needs an extra check because of wrapping.
             (Math.abs(index - base) == 1 &&
              index >= baseRowStart &&
              index < baseRowStart + width));
   }
}
