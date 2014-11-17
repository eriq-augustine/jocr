package com.eriqaugustine.ocr.classifier;

/**
 * Translate and replace the text in an image.
 */
public class BasePostClassifier implements PostClassifier {
   /**
    * @inheritDoc
    */
   public String postClassify(String input) {
      // System.err.println("Pre : " + input);

      // "ハ‥" -> "‼"
      // Note that the second character is a double ellipsis (some fonts show it strangely).
      input = input.replaceAll("ハ‥", "‼");

      // Replace double ellipsis with standard ellipsis.
      input = input.replaceAll("‥", "…");

      // Quote
      input = input.replaceAll("ワ\\s*\\.", "?");
      input = input.replaceAll("つ\\s*\\.", "?");
      input = input.replaceAll("っ\\s*\\.", "?");

      // Bang
      input = input.replaceAll("\\|\\s*\\.", "!");
      input = input.replaceAll("/\\s*\\.", "!");

      // Ellipsis
      input = input.replaceAll("\\.\\s*\\.\\s*\\.", "…");

      // "一ん" -> "え"
      input = input.replaceAll("-ん", "え");

      // "‼" -> "!"
      // For consistency, lets just use single bangs.
      input = input.replaceAll("‼", "!");
      input = input.replaceAll("!(\\s*!)*", "!");

      // Change any remaining periods to japanese periods.
      input = input.replaceAll("\\.", "。");

      // Change any remaining bars or dashes with an elongation.
      input = input.replaceAll("[|-]", "ー");

      // System.err.println("Post: " + input);

      return input;
   }
}
