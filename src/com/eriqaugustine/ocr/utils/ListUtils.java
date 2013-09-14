package com.eriqaugustine.ocr.utils;

import java.util.List;

/**
 * Utils for Lists and Arrays.
 */
public class ListUtils {
   public static <T> void append(List<T> list, T[] toAdd) {
      for (T toAddElement : toAdd) {
         list.add(toAddElement);
      }
   }

   public static void append(List<Integer> list, int[] toAdd) {
      for (int toAddElement : toAdd) {
         list.add(new Integer(toAddElement));
      }
   }

   public static int[] toIntArray(List<Integer> list) {
      int[] rtn = new int[list.size()];
      for (int i = 0; i < list.size(); i++) {
         rtn[i] = list.get(i).intValue();
      }
      return rtn;
   }

   /**
    * Fill |base| with elements from |content|.
    * Start filling |base| at offset.
    * |base| must be large enough to fit all of |content|.
    */
   public static <T> void fill(T[] base, T[]content, int offset) {
      assert(base.length - offset + 1 >= content.length);

      for (int i = 0; i < content.length; i++) {
         base[i + offset] = content[i];
      }
   }
}
