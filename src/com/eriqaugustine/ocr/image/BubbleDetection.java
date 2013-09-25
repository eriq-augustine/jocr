package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.math.BinaryConfusionMatrix;
import com.eriqaugustine.ocr.utils.ColorUtils;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import magick.ImageInfo;
import magick.MagickImage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A static class for detecting callouts (speech bubbles).
 */
public class BubbleDetection {
   // BEGIN deprecated, use ratios.
   public static final int DEFAULT_MIN_CHARACTER_BLOB_SIZE = 100;
   public static final int DEFAULT_MAX_CHARACTER_BLOB_SIZE = 3000;

   // Minimum size for a callout candidate blob.
   public static final int DEFAULT_MIN_CALLOUT_BLOB_SIZE = 1000;
   public static final int DEFAULT_MAX_CALLOUT_BLOB_SIZE = 100000;
   // END deprecated.

   // The minimum density for a blob on the first pass.
   public static final double DEFAULT_MIN_BLOB_DENSITY_1 = 0.40;

   // Second pass.
   //public static final double DEFAULT_MIN_BLOB_DENSITY_2 = 0.70;
   public static final double DEFAULT_MIN_BLOB_DENSITY_2 = 0.60;

   // Size ratios (ratio of the entire image size).
   private static final double MIN_CHARACTER_RATIO = 0.00001;
   private static final double MAX_CHARACTER_RATIO = 0.003;

   //private static final double MAX_CHARACTER_BOUNDING_RATIO = 0.00074173;
   private static final double MAX_CHARACTER_BOUNDING_RATIO = 0.005;
   private static final double MAX_CHARACTER_BOUNDING_LENGTH_RATIO = 0.0001;

   private static final double MIN_CALLOUT_RATIO = 0.00075;
   private static final double MAX_CALLOUT_RATIO = 0.075;
   private static final double MAX_CALLOUT_BOUNDING_RATIO = 0.1;

   /**
    * Run a detection test on the given image.
    * Reuse |matrix| for multiple images to get an overall score.
    * The resulting image will have the bubbles filled.
    * Note: The training sets are constructed so that there are no two valid bubbles inside
    *  of a single training bounding box. So, if there are multiples, all but one are FP.
    */
   public static MagickImage bubbleFillTest(String imageFile,
                                            FileUtils.BubbleTrainingSet trainingData,
                                            BinaryConfusionMatrix matrix) throws Exception {
      String baseImageName = new File(imageFile).getName();

      ImageInfo imageInfo = new ImageInfo(imageFile);
      MagickImage image = new MagickImage(imageInfo);

      List<Blob> bubbles = getBubbles(image);

      int foundCount = 0;
      List<Point[]> bounds = trainingData.trainingBubbles.get(baseImageName);
      if (bounds != null) {
         for (Blob bubble : bubbles) {
            boolean found = false;

            for (Point[] trainingBounds : bounds) {
               if (trainingBounds[0].y <= bubble.getMinRow() &&
                   trainingBounds[1].y >= bubble.getMaxRow() &&
                   trainingBounds[0].x <= bubble.getMinCol() &&
                   trainingBounds[1].x >= bubble.getMaxCol()) {
                  if (!found) {
                     // TP
                     foundCount++;
                     found = true;
                     matrix.truePositive();
                  } else {
                     // This blob was already marked
                     // FP
                     matrix.falsePositive();
                  }
               }
            }

            if (!found) {
               // FP
               matrix.falsePositive();
            }
         }

         // The rest of the blobs were not found, they are FN.
         for (int i = foundCount; i < bounds.size(); i++) {
            matrix.falseNegative();
         }
      }

      return colorBubbles(image, bubbles);
   }

   /**
    * Get the raw blobs that represent the bubbles.
    */
   public static List<Blob> getBubbles(MagickImage image) throws Exception {
      image = image.blurImage(3, 1);
      image = Filters.bw(image, 200);

      byte[] rawPixels = Filters.averageChannels(Filters.bwPixels(image, 200), 3);

      image = image.edgeImage(3);

      Dimension dimensions = image.getDimension();

      byte[] edgedPixels = Filters.averageChannels(Filters.bwPixels(image), 3);

      List<Blob> blobs = getBubbles(dimensions.width, edgedPixels, rawPixels);

      return blobs;
   }

   /**
    * Extract the pixels for each bubble and convert them to an image.
    */
   public static MagickImage[] extractBubbles(MagickImage image) throws Exception {
      BubbleInfo[] bubbles = extractBubblesWithInfo(image);
      MagickImage[] images = new MagickImage[bubbles.length];
      for (int i = 0; i < bubbles.length; i++) {
         images[i] = bubbles[i].image;
      }
      return images;
   }

