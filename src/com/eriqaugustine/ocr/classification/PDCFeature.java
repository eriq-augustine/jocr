package com.eriqaugustine.ocr.classification;

import com.eriqaugustine.ocr.image.PDC;

/**
 * A single PDC (DC) instance.
 */
public class PDCFeature {
   private double[] contributivity;

   /**
    * Make an empty PDCFeature (no directional influence).
    */
   public PDCFeature() {
      contributivity = new double[PDC.PDC_DIRECTION_DELTAS.length];
   }

   /**
    * Make a PDCFeature with real contents.
    */
   public PDCFeature(double[] contributivity) {
      this.contributivity = new double[contributivity.length];
      for (int i = 0; i < contributivity.length; i++) {
         this.contributivity[i] = contributivity[i];
      }
   }

   public boolean empty() {
      return contributivity == null;
   }

   public int length() {
      return contributivity.length;
   }

   /**
    * Will throw if out of bounds.
    */
   public double getValue(int index) {
      return contributivity[index];
   }

   /**
    * TODO(eriq): The directions cardinality should not be hardcoded.
    */
   public String toString() {
      String rtn = "[";

      for (double part : contributivity) {
         rtn += String.format("%5.3f, ", part);
      }

      return rtn.replaceFirst(", $", "]");
   }
}
