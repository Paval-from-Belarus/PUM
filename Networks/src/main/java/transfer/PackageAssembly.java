package transfer;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import security.Encryptor;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class construct transfer message form from components.
 * From DataPackage delivered by Storage class. According to assembly type, this class produce
 * different types of output messages<br>
 * <h3>the main assumption: all characters are encoded by ASCII</h3>
 */
public class PackageAssembly {
public static class VerificationException extends Exception {
      VerificationException(String msg) {
	    super(msg);
      }

      VerificationException(Throwable cause) {
	    super(cause);
      }
}

private static final int BODY_SIGN = 0xCCEEDDFF;

public enum AssemblyType {Library, Application}

public enum ArchiveType {None, GZip, Brotli, LZ77}

private final Encryptor encryptor;
private ArchiveType archive = ArchiveType.None;

private PackageAssembly(PackageHeader header, byte[] payload) {
      header.setArchiveType((char) ArchiveType.None.ordinal());
      header.setEncryptionType((char) Encryptor.Encryption.None.ordinal());
      //no encryption and no archive
      this.header = header;
      this.payload = payload;
      this.encryptor = new Encryptor(Encryptor.Encryption.None);
}

public PackageAssembly setArchive(ArchiveType type) {
      archive = type;
      header.setArchiveType((char) (archive.ordinal()));
      return this;
}

/**
 * This method attemp to change the old encryption to new
 * If chosen encryption is not available, the last is present
 */
public PackageAssembly setEncryption(@NotNull Encryptor.Encryption type) {
      if (Encryptor.validate(type, payload)) {
	    encryptor.setType(type);
	    header.setEncryptionType((char) (encryptor.getType()).ordinal());
      }
      return this;
}

public byte[] getPayload() {
      return payload;
}

public Integer getId() {
      return header.getId();
}

public Integer getVersion() {
      return header.getVersion();
}

public byte[] serialize() {
      compress();
      encrypt();
      int controlSum = getControlSum(payload);
      header.setPayloadHash(controlSum);
      byte[] header = this.header.serialize();
      ByteBuffer buffer = ByteBuffer.allocate(header.length + payload.length);
      buffer.put(header);
      buffer.put(payload);
      return buffer.array();
}

private void compress() {
      try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(payload.length)) {
	    OutputStream output = switch (archive) {
		  case None -> byteOutput;
		  case GZip -> gzipCompression(byteOutput);
		  case Brotli -> brotliCompression(byteOutput);
		  case LZ77 -> lz77Compression(byteOutput);
	    };
	    output.write(payload);
	    output.flush();
	    output.close();
	    payload = byteOutput.toByteArray();
      } catch (IOException e) {
	    throw new IllegalStateException("The compression failed");
      }
}

public static @NotNull byte[] decompress(byte[] compressed, ArchiveType archive) throws VerificationException {
      byte[] result;
      try (ByteArrayInputStream byteInput = new ByteArrayInputStream(compressed)) {
	    InputStream input = switch (archive) {
		  case None -> byteInput;
		  case GZip -> gzipDecompression(byteInput);
		  case Brotli -> brotliDecompression(byteInput);
		  case LZ77 -> lz77Decompression(byteInput);
	    };
	    result = input.readAllBytes();
      } catch (IOException e) {
	    throw new VerificationException(e);
      }
      return result;
}

private void encrypt() {
      this.payload = encryptor.encrypt(payload);
}

public static @NotNull byte[] decrypt(byte[] payload, Encryptor.Encryption type) throws VerificationException {
      Encryptor encryptor = new Encryptor(type);
      return encryptor.decrypt(payload);
}

private static @Nullable Encryptor.Encryption getCompatible(@NotNull PackageHeader header, Encryptor.Encryption type) {
      boolean isCompatible = header.getEncryption() < Encryptor.Encryption.values().length;
      Encryptor.Encryption packageEncryption = null;
      Encryptor.Encryption result = null;
      if (isCompatible) {
	    packageEncryption = Encryptor.Encryption.values()[header.getEncryption()];
	    isCompatible = type.isCompatible(packageEncryption);
      }
      if (isCompatible) {
	    result = packageEncryption;
      }
      return result;
}

public static @NotNull PackageAssembly deserialize(byte[] rawData, @NotNull Encryptor.Encryption type) throws VerificationException {
      PackageHeader header = PackageHeader.deserialize(rawData);
      PackageAssembly result;
      Encryptor.Encryption compatible;
      if (header != null && (compatible = getCompatible(header, type)) != null) {
	    byte[] payload = new byte[rawData.length - header.size()];//probably no payload
	    System.arraycopy(rawData, header.size(), payload, 0, payload.length);
	    payload = decrypt(payload, compatible);
	    payload = decompress(payload, convertArchive(header.getArchive())); //throws
	    result = new PackageAssembly(header, payload);
      } else {
	    String msg = "Unknown cause of exception";
	    if (header == null) {
		  msg = "The package header is damaged";
	    } else if (header.getEncryption() != type.ordinal()) {
		  msg = "The encryption type is not corresponds";
	    }
	    throw new VerificationException(msg);
      }
      return result;
}

private static @NotNull ArchiveType convertArchive(int index) {
      if (index >= ArchiveType.values().length)
	    index = ArchiveType.None.ordinal();
      return ArchiveType.values()[index];
}

/**
 * As said above, assume that String is encoded by ASCII
 *
 * @return null if rawData has incorrect format
 */
@Deprecated
private static @Nullable String extractDescription(byte[] rawData, AtomicInteger offset) {
      ByteBuffer buffer = ByteBuffer.wrap(rawData, offset.get(), rawData.length - offset.get());
      String result = null;
      int index = 0;
      boolean hasPayload;
      do {
	    hasPayload = buffer.getInt(index) == BODY_SIGN;
	    index += 4;
      } while (hasPayload && index < buffer.capacity());
      if (hasPayload) {
	    StringBuilder strText = new StringBuilder();
	    for (int i = 0; i < (index - 8); i += 4)
		  strText.append((char) buffer.get());
	    result = strText.toString();
      }
      offset.set(index);
      return result;
}

private static int getControlSum(byte[] payload) {
      return PackageHeader.getControlSum(payload);
}

public static PackageAssembly valueOf(@NotNull PackageHeader header, @NotNull byte[] payload) {
      return new PackageAssembly(header, payload);
}

private @NotNull GZIPOutputStream gzipCompression(OutputStream destination) throws IOException {
      return new GZIPOutputStream(destination);
}

private @NotNull BrotliOutputStream brotliCompression(OutputStream destination) throws IOException {
      Brotli4jLoader.ensureAvailability();
      Encoder.Parameters params = new Encoder.Parameters().setQuality(6);
      return new BrotliOutputStream(destination, params);
}

//todo: !
private @NotNull OutputStream lz77Compression(OutputStream destination) throws IOException {
      return destination;
}

private @NotNull
static GZIPInputStream gzipDecompression(InputStream source) throws IOException {
      return new GZIPInputStream(source);
}

private @NotNull
static BrotliInputStream brotliDecompression(InputStream source) throws IOException {
      Brotli4jLoader.ensureAvailability();
      return new BrotliInputStream(source);
}

private @NotNull
static InputStream lz77Decompression(InputStream source) throws IOException {
      return source;
}

final private PackageHeader header;
private byte[] payload;

}
