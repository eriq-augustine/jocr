package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.GeoUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Pull text images out of a bubble.
 * There are a few important things to note when working with text from bubbles:
 *  - The text may be either LTR or Down.
 *    In manga, down is far more common.
 *  - There can be multiple "sets" of text per bubble.
 *    This is usually caused when multiple bubbles collide with no border.
 *    It is a fairly common artistic technique.
 *  - Furigana is common.
 * Extractors should handle all of these.
 */
public abstract class TextExtractor {
   public enum Direction {
      LTR,
      DOWN
   };

   /**
    * Extract the text-parts (not ocr) from |image|.
    * |image| should be the inner portion of a bubble and only contain text.
    */
   public abstract List<TextSet> extractText(WrapImage image);
}
