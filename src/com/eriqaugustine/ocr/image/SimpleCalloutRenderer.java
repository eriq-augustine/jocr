package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.ImageUtils;

import java.awt.Rectangle;

/**
 * A VERY simple renderer.
 */
public class SimpleCalloutRenderer extends CalloutRenderer {
   /**
    * @inheritDoc
    */
   protected WrapImage renderTextImpl(String text, int width, int height) {
      return WrapImage.getStringImage(text, false, width, height);
   }

   /**
    * @inheritDoc
    */
   protected WrapImage renderIntoImpl(WrapImage baseImage, String text, Rectangle renderArea) {
      WrapImage newText = renderText(text, renderArea.width, renderArea.height);
      return ImageUtils.overlayImage(baseImage, newText, renderArea.y, renderArea.x);
   }
}

