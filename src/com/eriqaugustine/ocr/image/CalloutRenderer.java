package com.eriqaugustine.ocr.image;

import java.awt.Rectangle;

/**
 * Render some text into an image.
 * Presumably overriding a callout.
 * TODO(eriq): We will someday need to not rely on the render area being rectangular.
 */
public abstract class CalloutRenderer {
   /**
    * Render some text into an image of the given dimensions.
    */
   public WrapImage renderText(String text, int width, int height) {
      assert(text != null);
      assert(width > 0);
      assert(height > 0);

      return renderTextImpl(text, width, height);
   }

   protected abstract WrapImage renderTextImpl(String text, int width, int height);

   /**
    * Render some text into a copy of |baseImage|.
    */
   public WrapImage renderInto(WrapImage baseImage, String text, Rectangle renderArea) {
      assert(baseImage != null);
      assert(!baseImage.isEmpty());
      assert(text != null);

      Rectangle baseBounds = new Rectangle(baseImage.width(), baseImage.height());

      assert(renderArea != null);
      assert(baseBounds.contains(renderArea));

      return renderIntoImpl(baseImage, text, renderArea);
   }

   protected abstract WrapImage renderIntoImpl(WrapImage baseImage, String text, Rectangle renderArea);
}