   public static BubbleInfo[] extractBubblesWithInfo(MagickImage image) throws Exception {
      List<Blob> bubbles = getBubbles(image);

      Dimension dimensions = image.getDimension();
      byte[] pixels = new byte[dimensions.width * dimensions.height * 3];

      image.dispatchImage(0, 0,
                          dimensions.width, dimensions.height,
                          "RGB",
                          pixels);

      BubbleInfo[] infos = new BubbleInfo[bubbles.size()];

      int count = 0;
      for (Blob blob : bubbles) {
         byte[] blobPixels = new byte[blob.getBoundingSize() * 3];
         Map<Integer, int[]> bounds = blob.getBoundaries();

         int width = blob.getBoundingWidth();

         for (int row = blob.getMinRow(); row <= blob.getMaxRow(); row++) {
            for (int col = blob.getMinCol(); col <= blob.getMaxCol(); col++) {
               int baseImageIndex = 3 * (row * dimensions.width + col);
               int baseBlobIndex = 3 *
                                   ((row - blob.getMinRow()) * width + (col - blob.getMinCol()));

               if (col < bounds.get(row)[0] || col > bounds.get(row)[1]) {
                  blobPixels[baseBlobIndex + 0] = (byte)0xFF;
                  blobPixels[baseBlobIndex + 1] = (byte)0xFF;
                  blobPixels[baseBlobIndex + 2] = (byte)0xFF;
               } else {
                  blobPixels[baseBlobIndex + 0] = pixels[baseImageIndex + 0];
                  blobPixels[baseBlobIndex + 1] = pixels[baseImageIndex + 1];
                  blobPixels[baseBlobIndex + 2] = pixels[baseImageIndex + 2];
               }
            }
         }

         MagickImage blobImage = new MagickImage();
         blobImage.constituteImage(blob.getBoundingWidth(),
                                   blob.getBoundingHeight(),
                                   "RGB",
                                   blobPixels);

         infos[count++] = new BubbleInfo(blob.getMinRow(), blob.getMinCol(),
                                         blob.getBoundingWidth(), blob.getBoundingHeight(),
                                         blobImage);
      }

      return infos;
   }

   /**
    * Color all the text bubbles.
    * It is assumed that |image| has been edged, and that it has
    *  only two colors, true black and true white.
    * White pixels are edges.
    */
   public static MagickImage fillBubbles(MagickImage image) throws Exception {
      List<Blob> bubbles = getBubbles(image);
      return colorBubbles(image, bubbles);
   }

   public static MagickImage colorBubbles(MagickImage image,
                                          List<Blob> bubbles) throws Exception {
      Dimension dimensions = image.getDimension();
      byte[] pixels = new byte[dimensions.width * dimensions.height * 3];

      image.dispatchImage(0, 0,
                          dimensions.width, dimensions.height,
                          "RGB",
                          pixels);

      // Fill the blobs.
      fillBlobs(pixels, bubbles, null);

      MagickImage newImage = new MagickImage();
      newImage.constituteImage(dimensions.width, dimensions.height,
                               "RGB",
                               pixels);

      return newImage;
   }

   /**
    * Modify |pixels| to fill in all the blobs as red.
    */
   private static void fillBlobs(byte[] pixels, List<Blob> blobs) {
      fillBlobs(pixels, blobs, new Color(255, 0, 0));
   }

   /**
    * If |color| is null, then pick a different colot every time.
    */
   private static void fillBlobs(byte[] pixels, List<Blob> blobs, Color color) {
      for (Blob blob : blobs) {
         Color activeColor = color != null ? color : ColorUtils.nextColor();

         for (Integer index : blob.getPoints()) {
            int pixelIndex = index.intValue() * 3;

            // Mark the blobs as red.
            pixels[pixelIndex + 0] = (byte)activeColor.getRed();
            pixels[pixelIndex + 1] = (byte)activeColor.getGreen();
            pixels[pixelIndex + 2] = (byte)activeColor.getBlue();
         }
      }
   }

   /**
    * Get all the blobs.
    */
   private static List<Blob> getRawBlobs(int width, byte[] pixels) {
      List<Blob> allBlobs = new ArrayList<Blob>();

      boolean[] visited = new boolean[pixels.length];
      Queue<Integer> toVisit = new LinkedList<Integer>();

      // All the offsets to check for blobs.
      // left, right, up, down
      int[] offsets = {-1, 1,
                       -1 * width, 1 * width};

      // Fill the visited pixels with edges.
      for (int i = 0; i < visited.length; i++) {
         if ((0xFF & pixels[i]) == 255) {
            visited[i] = true;
         }
      }

      // Depth-first w.r.t. blobs.
      for (int i = 0; i < visited.length; i++) {
         if (visited[i]) {
            continue;
         }

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

         if (!blob.isBorderBlob()) {
            allBlobs.add(blob);
         }
      }

      return allBlobs;
   }

