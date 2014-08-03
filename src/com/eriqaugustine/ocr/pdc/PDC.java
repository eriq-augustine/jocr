package com.eriqaugustine.ocr.pdc;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.ListUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to handle the functionality for PDC (Peripheral Direction Contributivities).
 * See papers in articels.
 */
public final class PDC {
   public static final int SCALE_SIZE = 64;

   private static final int NUM_LAYERS = 3;

   private static final int NUM_CARDINAL_SCAN_DIRECTIONS = 4;
   private static final int NUM_DIAGONAL_SCAN_DIRECTIONS = 4;

   /**
    * The deltas [row, col] for the different directions available to PDC.
    * Starts at 12 and moves clockwise by 1:30.
    */
   public static final int[][] PDC_DIRECTION_DELTAS = new int[][]{
      new int[]{-1, 0},
      new int[]{-1, 1},
      new int[]{0, 1},
      new int[]{1, 1},
      new int[]{0, 1},
      new int[]{1, -1},
      new int[]{0, -1},
      new int[]{-1, -1}
   };

   /**
    * This is a static-only class, don't construct.
    */
   private PDC() {
   }

   /**
    * Get the number of DCs that this will produce.
    * This is the number of base DCs (so before any grouping is done).
    * This will be equal to PDCInfo.numPoints().
    */
   public static int getNumDCs() {
      return
         // Cardinals
         SCALE_SIZE * NUM_CARDINAL_SCAN_DIRECTIONS * NUM_LAYERS +
         // Diagonals (if scale size is odd, will not be the same number as cardinals.
         NUM_DIAGONAL_SCAN_DIRECTIONS * (int)(Math.ceil(SCALE_SIZE / 2.0) * 2) * NUM_LAYERS;
   }

   /**
    * Run PDC on an image.
    * |image| must be already be binary.
    */
   public static PDCInfo pdc(WrapImage image) {
      image = image.copy();
      image.scale(SCALE_SIZE, SCALE_SIZE);
      boolean[] discretePixels = image.getDiscretePixels();


      List<Integer> peripherals = ImageUtils.getPeripheralPoints(discretePixels, SCALE_SIZE,
                                                                 NUM_LAYERS, true);
      int[][] lengths = new int[peripherals.size()][];

      for (int i = 0; i < peripherals.size(); i++) {
         if (peripherals.get(i) == null) {
            lengths[i] = null;
         } else {
            lengths[i] = dcLengths(discretePixels, SCALE_SIZE, peripherals.get(i).intValue());
         }
      }

      return new PDCInfo(image, NUM_LAYERS,
                         lengths, peripherals);
   }

   public static PDCInfo[] pdc(WrapImage[] images) {
      PDCInfo[] rtn = new PDCInfo[images.length];
      for (int i = 0; i < images.length; i++) {
         rtn[i] = pdc(images[i]);
      }
      return rtn;
   }

   /**
    * Get the lengths that are the core components in the DC.
    */
   private static int[] dcLengths(boolean[] image, int imageWidth, int point) {
      int[] lengths = new int[PDC_DIRECTION_DELTAS.length];

      int baseRow = MathUtils.indexToRow(point, imageWidth);
      int baseCol = MathUtils.indexToCol(point, imageWidth);

      for (int delta = 0; delta < PDC_DIRECTION_DELTAS.length; delta++) {
         int length = 0;

         int row = baseRow + PDC_DIRECTION_DELTAS[delta][0];
         int col = baseCol + PDC_DIRECTION_DELTAS[delta][1];
         while (MathUtils.inBounds(row, col, imageWidth, image.length)) {
            length++;

            row += PDC_DIRECTION_DELTAS[delta][0];
            col += PDC_DIRECTION_DELTAS[delta][1];
         }

         lengths[delta] = length;
      }

      return lengths;
   }
}
