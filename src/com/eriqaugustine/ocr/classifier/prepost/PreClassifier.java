package com.eriqaugustine.ocr.classifier.prepost;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.image.ImageText;

import java.util.List;

/**
 * Try to classify a collection of images before they are run through the formal classifier.
 * Note that all the images (in order) are made available so that
 * certian combinations (like a vertical bar and period) can be combined.
 */
public interface PreClassifier {
   public List<ImageText> preClassify(List<WrapImage> characterImages);
}
