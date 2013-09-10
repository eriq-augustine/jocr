package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.classification.PDCFeature;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import magick.MagickImage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to handle the functionality for PDC (Peripheral Direction Contributivities).
 * See papers in articels.
 */
public final class PDC {
   //TEST
   //private static final int SCALE_SIZE = 64;
   private static final int SCALE_SIZE = 32;

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
    * TODO(eriq): Group vectors into bins.
    * TODO(eriq): Diagnal scans.
    */
   public static PDCFeature[] pdc(boolean[] image, int imageWidth) {
      // TODO(eriq): Be more flexible about sizes
      assert(imageWidth == SCALE_SIZE && image.length / imageWidth == SCALE_SIZE);

      PDCFeature[] features = new PDCFeature[image.length];
      for (int i = 0; i < image.length; i++) {
         features[i] = new PDCFeature();
      }

      Set<Integer> peripherals = new HashSet<Integer>();
      // Layers
      for (int i = 0; i < 3; i++) {
         // Horizontal LTR
         peripherals.addAll(scan(image, imageWidth, i, true, true));

         // Horizontal RTL
         peripherals.addAll(scan(image, imageWidth, i, true, false));

         // Vertial Down
         peripherals.addAll(scan(image, imageWidth, i, false, true));

         // Vertical Up
         peripherals.addAll(scan(image, imageWidth, i, false, false));
      }

      for (Integer peripheral : peripherals) {
         double[] contributivity = dc(image, imageWidth, peripheral.intValue());
         features[peripheral.intValue()] = new PDCFeature(contributivity);
      }

      return features;
   }

   public static PDCFeature[] pdc(MagickImage image) throws Exception {
      boolean[] discretePixels =
            Filters.discretizePixels(ImageUtils.scaleImage(image, SCALE_SIZE, SCALE_SIZE), 200);
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
    * |layerNumber| is the number of solid bodies to pass through before the final
    * peripheral edge. 0 means that it will pass through no bodies.
    * TODO(eriq): Abstract to allow vertical and maybe diagnal scans.
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

      // TODO(eriq): I hate these damn horizontal tricks.
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

      List<Integer> peripherals = new ArrayList<Integer>();

      for (int outer = outerStart; outer != outerEnd; outer += outerDelta) {
         int currentLayerCount = 0;
         boolean inBody = false;

         for (int inner = innerStart; inner != innerEnd; inner += innerDelta) {
            int index = horizontal ? MathUtils.rowColToIndex(outer, inner, imageWidth) :
                                     MathUtils.rowColToIndex(inner, outer, imageWidth);

            if (image[index] && !inBody) {
               inBody = true;

               if (currentLayerCount == layerNumber) {
                  peripherals.add(new Integer(index));
                  break;
               }
            } else if (!image[index] && inBody) {
               inBody = false;
               currentLayerCount++;
            }
         }
      }

      return peripherals;
   }
}
