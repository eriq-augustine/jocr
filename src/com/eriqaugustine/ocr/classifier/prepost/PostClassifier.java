package com.eriqaugustine.ocr.classifier.prepost;

/**
  * Apply some heuristics after the text has been classified.
  * This is a great place to cleanup very common misclassifications.
  * This should not do super serious work like spelling correction.
  * One common thing to to is combine characters like "ワ" and "." into "?".
  * Note that we work on a String rather than a List<ImageText> because it is easier to
  * use things like regular expressions.
  */
public interface PostClassifier {
   public String postClassify(String input);
}
