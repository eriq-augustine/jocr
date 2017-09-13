package com.eriqaugustine.ocr.plove;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.ListUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to extract the P-LOVE (Peripheral Local Outline Vector? ?) features from an image.
 * See papers in articles.
 * Note that this only uses a 3x3 window.
 */
public final class PLOVE {
   public static int SCALE_SIZE = 64;
   // public static int SCALE_SIZE = 28;

   public static final int WHITE_THRESHOLD = 100;
   public static final int MIN_BLOB_SIZE = 9;

   private static final int NUM_LAYERS = 3;

   private static final int NUM_SCANNING_DIRECTIONS = 8;

   private static final int DIRECTION_BACK_SLASH = 0; // \
   private static final int DIRECTION_VERTICAL = 1; // |
   private static final int DIRECTION_SLASH = 2; // /
   private static final int DIRECTION_HORIZONTAL = 3; // -

   /**
    * The Y deltas for the order to scan for connecting information.
    * Once a fg point is found, the scan stops.
    */
   private static final int[] CONNECTING_SCAN_ORDER_Y = {1, 0, 0, -1, 1, 1, -1, -1};

   /**
    * The X deltas for the order to scan for connecting information.
    * Once a fg point is found, the scan stops.
    */
   private static final int[] CONNECTING_SCAN_ORDER_X = {0, 1, -1, 0, 1, -1, 1, -1};

   /**
    * This is a static-only class, don't construct.
    */
   private PLOVE() {
   }

   /**
    * Get the total of number that will be returned by plove().
    * Note that four features are produced for every peripheral pioint.
    */
   public static int getNumberOfFeatures() {
      return
         NUM_SCANNING_DIRECTIONS * // Scaning directions.
         NUM_LAYERS * // Layers.
         SCALE_SIZE * // pixels per row/col.
         4; // Directional components per point.
   }

   /**
    * Run PLOVE on an image.
    * |image| must be already be binary.
    */
   public static double[] plove(WrapImage image) {
      double[] features = new double[getNumberOfFeatures()];

      // We may accidentally get empty images in our training set.
      // Durring real classification, empty images should get cut out earlier.
      if (image.isEmpty()) {
         for (int i = 0; i < features.length; i++) {
            features[i] = 0;
         }
         return features;
      }

      image = image.copy();
      image.scale(SCALE_SIZE, SCALE_SIZE);
      image.scrub(WHITE_THRESHOLD, MIN_BLOB_SIZE);
      boolean[] discretePixels = image.getDiscretePixels(WHITE_THRESHOLD);

      List<Integer> peripherals = ImageUtils.getPeripheralPoints(discretePixels, SCALE_SIZE,
                                                                 NUM_LAYERS, true);
      assert(peripherals.size() * 4 == getNumberOfFeatures());

      int[] connectingInfo = getConnectingInformation(discretePixels, SCALE_SIZE);

      // The directional comonents for each peripheral point.
      // We will reuse instead of re-allocate.
      double[] directionalComponents = new double[4];

      for (int i = 0; i < peripherals.size(); i++) {
         if (peripherals.get(i) == null) {
            for (int j = 0; j < 4; j++) {
               // Zero for nothing.
               features[(i * 4) + j] = 0;
            }
         } else {
            ploveDirectionalComponents(directionalComponents,
                                       discretePixels, connectingInfo,
                                       SCALE_SIZE, peripherals.get(i).intValue());

            for (int j = 0; j < 4; j++) {
               features[(i * 4) + j] = directionalComponents[j];
            }
         }
      }

      return features;
   }

   public static double[][] plove(WrapImage[] images) {
      double[][] rtn = new double[images.length][];
      for (int i = 0; i < images.length; i++) {
         rtn[i] = plove(images[i]);
      }
      return rtn;
   }

