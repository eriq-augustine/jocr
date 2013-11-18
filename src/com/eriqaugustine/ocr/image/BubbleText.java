package com.eriqaugustine.ocr.image;

import magick.MagickImage;

import java.awt.Dimension;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A representation of text that appears inside of a bubble.
 * There are a few important things to note when working with text from bubbles:
 *  - The text may be either LTR or Down.
 *    In manga, down is more common.
 *  - There can be multiple "sets" of text per bubble.
 *    This is usually caused when multiple bubbles collide with no border.
 *    It is a fairly common artistic technique.
 *  - Furigana is common.
 * This class attempts to handle all of these and provide a simple interface to the text
 *  (both kanji and kana).
 */
public class BubbleText {
   private enum Direction {
      LTR,
      DOWN
   };

   // TODO(eriq): Order the sets from right to left.
   private List<TextSet> textSets;

   /**
    * Extract the text-parts (not ocr) from |image|.
    * |image| should be the inner portion of a bubble and only contain text.
    */
   public BubbleText(MagickImage image) throws Exception {
      textSets = new ArrayList<TextSet>();

      List<Rectangle> setBoundaries = discoverSets(image);

      for (Rectangle setBoundary : setBoundaries) {
         Direction direction = guessDirection(image, setBoundary);
         textSets.add(gridBreakup(image, setBoundary, direction));
      }
   }

   /**
    * Find the sets of text in the bubble.
    */
   private static List<Rectangle> discoverSets(MagickImage image) throws Exception {
      // TODO(eriq): Remove noise before set discovery.

      // First, find all the bounding boxes for non-whitespace objects (text).
      List<Rectangle> boundingRectangles = findBoundingRectangles(image);
      boolean[] checkedRectangles = new boolean[boundingRectangles.size()];

      int[][] boundsMap = mapBounds(image.getDimension(), boundingRectangles);

      List<List<Rectangle>> groups = new ArrayList<List<Rectangle>>();

      // While there are still rectangles left to analyze.
      for (int currentRectangle = 0; currentRectangle < checkedRectangles.length;
           currentRectangle++) {
         if (checkedRectangles[currentRectangle]) {
            continue;
         }

         // Make a new group.
         List<Rectangle> group = new ArrayList<Rectangle>();
         Queue<Integer> queue = new LinkedList<Integer>();

         // Add the first rectangle to the queue.
         queue.add(currentRectangle);

         while (!queue.isEmpty()) {
            int rectIndex = queue.remove().intValue();
            Rectangle rect = boundingRectangles.get(rectIndex);

            checkedRectangles[rectIndex] = true;

            // Add the rectangle to the group.
            group.add(rect);

            // Check in all adjacent (not diagonal) directions for other rectangles, and add them.
            // Note: Some rectangles can slip through because they are inbetween two other
            //  rectangles. These will get caught and merged later when the bounding
            //  box for each group is negotiated.
            queue.addAll(getAdjacentRectangles(rect, boundingRectangles, boundsMap));
         }

         groups.add(group);
      }

      // Get the bounding rectangles for each group.
      List<Rectangle> groupBounds = findGroupBounds(groups);

      return mergeOverlappingGroups(groupBounds);
   }

   /**
    * Find any groups that overlap and merge them.
    */
   private static List<Rectangle> mergeOverlappingGroups(List<Rectangle> groups) {
      // TODO
      return null;
   }

   /**
    * Take in all the groups and merge their bounding boxes.
    */
   private static List<Rectangle> findGroupBounds(List<List<Rectangle>> groups) {
      // TODO
      return null;
   }

   /**
    * Find all the bounding rectangles that are laterially adjacent to |rect|.
    */
   private static List<Integer> getAdjacentRectangles(Rectangle rect,
                                                      List<Rectangle> boundingRectangles,
                                                      int[][] boundsMap) {
      // TODO
      return null;
   }

   /**
    * Find all the rectangles that minimally bound non-whitespace.
    */
   private static List<Rectangle> findBoundingRectangles(MagickImage image) {
      // TODO
      return null;
   }

   /**
    * Map the bounding rectangles onto a map.
    * Each map location will hold -1 for nothing, otherwise the index of the bounding rectangle.
    */
   private static int[][] mapBounds(Dimension dimensions, List<Rectangle> boundingRectangles) {
      // TODO
      return null;
   }

   /**
    * Guess the direction of the text.
    */
   private static Direction guessDirection(MagickImage image, Rectangle setBoundary) {
      // TODO
      return null;
   }

   /**
    * Break an image apart in characters based off of the idea that
    * Japanese characters (whether horozontal or vertical) always
    * fit in constant sized boxes.
    * Therefore, the image can be broken up into a grid and each position
    *  will represent a character, puncuation, or space.
    */
   private static TextSet gridBreakup(MagickImage image, Rectangle setBoundary,
                                      Direction direction) {
      // TODO
      return null;
   }

   /**
    * A container for a "set" of text.
    */
   // TODO(eriq): Define the types. String and rect won't work.
   // TODO(eriq): Breakout into formal class?
   private static class TextSet {
      /**
       * The entire set (including any kanji and furigana).
       */
      public final String fullText;

      /**
       * The set without any furigana.
       */
      public final String noFuriganaText;

      /**
       * The text with any furigana replacing the kanji it covers.
       */
      public final String furiganaReplacementText;

      public TextSet(String fullText, String noFuriganaText, String furiganaReplacementText) {
         // TODO
         this.fullText = null;
         this.noFuriganaText = null;
         this.furiganaReplacementText = null;
      }
   }
}
