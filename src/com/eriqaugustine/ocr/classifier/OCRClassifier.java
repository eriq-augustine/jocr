package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.image.WrapImage;

/**
 * A classifier specialized for ORCing characters.
 */
public interface OCRClassifier {
   public String classify(WrapImage image);
}