   /**
    * Get the connecting information for an image.
    * The authors call this "connecting information", but the name and the process are
    * not very descriptive.
    * For each fg point, we assign it a direction.
    * This is supposed to be that points "connecting information".
    * To get the connecting information, we open a 3x3 window around the point,
    * then look in a very specific order for other fg points in the window.
    * The first fg point to appear claims the direction for that point.
    * The order was not specified in the paper.
    * The only other P-LOVE implementation I found has the following order
    * (if the point is centered at (0,0) on a cartesian plane (x,y):
    *  (0,-1), (1,0), (-1,0), (0,1), (1,-1), (-1,-1), (1,1), (-1,1).
    * Points that do not have connecting information will get a -1.
    */
   private static int[] getConnectingInformation(
         boolean[] discretePixels,
         int imageSideLength) {
      int[] rtn = new int[discretePixels.length];

      int dx;
      int dy;
      int direction;

      for (int y = 0; y < imageSideLength; y++) {
         for (int x = 0; x < imageSideLength; x++) {
            // Start the point as having no connecting information.
            rtn[MathUtils.rowColToIndex(y, x, imageSideLength)] = -1;

            // Skip if not a fg point.
            if (!discretePixels[MathUtils.rowColToIndex(y, x, imageSideLength)]) {
               continue;
            }

            // Scan for another fg point in the specified order.
            for (int i = 0; i < CONNECTING_SCAN_ORDER_Y.length; i++) {
               dy = CONNECTING_SCAN_ORDER_Y[i];
               dx = CONNECTING_SCAN_ORDER_X[i];

               // If we are inbounds and on a fg point.
               if (MathUtils.inBounds(y + dy, x + dx, imageSideLength, imageSideLength * imageSideLength)
                     && discretePixels[MathUtils.rowColToIndex(y + dy, x + dx, imageSideLength)]) {

                  // Find what direction this fg point lies on.
                  if (dy == 0) {
                     direction = DIRECTION_HORIZONTAL;
                  } else if (dx == 0) {
                     direction = DIRECTION_VERTICAL;
                  } else if (dx + dy == 0) {
                     direction = DIRECTION_SLASH;
                  } else {
                     direction = DIRECTION_BACK_SLASH;
                  }

                  rtn[MathUtils.rowColToIndex(y, x, imageSideLength)] = direction;
                  break;
               }
            }
         }
      }

      return rtn;
   }

   /**
    * Get the directional components for a single peripheral pixel.
    * The results will be filled into |directionalComponents|.
    * |discretePixels| should represent a |imageSideLength| x |imageSideLength| image.
    */
   private static void ploveDirectionalComponents(
         double[] directionalComponents,
         boolean[] discretePixels,
         int[] connectingInfo,
         int imageSideLength,
         int point) {
      assert(directionalComponents.length == 4);

      // Init the directional components.
      for (int i = 0; i < 4; i++) {
         directionalComponents[i] = 0;
      }

      int y = MathUtils.indexToRow(point, imageSideLength);
      int x = MathUtils.indexToCol(point, imageSideLength);

      // The number of foregroud pixels discovered in the window.
      int count = 0;

      int newY;
      int newX;
      int direction;

      for (int dy = -1; dy != 2; dy++) {
         for (int dx = -1; dx != 2; dx++) {
            newY = y + dy;
            newX = x + dx;

            // Skip out-of-bounds and pixels that have no connecting information.
            // Note that we are including the current point.
            if (!MathUtils.inBounds(newY, newX, imageSideLength, imageSideLength * imageSideLength)
                  || connectingInfo[MathUtils.rowColToIndex(newY, newX, imageSideLength)] == -1) {
               continue;
            }

            // Add one to the directional component for every point in the window that has
            //  a connecting information that matches that direction.
            directionalComponents[connectingInfo[MathUtils.rowColToIndex(newY, newX, imageSideLength)]]++;

            // Count one for every fg point in the window that has connecting information.
            count++;
         }
      }

      if (count == 0) {
         return;
      }

      // Normalize the directions' contributions by the number of actual fg points seen.
      for (int i = 0; i < 4; i++) {
         directionalComponents[i] /= (double)count;
      }
   }
}
