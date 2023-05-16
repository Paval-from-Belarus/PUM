package requests;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

abstract class AbstractRequest implements SimpleRequest{
protected static String stringify(Integer... values) {
      Base64.Encoder encoder = Base64.getEncoder();
      ByteBuffer buffer = ByteBuffer.allocate(4);
      StringBuilder strText = new StringBuilder();
      for (Integer value : values) {
	    buffer.putInt(value).position(0);
	    strText.append(encoder.encodeToString(buffer.array())).append("#");
      }
      if (values.length > 0)
	    strText.setLength(strText.length() - 1);
      return strText.toString();
}
protected static String join(String base64, String line) {
      byte[] bytes = line.getBytes(StandardCharsets.US_ASCII);
      return base64 + "#" + Base64.getEncoder().encodeToString(bytes);
}
protected static String join(String base64, Integer value) {
      ByteBuffer buffer = ByteBuffer.allocate(4);
      buffer.putInt(value).position(0);
      return join(base64, Base64.getEncoder().encodeToString(buffer.array()));
}
protected static List<byte[]> split(String content) { //return the array of byte[] as result
      Base64.Decoder decoder = Base64.getDecoder();
      String[] parts = content.split("#");
      List<byte[]> bytes = new ArrayList<>();
      boolean areValid = checkParts(parts);
      if (areValid) {
	    for (String part : parts) {
		  bytes.add(decoder.decode(part));
	    }
      }
      return bytes;
}
private static boolean checkParts(String[] parts) {
      boolean response = true;
      int index = 0;
      while (response && index < 2) {
	    response = parts[index].length() >= 2;
	    index += 1;
      }
      return response;
}
protected static String toString(byte[] bytes) {
      return new String(bytes, StandardCharsets.US_ASCII);
}
protected static Integer toInteger(byte[] bytes) {
      return ByteBuffer.allocate(bytes.length).put(bytes).position(0).getInt();
}
}
