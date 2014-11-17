package com.eriqaugustine.ocr.image;

import static com.eriqaugustine.ocr.image.WrapImage.Pixel;
import com.eriqaugustine.ocr.math.BinaryConfusionMatrix;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import java.awt.Color;
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
 * This exact method does not currently have a name.
 * This was created before the formal API, so it is still a bit messy.
 */
public class BubbleDetection extends BubbleDetector {
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
   public WrapImage bubbleFillTest(String imageFile,
                                          FileUtils.BubbleTrainingSet trainingData,
                                          BinaryConfusionMatrix matrix) {
      String baseImageName = new File(imageFile).getName();

      WrapImage image = WrapImage.getImageFromFile(imageFile);

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
    * @inheritDoc
    */
   public List<Blob> getBubbles(WrapImage image) {
      image = image.copy();
      image.blur(3, 1);

      boolean[] rawPixels = image.getDiscretePixels(200);

      image.edge(3);
      boolean[] edgedPixels = image.getDiscretePixels();

      List<Blob> blobs = getBubbles(image.width(), edgedPixels, rawPixels);

      return blobs;
   }

   /**
    * @inheritDoc
    */
   public BubbleInfo[] extractBubblesWithInfo(WrapImage image) {
      List<Blob> bubbles = getBubbles(image);

      Pixel[] pixels = image.getPixels();

      BubbleInfo[] infos = new BubbleInfo[bubbles.size()];

      int count = 0;
      for (Blob blob : bubbles) {
         Pixel[] blobPixels = new Pixel[blob.getBoundingSize()];
         Map<Integer, int[]> bounds = blob.getBoundaries();

         int width = blob.getBoundingWidth();

         for (int row = blob.getMinRow(); row <= blob.getMaxRow(); row++) {
            for (int col = blob.getMinCol(); col <= blob.getMaxCol(); col++) {
               int baseImageIndex = row * image.width() + col;
               int baseBlobIndex = (row - blob.getMinRow()) * width + (col - blob.getMinCol());

               if (col < bounds.get(row)[0] || col > bounds.get(row)[1]) {
                  blobPixels[baseBlobIndex] = new Pixel();
               } else {
                  blobPixels[baseBlobIndex] = new Pixel(pixels[baseImageIndex]);
               }
            }
         }

         WrapImage blobImage = WrapImage.getImageFromPixels(blobPixels,
                                                            blob.getBoundingWidth(),
                                                            blob.getBoundingHeight());

         infos[count++] = new BubbleInfo(blob.getMinRow(), blob.getMinCol(),
                                         blob.getBoundingWidth(), blob.getBoundingHeight(),
                                         blobImage, blob);
      }

      return infos;
   }

   /**
    * Get all the blobs.
    */
   private List<Blob> getRawBlobs(int width, boolean[] pixels) {
      List<Blob> allBlobs = new ArrayList<Blob>();

      boolean[] visited = new boolean[pixels.length];
      Queue<Integer> toVisit = new LinkedList<Integer>();

      // All the offsets to check for blobs.
      // left, right, up, down
      int[] offsets = {-1, 1,
                       -1 * width, 1 * width};

      // Fill the visited pixels with edges.
      for (int i = 0; i < visited.length; i++) {
         if (!pixels[i]) {
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
   private List<Blob> getBubbles(int width, boolean[] edgedPixels, boolean[] rawPixels) {
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
            if (rawPixels[point.intValue()]) {
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
    * Resolve the parentage of the kid blobs.
    * This one is expensive, but will find the optimal parent.
    * To be a parent, a blob must completley surround a child.
    */
   private void resolveParentage(List<Blob> kids,
                                 List<Blob> possibleParents,
                                 boolean[] edgedPixels,
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
                  if (edgedPixels[index]) {
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

   private Blob getBlobWithPixel(List<Blob> blobs, int index) {
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
   private void quickResolveParentage(List<Blob> kids,
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
   private boolean inBoundsAdjacent(int base, int index,
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
