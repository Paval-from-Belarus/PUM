package org.petos.pum.networks.transfer;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LZ77Compressor {
private static final byte DUMMY_TAIL_MARKER = 0x77;

public LZ77Compressor(WindowType type, final byte[] content) {
      this.windowType = type;
      this.content = content;
}

public enum WindowType {
      cb4096, cb32768, cb65536;
      private int bytes;

      static {
	    cb4096.bytes = 4096;
	    cb32768.bytes = 32768;
	    cb65536.bytes = 65536;
      }
      public int getSize() {
	    return bytes;
      }
}
@EqualsAndHashCode
private static class Entry {
      final int offset;
      final int length;
      byte next;
      boolean isDummy;

      public Entry(int offset, int length) {
	    this.offset = offset;
	    this.length = Math.max(0, length);
	    this.isDummy = false;
      }

      @Override
      public String toString() {
	    return String.format("(%d, %d, %d)", offset, length, next);
      }

      public static Entry empty() {
	    return new Entry(0, 0);
      }
}

private static class FloatWindow {
      private final int windowSize;
      private int lPivot;
      private int rPivot;
      private final int MAX_LENGTH = 4095;
      FloatWindow(int windowSize) {
	    this.windowSize = windowSize;
	    lPivot = 0;
	    rPivot = 0; //rPivot is always outside the window and never cross with lPivot (only on initial state)
      }

      Entry nextEntry(byte[] source, int offset) {
//	    final int floatSize = Math.min(rPivot - lPivot, windowSize);
	    final int floatSize = (rPivot - lPivot);//should be zero!!!
	    int byteOffset = offset; //always the following byte after current
	    int pivotOffset = lPivot;
	    boolean isOverJumped = floatSize <= 0;
	    boolean wasTouched = false;
	    for (int pivot = 0; !isOverJumped && pivot < floatSize; pivot++) {
		  int tempOffset = offset;
		  int tinyPivot = pivot;
		  while (tempOffset - offset < MAX_LENGTH && tempOffset < source.length && source[tempOffset] == source[tinyPivot + lPivot]) {
			wasTouched = true;
			tempOffset += 1;
			tinyPivot = (tinyPivot + 1) % (floatSize);
		  }
		  if (tempOffset >= byteOffset) {
			byteOffset = tempOffset;
			pivotOffset = pivot + lPivot;
		  }
//		    if (tempOffset >= source.length)
//			  tempOffset += 1;//align to empty byte
//		    byteOffset = Math.max(byteOffset, tempOffset);
		  isOverJumped = (tempOffset - offset) >= floatSize; //tempOffset == rightWindowBorder + 1 -> always greater
		  							//the +1 is already defined
	    }
	    Entry result;
	    if (wasTouched)
		  result = new Entry(Math.min(offset - pivotOffset, floatSize), byteOffset - offset);
	    else
		  result = Entry.empty();
	    return result;
      }

      void shift(int byteCnt) {
	    rPivot += byteCnt;
	    lPivot = Math.max(0, rPivot - windowSize + 1);
      }
}

private static class HeaderProps {
      HeaderProps(WindowType type, int payloadSize) {
	    entrySize = bitsPerEntry(type);
	    short paramSize = (short) ((entrySize - 8) >> 1);
	    boOffset = (short) (paramSize + 8); //bitOffset
	    boLength = 8;
	    this.payloadSize = payloadSize;
	    this.type = type;
      }

      public byte[] toBytes() {
	    ByteBuffer buffer = ByteBuffer.allocate(5);
	    buffer.put((byte) type.ordinal());
	    buffer.putInt(payloadSize);
	    return buffer.array();
      }

      private final WindowType type;
      private final int entrySize;
      private final short boOffset;
      private final short boLength;
      private final int payloadSize;

      public int bytesPerEntry() {
	    return (entrySize + 7) / 8;
      }

      public int bitsPerEntry() {
	    return entrySize;
      }

      public int size() {
	    return 5;
      }

      public static Optional<HeaderProps> valueOf(byte[] header) {
	    if (header.length < 5)
		  return Optional.empty();
	    ByteBuffer buffer = ByteBuffer.allocate(5);
	    buffer.put(header, 0, 5);
	    buffer.position(0);
	    WindowType type = WindowType.values()[buffer.get()];
	    int payloadSize = buffer.getInt();
	    return Optional.of(new HeaderProps(type, payloadSize));
      }

      private static int bitsPerEntry(@NotNull WindowType type) {
	    final int[] SIZES = {32, 38, 40};//in second case 2 bits are losed
	    return SIZES[type.ordinal()];
      }

      private static int getParamMask(int entrySize) {
	    final int[] MASKS = {0xFFF, 0x7FFF, 0xFFFF};
	    int result;
	    switch (entrySize) {
		  case 32 -> result = MASKS[0];
		  case 38 -> result = MASKS[1];
		  case 40 -> result = MASKS[2];
		  default -> throw new IllegalStateException("Invalid entry size");
	    }
	    return result;
      }

      public static Entry entryOf(long value, int entrySize) {
	    short paramSize = (short) ((entrySize - 8) >> 1);
	    int paramMask = getParamMask(entrySize);
	    short boOffset = (short) (paramSize + 8); //bitOffset
	    short boLength = 8;
	    int offset = (int) (value >>> boOffset) & paramMask;
	    int length = (int) (value >>> boLength) & paramMask;
	    byte next = (byte) (value & 0xFF);
	    Entry entry = new Entry(offset, length);
	    entry.next = next;
	    return entry;
      }
}

public byte[] zip() {
      byte[] source = deltaEncode(this.content);
      FloatWindow window = new FloatWindow(windowType.getSize());
      List<Entry> result = new ArrayList<>();
      int pos = 0;
      while (pos < source.length) {
	    Entry entry = window.nextEntry(source, pos);
	    window.shift(entry.length + 1);
	    pos += entry.length;
	    if (pos < source.length)
		  entry.next = source[pos++];
	    else
		  entry.isDummy = true;
	    result.add(entry);
      }
      lastEntries = result;
//      result.forEach(System.out::println);
      return toBytes(result, source.length);
}

private byte[] toBytes(List<Entry> entries, int payloadSize) {
      var header = new HeaderProps(windowType, payloadSize);
      ByteBuffer buffer = ByteBuffer.allocate(header.size() + header.bytesPerEntry() * entries.size() + 1); //last for tail
      buffer.put(header.toBytes());
      for (Entry entry : entries) {
	    long value = ((long) entry.offset << header.boOffset) | ((long) entry.length << header.boLength) | (char) entry.next & 0xFF;
	    for (int i = header.bytesPerEntry() - 1; i >= 0; i--) {
		  buffer.put((byte) ((value >> (i * 8)) & 0xFF));
	    }
      }

      byte tailMarker = 0;
      if (entries.get(entries.size() - 1).isDummy)
	    tailMarker = DUMMY_TAIL_MARKER;
      buffer.put(tailMarker);
      return buffer.array();
}
private static int WINDOW_SIZE;
private static int fillBytesAndGetOffset(byte[] result, int byteOffset, Entry entry) {
      final int floatSize = WINDOW_SIZE - 1;
      final int lBorder = Math.max(0, byteOffset - floatSize);
      if (entry.length > 0) {
	    int pivot = byteOffset - entry.offset - lBorder;
	    for (int i = 0; i < entry.length; i += 1) {
		  result[byteOffset++] = result[pivot + lBorder];
		  pivot = (pivot + 1) % floatSize;
	    }
      }
      return byteOffset;
}

private static byte[] unzip(List<Entry> entries, int resultSize) {
      byte[] result = new byte[resultSize];
      int byteOffset = 0; //the next byte in result
      for (int j = 0; j < entries.size() - 1; j++) {
	    var node = entries.get(j);
	    byteOffset = fillBytesAndGetOffset(result, byteOffset, node);
	    result[byteOffset++] = node.next;
      }
      var node = entries.get(entries.size() - 1);
      byteOffset = fillBytesAndGetOffset(result, byteOffset, node);
      if (!node.isDummy)
	    result[byteOffset] = node.next;
      return deltaDecode(result);
}
private static List<Entry> lastEntries;
//utils methods
public static Optional<byte[]> unzip(byte[] source) {
      if (source.length <= 5) {
	    return Optional.empty();
      }
      byte[] result = null;
      var props = HeaderProps.valueOf(source);
      List<Entry> nodes = new ArrayList<>();
      if (props.isPresent()) {
	    var header = props.get();
	    LZ77Compressor.WINDOW_SIZE = header.type.getSize();
	    byte[] content = new byte[source.length - header.size() - 1];
	    System.arraycopy(source, header.size(), content, 0, content.length);
	    int bytesCnt = 0;
	    long accumulator = 0;
	    for (byte value : content) {
		  bytesCnt += 1;
		  accumulator = (accumulator << 8) | (char) value & 0xFF;
		  if (bytesCnt == header.bytesPerEntry()) {
			var entry = HeaderProps.entryOf(accumulator, header.bitsPerEntry());
			nodes.add(entry);
			bytesCnt = 0;
			accumulator = 0;
		  }
	    }
	    byte last = source[source.length - 1];
	    if (last == DUMMY_TAIL_MARKER)
		  nodes.get(nodes.size() - 1).isDummy = true;
//	    System.out.println("UNZIP");
//	    nodes.forEach(System.out::println);
	    assert lastEntries != null && lastEntries.size() == nodes.size();
	    int index = 0;
	    System.out.println("The following nodes are diff:");
	    for (var node : nodes) {
		  if (!node.equals(lastEntries.get(index))) {
			System.out.println(node);
		  }
		  index += 1;
	    }
	    result = unzip(nodes, header.payloadSize);
      }
      return Optional.ofNullable(result);
}


public static byte[] deltaEncode(byte[] source) {
//      return source;
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
//      return source;
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

private final WindowType windowType;
private byte[] content;
public static void main(String[] args) {
      List<String> testCases = new ArrayList<>(List.of(
	  "abehhilopsu", "Salamandra", "abcde", "Nothing But", "ButButBut", "Okke", "a",
	  "abacababacabc"
      ));
      var zeroes = new byte[4098 * 2];
      zeroes[1] = 1;
      zeroes[4097] = 1;
      zeroes[4098] = 1;
      zeroes[4098 * 2 - 2] = 1;
      LZ77Compressor compressor = new LZ77Compressor(WindowType.cb4096, zeroes);
      var coded = compressor.zip();
      var decoded = LZ77Compressor.unzip(coded).get();
      for (int i = 0; i < decoded.length; i++) {
	    if (decoded[i] != zeroes[i]){
		  System.out.println("DIFF");
		  return;
	    }
      }
      try {
//	    var bytes = Files.readAllBytes(Path.of("client/usr/programs/pum/packages/PetOS Kernel/bin/PetOS Kernel.exe"));
	    var bytes = Files.readAllBytes(Path.of("Server/target/Server-0.0.1.jar"));
	    LZ77Compressor lz = new LZ77Compressor(WindowType.cb32768, bytes);
	    var compressed = lz.zip();
	    var uncompressed = LZ77Compressor.unzip(compressed);
	    if (uncompressed.isPresent())
		  Files.write(Path.of("Networks/pure.exe"), uncompressed.get());
	    Files.write(Path.of("Networks/test.zip"), compressed);
//	    Files.write(Path.of("pure.exe"), uncompressed);

      } catch (IOException e) {
	    System.out.println(e.getMessage());
      }
      for (String test : testCases) {
	    var bytes = test.getBytes(StandardCharsets.US_ASCII);
	    LZ77Compressor lzma = new LZ77Compressor(WindowType.cb4096, bytes);
	    byte[] compressed = lzma.zip();
	    var optional = LZ77Compressor.unzip(compressed);
	    if (optional.isPresent()) {
		  var restored = optional.get();
		  System.out.print(test + "\t");
		  for (byte value : restored) {
			System.out.print((char) value);
		  }
		  System.out.println();
	    }

      }
}
}