   /**
    * Get the bubbles (callouts with text).
    */
   private static List<Blob> getBubbles(int width, byte[] edgedPixels,
                                        byte[] rawPixels) {
      assert(edgedPixels.length == rawPixels.length);

      List<Blob> allBlobs = getRawBlobs(width, edgedPixels);

      // Blobs for possible colors.
      List<Blob> characterBlobs = new ArrayList<Blob>();
      // Blobs for callout candidates.
      List<Blob> candidateBlobs = new ArrayList<Blob>();
      // Any blob that is black
      Set<Blob> blackBlobs = new HashSet<Blob>();

      int numPixels = edgedPixels.length;

      int minCharPixels = (int)(numPixels * MIN_CHARACTER_RATIO);
      int maxCharPixels = (int)(numPixels * MAX_CHARACTER_RATIO);
      int maxCharBoundingLengthPixels = (int)(numPixels * MAX_CHARACTER_BOUNDING_LENGTH_RATIO);

      int minCalloutPixels = (int)(numPixels * MIN_CALLOUT_RATIO);
      int maxCalloutPixels = (int)(numPixels * MAX_CALLOUT_RATIO);
      int maxCalloutBoundingPixels = (int)(numPixels * MAX_CALLOUT_BOUNDING_RATIO);

      // Get all blobs that are black.
      // We need this because callout candidates need to the surrounded by
      // a single black block.
      for (Blob blob : allBlobs) {
         int blackCount = 0;
         for (Integer point : blob.getPoints()) {
            if (rawPixels[point.intValue()] == 0) {
               blackCount++;
            }
         }

         if (blackCount >= blob.size() / 2) {
            blackBlobs.add(blob);
         }
      }

      for (Blob blob : allBlobs) {
         if (blob.size() >= minCharPixels &&
             blob.size() <= maxCharPixels &&
             blob.getBoundingWidth() <= maxCharBoundingLengthPixels &&
             blob.getBoundingHeight() <= maxCharBoundingLengthPixels &&
             // Character pixels must be black.
             blackBlobs.contains(blob)) {
            characterBlobs.add(blob);
         } else if (blob.size() >= minCalloutPixels &&
                    blob.size() <= maxCalloutPixels &&
                    blob.getBoundingSize() <= maxCalloutBoundingPixels &&
                    // Callout pixels must be white.
                    !blackBlobs.contains(blob)) {
            candidateBlobs.add(blob);
         }
      }

      // Resolve the parentage of the callout candidates.
      resolveParentage(candidateBlobs, allBlobs, edgedPixels, width);

      // Only keep candidates that have a black parent.
      int index = 0;
      while (index < candidateBlobs.size()) {
         if (candidateBlobs.get(index).getParent() == null ||
             !blackBlobs.contains(candidateBlobs.get(index).getParent())) {
            candidateBlobs.remove(index);
         } else {
            index++;
         }
      }

      // Find the parentage for character candidates and callout candidates.
      quickResolveParentage(characterBlobs, candidateBlobs);

      // Only keep candidates with character kids.
      index = 0;
      while (index < candidateBlobs.size()) {
         if (candidateBlobs.get(index).numChildren() == 0 ||
             candidateBlobs.get(index).numSurroundedChildren() == 0) {
            candidateBlobs.remove(index);
         } else {
            index++;
         }
      }

      return candidateBlobs;
   }

   /**
    * Get the bubbles (callouts with text).
    * (Old version for reference).
    */
   private static List<Blob> getBubblesOld(int width, byte[] pixels) {
      List<Blob> allBlobs = getRawBlobs(width, pixels);

      // Blobs for possible colors.
      List<Blob> characterBlobs = new ArrayList<Blob>();
      // Blobs for callout candidates.
      List<Blob> candidateBlobs = new ArrayList<Blob>();

      for (Blob blob : allBlobs) {
         // Callout candidate.
         if (blob.size() >= DEFAULT_MIN_CALLOUT_BLOB_SIZE &&
             blob.size() < DEFAULT_MAX_CALLOUT_BLOB_SIZE &&
             blob.density() >= DEFAULT_MIN_BLOB_DENSITY_1) {
            blob.geometryAdjust(0.10);
            candidateBlobs.add(blob);
         // Get character candidates.
         } else if (blob.size() >= DEFAULT_MIN_CHARACTER_BLOB_SIZE &&
             blob.size() <= DEFAULT_MAX_CHARACTER_BLOB_SIZE) {
            characterBlobs.add(blob);
         }
      }

      // Find children (character candidates) for candidate blobs.
      quickResolveParentage(characterBlobs, candidateBlobs);

      // Check the candidate blobs.
      // Callouts must contain characters.
      // The characters (child blobs) count as part of the parent for densite calculations.
      List<Blob> calloutBlobs = new ArrayList<Blob>();

      for (Blob candidate : candidateBlobs) {
         if (candidate.numChildren() == 0) {
            continue;
         }

         if (candidate.density(true) > 0.60) {
            int numSurroundedKids = candidate.numSurroundedChildren();
            if (numSurroundedKids > 0) {
               calloutBlobs.add(candidate);
            }
         }
      }

      return calloutBlobs;
   }

