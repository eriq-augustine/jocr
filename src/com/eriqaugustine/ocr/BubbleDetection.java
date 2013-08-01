package com.eriqaugustine.ocr;

import com.eriqaugustine.ocr.Filters;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PaintMethod;
import magick.PixelPacket;

public class BubbleDetection {
   public static final int DEFAULT_MIN_BLOB_SIZE = 1000;

   /**
    * Color all the text bubbles.
    * It is assumed that |image| has been edged, and that it has
    *  only two colors, true black and true white.
    * White pixels are edges.
    */
   public static MagickImage findBubbles(MagickImage image) throws Exception {
      // TODO(eriq): Take in base image and do all necessary transforms.

      Dimension dimensions = image.getDimension();

      // Note: bw pixels pulls out three values for each pixel.
      //  Therefore, visited only needs to be one third the size.
      byte[] pixels = Filters.bwPixels(image);

      // {blob start point -> blob size}
      Map<Integer, Integer> blobs = getBlobs(dimensions.width, pixels);

      // Fill the blobs.
      fillBlobs(pixels, blobs, dimensions.width);

      MagickImage newImage = new MagickImage();
      newImage.constituteImage(dimensions.width, dimensions.height,
                               "RGB",
                               pixels);

      return newImage;
   }

   /**
    * Modify |pixels| to fill in all the blobs as red.
    */
   private static void fillBlobs(byte[] pixels,
                                 Map<Integer, Integer> blobs,
                                 int width) {
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

      for (Integer blobStart : blobs.keySet()) {
         toVisit.add(blobStart);
         visited[blobStart.intValue()] = true;

         while (!toVisit.isEmpty()) {
            int index = toVisit.remove().intValue();

            // Mark the blobs as red.
            int pixelIndex = index * 3;
            pixels[pixelIndex + 0] = (byte)0xFF;
            pixels[pixelIndex + 1] = 0;
            pixels[pixelIndex + 2] = 0;

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
               }
            }
         }
      }
   }

   /**
    * Get the large blobs.
    * Note that |pixels| is three times the length of the image because
    *  it has RGB.
    */
   private static Map<Integer, Integer> getBlobs(int width, byte[] pixels) {
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

      // {blob start point -> blob size}
      Map<Integer, Integer> blobs = new HashMap<Integer, Integer>();

      // Depth-first w.r.t. blobs.
      for (int i = 0; i < visited.length; i++) {
         if (!visited[i]) {
            int blobStart = i;
            int blobSize = 0;
            toVisit.add(new Integer(blobStart));
            visited[blobStart] = true;

            while (!toVisit.isEmpty()) {
               int index = toVisit.remove().intValue();
               blobSize++;

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
                  }
               }
            }

            if (blobSize > DEFAULT_MIN_BLOB_SIZE) {
               blobs.put(blobStart, blobSize);
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

      return index >= 0 &&
             index < length &&
             // Vertical
             ((Math.abs(index - base) == width) ||
             // Horozontal, needs an extra check because of wrapping.
             (Math.abs(index - base) == 1 &&
              index >= baseRowStart &&
              index < baseRowStart + width));
   }
}
