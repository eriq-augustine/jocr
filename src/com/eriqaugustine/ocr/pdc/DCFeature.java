package com.eriqaugustine.ocr.pdc;

import com.eriqaugustine.ocr.utils.MathUtils;

/**
 * A single DC (Directional Contributivity) feature instance.
 */
public abstract class DCFeature {
   private double[] contributivity;

   /**
    * Make an empty DCFeature (no directional influence).
    */
   public DCFeature() {
      contributivity = new double[0];
   }

   /**
    * Make a DCFeature with real contents.
    */
   public DCFeature(double[] contributivity) {
      this.contributivity = new double[contributivity.length];
      for (int i = 0; i < contributivity.length; i++) {
         this.contributivity[i] = contributivity[i];
      }
   }

   public abstract boolean empty();

   public int length() {
      return contributivity.length;
   }

   /**
    * Will throw if out of bounds.
    */
   public double getValue(int index) {
      assert(index >= 0 && index < contributivity.length);

      return contributivity[index];
   }

   public String toString() {
      if (empty()) {
         return "[empty]";
      }

      String rtn = "[";
      for (double part : contributivity) {
         rtn += String.format("%5.3f, ", part);
      }
      return rtn.replaceFirst(", $", "]");
   }
}
