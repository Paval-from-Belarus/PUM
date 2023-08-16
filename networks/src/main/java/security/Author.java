package security;

import org.jetbrains.annotations.NotNull;
import transfer.BinaryObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public record Author(@NotNull String name, @NotNull String email, @NotNull String token) {
      public static int PREF_TOKEN_LENGTH = 40;
      public String stringify(){
            Base64.Encoder encoder = Base64.getEncoder();
            byte[][] bytesList = new byte[][]{
                name.getBytes(StandardCharsets.US_ASCII), email.getBytes(StandardCharsets.US_ASCII),
                token.getBytes(StandardCharsets.US_ASCII)
            };
            StringBuilder strText = new StringBuilder();
            for (var bytes : bytesList) {
                  strText.append(BinaryObjectMapper.toString(encoder.encode(bytes))).append("#");
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
                        params[index++] = BinaryObjectMapper.toString(converted);
                  }
                  result = new Author(params[0], params[1], params[2]);
            }
            return Optional.ofNullable(result);
      }
}
