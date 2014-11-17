package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.classifier.BasePreClassifier;
import com.eriqaugustine.ocr.classifier.BasePostClassifier;
import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.PreClassifier;
import com.eriqaugustine.ocr.classifier.PostClassifier;
import com.eriqaugustine.ocr.classifier.RemoteClassifier;
import com.eriqaugustine.ocr.translate.Translator;
import com.eriqaugustine.ocr.utils.ImageUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Translate and replace the text in an image.
 * This class is mainly just a collector for all the pieces in the pipeline.
 */
public class ImageTranslator {
   private static Logger logger = LogManager.getLogger(ImageTranslator.class.getName());

   private BubbleDetector bubbleDetector;
   private TextExtractor textExtractor;
   private PreClassifier preClassy;
   private PostClassifier postClassy;
   private OCRClassifier classy;
   private Translator translator;
   private CalloutRenderer calloutRenderer;

   public ImageTranslator() throws Exception {
      this(new BubbleDetection(), new TextExtraction(),
           new BasePreClassifier(), new BasePostClassifier(),
           new RemoteClassifier(), new Translator("ja", "en"),
           new SimpleCalloutRenderer());
   }

   public ImageTranslator(OCRClassifier classy) throws Exception {
      this(new BubbleDetection(), new TextExtraction(),
           new BasePreClassifier(), new BasePostClassifier(),
           classy, new Translator("ja", "en"),
           new SimpleCalloutRenderer());
   }

   public ImageTranslator(BubbleDetector bubbleDetector, TextExtractor textExtractor,
                          PreClassifier preClassy, PostClassifier postClassy,
                          OCRClassifier classy, Translator translator,
                          CalloutRenderer calloutRenderer) throws Exception {
      this.bubbleDetector = bubbleDetector;
      this.textExtractor = textExtractor;
      this.preClassy = preClassy;
      this.postClassy = postClassy;
      this.classy = classy;
      this.translator = translator;
      this.calloutRenderer = calloutRenderer;
   }

   public WrapImage translate(WrapImage baseImage) {
      return translate(baseImage, false, null, null);
   }

   // |outDirectory| and |prefix| must not be null if |debug| is true.
   public WrapImage translate(WrapImage baseImage, boolean debug, String outDirectory, String prefix) {
      assert(!debug || (outDirectory != null && prefix != null));

      if (debug) {
         baseImage.write(String.format("%s/%s-translate-00-base.png", outDirectory, prefix));
      }

      BubbleInfo[] bubbles = bubbleDetector.extractBubblesWithInfo(baseImage);

      if (debug) {
         WrapImage coloredBubbles = bubbleDetector.colorBubbles(baseImage, bubbles);
         coloredBubbles.write(String.format("%s/%s-translate-01-fillBubbles.png", outDirectory, prefix));
         coloredBubbles.clear();
      }

      for (int bubbleIndex = 0; bubbleIndex < bubbles.length; bubbleIndex++) {
         BubbleInfo bubble = bubbles[bubbleIndex];

         String text = "";

         List<TextSet> bubbleText = textExtractor.extractText(bubble.image);
         if (bubbleText == null) {
            // TODO(eriq): Add a metaword, or some indication of failure?
            continue;
         }

         for (int textSetIndex = 0; textSetIndex < bubbleText.size(); textSetIndex++) {
            TextSet textSet  = bubbleText.get(textSetIndex);

            if (debug) {
               WrapImage noFuriReplace = textSet.toImage(false);
               WrapImage withFuriReplace = textSet.toImage(true);

               noFuriReplace.write(String.format("%s/%s-translate-02a-%02d-%02d-noFuriReplace.png",
                                                 outDirectory, prefix, bubbleIndex, textSetIndex));
               withFuriReplace.write(String.format("%s/%s-translate-02a-%02d-%02d-yesFuriReplace.png",
                                                   outDirectory, prefix, bubbleIndex, textSetIndex));

               noFuriReplace.clear();
               withFuriReplace.clear();
            }

            List<ImageText> imageTexts = preClassy.preClassify(textSet.furiganaReplacementText);
            String currentSetText = "";

            if (debug) {
               System.out.println("<PreClassify");

               System.out.print("{");
               for (ImageText imageText : imageTexts) {
                  if (imageText.text == null) {
                     System.out.print(" ");
                  } else {
                     System.out.print(imageText.text);
                  }
               }
               System.out.println("}");
               System.out.println("</PreClassify");
            }

            for (ImageText imageText : imageTexts) {
               if (imageText.text == null) {
                  for (WrapImage image : imageText.images) {
                     currentSetText += classy.classify(image);
                  }
               } else {
                  currentSetText += imageText.text;
               }
            }

            String postClassifyText = postClassy.postClassify(currentSetText);

            if (debug) {
               System.out.println("<PostClassify");
               System.out.println("Orig: " + currentSetText);
               System.out.println("Post: " + postClassifyText);
               System.out.println("</PostClassify");
            }

            text += postClassifyText + " ";
         }

         logger.debug(text.trim());

         String translation = translator.translate(text.trim());

         Rectangle renderArea = new Rectangle(bubble.startCol, bubble.startRow, bubble.width, bubble.height);
         baseImage = calloutRenderer.renderInto(baseImage, translation, renderArea);


         if (debug) {
            baseImage.write(String.format("%s/%s-translate-02b-%02d-renderInto.png",
                                          outDirectory, prefix, bubbleIndex));
         }                                      
      }

      if (debug) {
         baseImage.write(String.format("%s/%s-translate-99-final.png", outDirectory, prefix));
      }                                      

      return baseImage;
   }
}
