package com.eriqaugustine.ocr.pdc;

import magick.MagickImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all the information necessary after a PDC analysis of an image.
 */
public class PDCInfo {
   private final MagickImage baseImage;
   private final MagickImage scaleImage;

   private final int numLayers;

   private final int[][] lengths;
   private final int[] peripherals;

   private DCFeature[] fullPDCs;
   private DCFeature[] halfPDCs;

   /**
    * Make a new PDCInfo.
    * This should be fast, since all DC calculations are lazy.
    * A null length means that there was no peripheral point.
    */
   public PDCInfo(MagickImage baseImage, MagickImage scaleImage,
                  int numLayers,
                  int[][] lengths, int[] peripherals) {
      this.baseImage = baseImage;
      this.scaleImage = scaleImage;
      this.numLayers = numLayers;
      this.lengths = lengths;
      this.peripherals = peripherals;

      fullPDCs = null;
      halfPDCs = null;
   }

   public int numPoints() {
      return lengths.length;
   }

   /**
    * Get every dimension of each DC in a single List.
    * Empty DCs count as 8 zeros.
    */
   public double[] fullPDCDimensions() {
      if (fullPDCs == null) {
         calcFullPDCs();
      }

      double[] rtn = new double[lengths.length * PDC.PDC_DIRECTION_DELTAS.length];

      int count = 0;
      for (DCFeature dc : fullPDCs) {
         if (dc.empty()) {
            for (int i = 0; i < PDC.PDC_DIRECTION_DELTAS.length; i++) {
               rtn[count++] = 0;
            }
         } else {
            for (int i = 0; i < dc.length(); i++) {
               rtn[count++] = dc.getValue(i);
            }
         }
      }

      assert(count == lengths.length * PDC.PDC_DIRECTION_DELTAS.length);

      return rtn;
   }

   public List<DCFeature> fullPDCs(boolean getEmpties) {
      if (fullPDCs == null) {
         calcFullPDCs();
      }

      List<DCFeature> rtn = new ArrayList<DCFeature>(fullPDCs.length);
      for (DCFeature dc : fullPDCs) {
         if (getEmpties) {
            rtn.add(dc);
         } else if (!dc.empty()) {
            rtn.add(dc);
         }
      }

      return rtn;
   }

   private void calcFullPDCs() {
      fullPDCs = new DCFeature[lengths.length];

      for (int i = 0; i < lengths.length; i++) {
         if (lengths[i] == null) {
            fullPDCs[i] = new EmptyDCFeature();
            continue;
         }

         double[] contributivity = new double[lengths[i].length];
         double normalizationFactor = 0;

         for (int directionIndex = 0; directionIndex < lengths[i].length; directionIndex++) {
            contributivity[directionIndex] = lengths[i][directionIndex];
            normalizationFactor += Math.pow(lengths[i][directionIndex], 2);
         }
         normalizationFactor = Math.sqrt(normalizationFactor);

         for (int directionIndex = 0; directionIndex < lengths[i].length; directionIndex++) {
            contributivity[directionIndex] /= normalizationFactor;
         }

         fullPDCs[i] = new FullDCFeature(contributivity);
      }
   }

   public double[] halfPDCDimensions() {
      if (halfPDCs == null) {
         calcHalfPDCs();
      }

      double[] rtn = new double[lengths.length * PDC.PDC_DIRECTION_DELTAS.length / 2];

      int count = 0;
      for (DCFeature dc : halfPDCs) {
         if (dc.empty()) {
            for (int i = 0; i < PDC.PDC_DIRECTION_DELTAS.length / 2; i++) {
               rtn[count++] = 0;
            }
         } else {
            for (int i = 0; i < dc.length(); i++) {
               rtn[count++] = dc.getValue(i);
            }
         }
      }

      assert(count == lengths.length * PDC.PDC_DIRECTION_DELTAS.length / 2);

      return rtn;
   }

   /**
    * Get the halfed PDCs.
    * Both sides of each component are copmbined
    * (both verticals combined into one value etc.).
    */
   public List<DCFeature> halfPDCs(boolean getEmpties) {
      if (halfPDCs == null) {
         calcHalfPDCs();
      }

      List<DCFeature> rtn = new ArrayList<DCFeature>(halfPDCs.length);
      for (DCFeature dc : halfPDCs) {
         if (getEmpties) {
            rtn.add(dc);
         } else if (!dc.empty()) {
            rtn.add(dc);
         }
      }

      return rtn;
   }

   private void calcHalfPDCs() {
      halfPDCs = new DCFeature[lengths.length];

      for (int i = 0; i < lengths.length; i++) {
         if (lengths[i] == null) {
            halfPDCs[i] = new EmptyDCFeature();
            continue;
         }

         assert(lengths[i].length % 2 == 0);

         double[] contributivity = new double[lengths[i].length / 2];
         double normalizationFactor = 0;

         for (int directionIndex = 0; directionIndex < lengths[i].length / 2; directionIndex++) {
            int value = lengths[i][directionIndex] +
                        lengths[i][directionIndex + lengths[i].length / 2];

            contributivity[directionIndex] = value;
            normalizationFactor += Math.pow(value, 2);
         }
         normalizationFactor = Math.sqrt(normalizationFactor);

         for (int contributivityIndex = 0; contributivityIndex < contributivity.length;
              contributivityIndex++) {
            contributivity[contributivityIndex] /= normalizationFactor;
         }

         halfPDCs[i] = new HalfDCFeature(contributivity);
      }
   }
}