   /**
    * Clear all the parents and kids.
    */
   private static void clearParentage(List<Blob> possibleKids,
                                      List<Blob> possibleParents) {
      for (Blob kid : possibleKids) {
         kid.setParent(null);
      }

      for (Blob parent : possibleParents) {
         parent.clearChildren();
      }
   }

   /**
    * Resolve the parentage of the kid blobs.
    * This one is expensive, but will find the optimal parent.
    * To be a parent, a blob must completley surround a child.
    */
   private static void resolveParentage(List<Blob> kids,
                                        List<Blob> possibleParents,
                                        byte[] edgedPixels,
                                        int imageWidth) {
      for (Blob kidCandidate : kids) {
         int[][] outline = kidCandidate.approximateOutline();
         Blob parentCandidate = null;
         boolean done = false;

         for (int i = 0; i < outline.length; i++) {
            int[] offsets = new int[2];
            // Invert the offset so we are going away from the outline.
            offsets[0] = -1 * Blob.DIRECTIONAL_OFFSETS[i][0];
            offsets[1] = -1 * Blob.DIRECTIONAL_OFFSETS[i][1];

            // Move down a side.
            for (int base : outline[i]) {
               int index = MathUtils.indexOffset(base, offsets[0], offsets[1], imageWidth);

               // Move out from the outline.
               while (inBoundsAdjacent(base, index, imageWidth, edgedPixels.length)) {
                  if (edgedPixels[index] == 0) {
                     // Found another blob.
                     Blob blob = getBlobWithPixel(possibleParents, index);

                     if (blob != null) {
                        if (parentCandidate == null) {
                           parentCandidate = blob;
                           break;
                        } else if (blob.getId() != parentCandidate.getId()) {
                           // Found multiple surrounding blobs, this kid has no parents. :.(
                           done = true;
                           parentCandidate = null;
                           break;
                        } else {
                           break;
                        }
                     }
                  }

                  // Move index and base.
                  base = MathUtils.indexOffset(base, offsets[0], offsets[1], imageWidth);
                  index = MathUtils.indexOffset(index, offsets[0], offsets[1], imageWidth);
               }

               if (done) {
                  break;
               }
            }

            if (done) {
               break;
            }
         }

         if (parentCandidate != null) {
            // Adopt!
            parentCandidate.addChild(kidCandidate);
            kidCandidate.setParent(parentCandidate);
         }
      }
   }

   private static Blob getBlobWithPixel(List<Blob> blobs, int index) {
      for (Blob blob : blobs) {
         if (blob.contains(index)) {
            return blob;
         }
      }

      return null;
   }

   /**
    * Resolve the parentage of the kid blobs.
    * Uses contains() (really avgContainingDistance()).
    * Because of contains(), this is a quick approximation.
    * Use resolveParentage() for more accurate results (at a computational cost).
    */
   private static void quickResolveParentage(List<Blob> kids,
                                             List<Blob> possibleParents) {
      for (Blob kidCandidate : kids) {
         double minContainingDist = Integer.MAX_VALUE;
         Blob parent = null;

         // Get the closest containing blob.
         for (Blob parentCandidate : possibleParents) {
            double containingDist = parentCandidate.avgContainingDistance(kidCandidate);
            if (containingDist > 0 &&
                (parent == null || containingDist < minContainingDist)) {
               minContainingDist = containingDist;
               parent = parentCandidate;
            }
         }

         if (parent != null) {
            parent.addChild(kidCandidate);
            kidCandidate.setParent(parent);
         }
      }
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

   /**
    * A container for information about bubbles.
    */
   public static class BubbleInfo {
      public final int startRow;
      public final int startCol;
      public final int width;
      public final int height;
      public final MagickImage image;

      public BubbleInfo(int startRow, int startCol, int width, int height, MagickImage image) {
         this.startRow = startRow;
         this.startCol = startCol;
         this.width = width;
         this.height = height;
         this.image = image;
      }
   }
}
