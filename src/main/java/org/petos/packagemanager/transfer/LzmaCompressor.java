package org.petos.packagemanager.transfer;

import org.petos.packagemanager.client.storage.PackageStorage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LzmaCompressor {
public final int WINDOW_SIZE = 10_000;
public static class Node {
      final int offset;
      final int length;
      byte next;
      boolean isLast;

      public Node(int offset, int length) {
	    this.offset = offset;
	    this.length = Math.max(0, length);
	    this.isLast = false;
      }

      @Override
      public String toString() {
	    return String.format("(%d, %d, %d)", offset, length, next);
      }

      public static Node empty() {
	    return new Node(0, 0);
      }
}

public static class FloatWindow {
      private final int windowSize;
      private int lPivot;
      private int rPivot;

      FloatWindow(int windowSize) {
	    this.windowSize = windowSize;
	    lPivot = 0;
	    rPivot = 0; //rPivot is always outside the window and never cross with lPivot (only on initial state)
      }

      Node nextNode(byte[] source, int offset) {
	    if (rPivot == lPivot) {
		  return Node.empty();
	    }
	    final int floatSize = (rPivot - lPivot);//should be zero!!!
	    int byteOffset = offset; //always the following byte after current
	    int pivotOffset = lPivot;
	    boolean isOverJumped = false;
	    for (int pivot = lPivot; !isOverJumped && pivot < rPivot; pivot++) {
		  int tempOffset = offset;
		  int tinyPivot = pivot;
		  while (tempOffset < source.length && source[tempOffset] == source[tinyPivot]) {
			tempOffset += 1;
			tinyPivot = (tinyPivot + 1) % floatSize + lPivot;
		  }
		  if (tempOffset >= byteOffset) {
			byteOffset = tempOffset;
			pivotOffset = pivot;
		  }
//		    if (tempOffset >= source.length)
//			  tempOffset += 1;//align to empty byte
//		    byteOffset = Math.max(byteOffset, tempOffset);
		  isOverJumped = (tempOffset - offset) >= floatSize;
	    }
	    return new Node(Math.min(offset - pivotOffset, floatSize), byteOffset - offset);
      }

      void shift(int byteCnt) {
	    rPivot += byteCnt;
	    lPivot = Math.max(0, rPivot - windowSize);
      }
}
public LzmaCompressor() {
}

public List<Node> zip(byte[] source) {
      FloatWindow window = new FloatWindow(WINDOW_SIZE);
      List<Node> result = new ArrayList<>();
      int pos = 0;
      while (pos < source.length) {
	    Node node = window.nextNode(source, pos);
	    window.shift(node.length + 1);
	    pos += node.length;
	    if (pos < source.length)
		  node.next = source[pos++];
	    else
		  node.isLast = true;
//	    if (pos == source.length && node.length == 0)
//	    if (node.length == 0) {
//		  pos += 1;
////		  window.shift(1);
//	    } else {
////		  window.shift(node.length + 1);
//	    }
	    //else do nothing because the end of stream is not used
	    result.add(node);
      }
      result.forEach(System.out::println);
      return result;
}

private int fillBytesAndGetOffset(byte[] result, int byteOffset, Node node) {
      if (node.length > 0) {
	    int start = byteOffset - node.offset;
	    for (int i = 0; i < node.length; i++) {
		  result[byteOffset++] = result[start + i];
	    }
      }
      return byteOffset;
}

public byte[] unzip(List<Node> nodes, int resultSize) {
      byte[] result = new byte[resultSize];
      int byteOffset = 0; //the next byte in result
      for (int j = 0; j < nodes.size() - 1; j++) {
	    var node = nodes.get(j);
	    byteOffset = fillBytesAndGetOffset(result, byteOffset, node);
	    result[byteOffset++] = node.next;
      }
      var node = nodes.get(nodes.size() - 1);
      byteOffset = fillBytesAndGetOffset(result, byteOffset, node);
      if (!node.isLast)
	    result[byteOffset] = node.next;
      return result;
}

public static byte[] deltaEncode(byte[] source) {
      byte last = 0;
      byte[] output = new byte[source.length];
      int index = 0;
      for (byte octet : source) {
	    output[index] = (byte) (octet - last);
	    last = octet;
	    index += 1;
      }
      return output;
}

public static byte[] deltaDecode(byte[] source) {
      byte last = 0;
      byte[] output = new byte[source.length];
      int index = 0;
      for (byte octet : source) {
	    output[index] = (byte) (octet + last);
	    last = output[index];
	    index += 1;
      }
      return output;
}

public static void main(String[] args) {
      List<String> testCases = List.of(
	  "abehhilopsu", "Salamandra", "abcde", "Nothing But", "ButButBut", "Okke", "a"
      );
      for (String test : testCases) {
	    LzmaCompressor lzma = new LzmaCompressor();
	    var bytes = test.getBytes(StandardCharsets.US_ASCII);
	    var compressed = lzma.zip(deltaEncode(bytes));
	    byte[] restored = deltaDecode(lzma.unzip(compressed, bytes.length));
	    System.out.print(test + "\t");
	    for (byte value : restored) {
		  System.out.print((char) value);
	    }
	    System.out.println();
      }

      byte[] bytes = {2, 3, 4, 6, 7, 9, 8, 7, 5, 3, 4};
      byte[] encoded = deltaEncode(bytes);
//      System.out.println(Arrays.toString(encoded));
//      System.out.println(Arrays.toString(deltaDecode(encoded)));
}
}
