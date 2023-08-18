package org.petos.pum.server.network;


import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Paval Shlyk
 * @since 17/08/2023
 */
public class Solution {
public static void main(String[] args) {
      int[][] numbers = {
	  {7, 8}, {12, 16}, {12, 5}, {1, 8}, {4, 19}, {3, 15}, {4, 10}, {9, 16}
      };
      var instance = new Solution();
      instance.maxEnvelopes(numbers);
}

private static class Enveloper {
      private int index;
      private int currCnt; //exactly stores in matrix

      private Enveloper(int index, int currCnt) {
	    this.index = index;
	    this.currCnt = currCnt;
      }

}

private static final Comparator<int[]> comparator = (left, right) -> {
      if (left[0] == right[0]) {
	    return left[1] - right[1];
      }
      return left[0] - right[0];
};

public int maxEnvelopes(int[][] envelopes) {
      Arrays.sort(envelopes, comparator);
      int[] lengths = new int[envelopes.length];
      for (int i = 0; i < envelopes.length; i++) {
	    for (int j = i + 1; j < envelopes.length; j++) {
		  if (envelopes[i][0] < envelopes[j][0] && envelopes[i][1] < envelopes[j][1] && lengths[j] < lengths[i] + 1) {
			lengths[j] = lengths[i] + 1;
		  }
	    }
      }
      return lengths[lengths.length - 1] + 1;
}

}
