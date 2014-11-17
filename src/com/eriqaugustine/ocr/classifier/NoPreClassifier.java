package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.image.ImageText;

import java.util.ArrayList;
import java.util.List;

/**
 * Do nothing.
 */
public class NoPreClassifier {
   public List<ImageText> preClassify(List<WrapImage> characterImages) {
      List<ImageText> rtn = new ArrayList<ImageText>();

      for (WrapImage image : characterImages) {
         rtn.add(new ImageText(image));
      }

      return rtn;
   }
}
