package com.eriqaugustine.ocr.pdc;

/**
 * A DC featuring all eight dimensions.
 */
public class FullDCFeature extends DCFeature {
   public FullDCFeature(double[] contributivity) {
      super(contributivity);
      assert(contributivity.length == PDC.PDC_DIRECTION_DELTAS.length);
   }

   public boolean empty() {
      return false;
   }
}
