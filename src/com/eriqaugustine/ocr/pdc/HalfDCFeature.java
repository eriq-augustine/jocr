package com.eriqaugustine.ocr.pdc;

import com.eriqaugustine.ocr.pdc.PDC;

public class HalfDCFeature extends DCFeature {
   public HalfDCFeature(double[] contributivity) {
      super(contributivity);
      assert(contributivity.length == PDC.PDC_DIRECTION_DELTAS.length / 2);
   }

   public boolean empty() {
      return false;
   }
}
