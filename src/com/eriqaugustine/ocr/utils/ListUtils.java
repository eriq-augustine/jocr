package com.eriqaugustine.ocr.utils;

import java.util.List;

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
}
