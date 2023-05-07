package security;

import org.jetbrains.annotations.NotNull;
import transfer.NetworkPacket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

public record Author(String author, String email, String token) {
      public String stringify(){
            Base64.Encoder encoder = Base64.getEncoder();
            byte[][] bytesList = new byte[][]{
                author.getBytes(StandardCharsets.US_ASCII), email.getBytes(StandardCharsets.US_ASCII),
                token.getBytes(StandardCharsets.US_ASCII)
            };
            StringBuilder strText = new StringBuilder();
            for (var bytes : bytesList) {
                  strText.append(NetworkPacket.bytesToString(encoder.encode(bytes))).append("#");
            }
            strText.setLength(strText.length() - 1);
            return strText.toString();
      }
      public static Optional<Author> valueOf(@NotNull String base64Line) {
            String[] parts = base64Line.split("#");
            Base64.Decoder decoder = Base64.getDecoder();
            Author result = null;
            if (parts.length == 3) {
                  String[] params = new String[3];
                  int index = 0;
                  for (String line : parts) {
                        byte[] converted = decoder.decode(line.getBytes(StandardCharsets.US_ASCII));
                        params[index++] = NetworkPacket.bytesToString(converted);
                  }
                  result = new Author(params[0], params[1], params[2]);
            }
            return Optional.ofNullable(result);
      }
}
