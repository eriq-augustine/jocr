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
   public static final int SCALE_SIZE = 64;

   private static final int NUM_LAYERS = 3;

   private static final int NUM_SCANNING_DIRECTIONS = 8;

   private static final int DIRECTION_BACK_SLASH = 0; // \
   private static final int DIRECTION_VERTICAL = 1; // |
   private static final int DIRECTION_SLASH = 2; // /
   private static final int DIRECTION_HORIZONTAL = 3; // -

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
      image = image.copy();
      image.scale(SCALE_SIZE, SCALE_SIZE);
      boolean[] discretePixels = image.getDiscretePixels();


      List<Integer> peripherals = ImageUtils.getPeripheralPoints(discretePixels, SCALE_SIZE,
                                                                 NUM_LAYERS, true);

      assert(peripherals.size() * 4 == getNumberOfFeatures());

      double[] features = new double[getNumberOfFeatures()];

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
            ploveDirectionalComponents(directionalComponents, discretePixels,
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
    * Get the directional components for a single peripheral pixel.
    * The results will be filled into |directionalComponents|.
    * |discretePixels| should represent a |imageSideLength| x |imageSideLength| image.
    */
   private static void ploveDirectionalComponents(
         double[] directionalComponents,
         boolean[] discretePixels,
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

            // Skip out-of-bounds, the actual peripheral point we are looking at, and bg pixels.
            if ((dy == 0 && dx == 0)
                  || !MathUtils.inBounds(newY, newX, imageSideLength, imageSideLength)
                  ) { // || !discretePixels[MathUtils.rowColToIndex(newY, newX, imageSideLength)]) {
               continue;
            }

            if (discretePixels[MathUtils.rowColToIndex(newY, newX, imageSideLength)]) {
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

               // Add one for every fg pixel in that direction.
               directionalComponents[direction]++;
            }

            count++;
         }
      }

      if (count == 0) {
         return;
      }

      // TEST
      System.err.println("^^^");
      System.err.println("[" + directionalComponents[0] +
                         ", " + directionalComponents[1] +
                         ", " + directionalComponents[2] +
                         ", " + directionalComponents[3] + "]");

      // Normalize the directions' contributions by the number of actual fg points seen.
      for (int i = 0; i < 4; i++) {
         directionalComponents[i] /= (double)count;
      }

      // TEST
      System.err.println(directionalComponents);
      System.err.println("[" + directionalComponents[0] +
                         ", " + directionalComponents[1] +
                         ", " + directionalComponents[2] +
                         ", " + directionalComponents[3] + "]");
      System.err.println("vvv");
   }
}
