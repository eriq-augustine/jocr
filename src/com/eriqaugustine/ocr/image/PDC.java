package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.classification.PDCFeature;
import com.eriqaugustine.ocr.utils.MathUtils;

import magick.MagickImage;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle the functionality for PDC (Peripheral Direction Contributivities).
 * See papers in articels.
 */
public final class PDC {
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
    * Run PDC on an image.
    * |image| must be already be binary.
    * TODO(eriq): Normalize image size.
    * TODO(eriq): Multiple layers.
    * TODO(eriq): Group vectors into bins.
    * TODO(eriq): Multiple scanning directions.
    */
   public static PDCFeature[] pdc(boolean[] image, int imageWidth) {
      PDCFeature[] features = new PDCFeature[image.length];
      for (int i = 0; i < image.length; i++) {
         features[i] = new PDCFeature();
      }

      List<Integer> peripherals = horizontalScan(image, imageWidth, 0, 1);

      for (Integer peripheral : peripherals) {
         double[] contributivity = dc(image, imageWidth, peripheral.intValue());
         features[peripheral.intValue()] = new PDCFeature(contributivity);
      }

      return features;
   }

   public static PDCFeature[] pdc(MagickImage image) throws Exception {
      boolean[] discretePixels = Filters.discretizePixels(image, 200);
      return pdc(discretePixels, image.getDimension().width);
   }

   public static List<PDCFeature[]> pdc(MagickImage[] images) throws Exception {
      List<PDCFeature[]> rtn = new ArrayList<PDCFeature[]>(images.length);
      for (MagickImage image : images) {
         rtn.add(pdc(image));
      }
      return rtn;
   }

   /**
    * Get the DC (Direction Contributivity) of a point.
    */
   private static double[] dc(boolean[] image, int imageWidth, int point) {
      double[] contributivity = new double[PDC_DIRECTION_DELTAS.length];
      double normalizationFactor = 0;

      int baseRow = MathUtils.indexToRow(point, imageWidth);
      int baseCol = MathUtils.indexToCol(point, imageWidth);

      // First, get all the contributivity lengths.
      for (int delta = 0; delta < PDC_DIRECTION_DELTAS.length; delta++) {
         int length = 0;

         int row = baseRow + PDC_DIRECTION_DELTAS[delta][0];
         int col = baseCol + PDC_DIRECTION_DELTAS[delta][1];
         while (MathUtils.inBounds(row, col, imageWidth, image.length)) {
            length++;

            row += PDC_DIRECTION_DELTAS[delta][0];
            col += PDC_DIRECTION_DELTAS[delta][1];
         }

         contributivity[delta] = length;
         normalizationFactor += length * length;
      }

      // Normalize the vector.
      normalizationFactor = Math.sqrt(normalizationFactor);
      for (int i = 0; i < contributivity.length; i++) {
         // TODO(eriq): Another way to handle this?
         if (normalizationFactor == 0) {
            contributivity[i] = 0;
         } else {
            contributivity[i] = contributivity[i] / normalizationFactor;
         }
      }

      return contributivity;
   }

   /**
    * Scan a direction and get all of the peripheral (edge) points.
    * TODO(eriq): Allow for layer selection.
    * TODO(eriq): Abstract to allow vertical and maybe diagnal scans.
    */
   private static List<Integer> horizontalScan(boolean[] image, int imageWidth,
                                               int startCol, int colDelta) {
      List<Integer> peripherals = new ArrayList<Integer>();

      for (int row = 0; row < image.length / imageWidth; row++) {
         for (int col = startCol; col >= 0 && col < imageWidth; col += colDelta) {
            int index = MathUtils.rowColToIndex(row, col, imageWidth);

            if (image[index]) {
               peripherals.add(new Integer(index));
               break;
            }
         }
      }

      return peripherals;
   }
}
