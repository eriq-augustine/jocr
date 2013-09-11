package com.eriqaugustine.ocr.pdc;

/**
 * A DC where all co-linear dimensions are combined.
 * Eg. 12:00 and 6:00 are combined in one.
 */
public class HalfDCFeature extends DCFeature {
   public HalfDCFeature(double[] contributivity) {
      super(contributivity);
      assert(contributivity.length == PDC.PDC_DIRECTION_DELTAS.length / 2);
   }

   public boolean empty() {
      return false;
   }
}
