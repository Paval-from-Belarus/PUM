package org.petos.packagemanager;

import javafx.scene.shape.Arc;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class construct transfer message form from components.
 * From DataPackage delivered by Storage class. According to assembly type, this class produce
 * different types of output messages<br>
 * <h3>the main assumption: all characters are encoded by ASCII</h3>
 */
public class PackageAssembly {
private static final int BODY_SIGN = 0xCCEEDDFF;
public enum AssemblyType {Info, Package}
public enum EncryptionType {None, RC4, A5}
public enum ArchiveType {None, Lzma}
private EncryptionType encryption = EncryptionType.RC4;
private ArchiveType archive = ArchiveType.Lzma;
private PackageAssembly(PackageHeader header, byte[] payload){
      header.setArchiveType((char) ArchiveType.None.ordinal());
      header.setEncryptionType((char) EncryptionType.None.ordinal());
      //no encryption and no archive
      this.header = header;
      this.payload = payload;
      this.description = ""; //no description about package
}
private PackageAssembly(PackageHeader header, PackageInfo info, @NotNull byte[] payload) {
      this(header, payload);
      this.description = info.toJson();
}

public PackageAssembly setArchive(ArchiveType type){
      archive = type;
      header.setArchiveType((char)(archive.ordinal() + 1));
      return this;
}
public PackageAssembly setEncrypt(EncryptionType type){
      encryption = type;
      header.setEncryptionType((char)(encryption.ordinal() + 1));
      return this;
}

public byte[] serialize() {
      compress();
      encrypt();
      byte[] header = this.header.serialize();
      byte[] description = this.description.getBytes(StandardCharsets.US_ASCII);
      ByteBuffer buffer = ByteBuffer.allocate(header.length + description.length + payload.length + 4); //length for sign
      buffer.put(header);
      buffer.put(description);
      buffer.putInt(BODY_SIGN);
      buffer.put(payload);
      return buffer.array();
}
private void compress(){
      payload = payload;//compress
}
private void encrypt(){
      payload = payload;
}

public static @NotNull byte[] decrypt(byte[] payload, EncryptionType type){
      return payload;
}
public static @NotNull byte[] uncompress(byte[] payload, ArchiveType type){
      return payload;
}
public static @Nullable PackageAssembly deserialize(byte[] rawData){
      var header = PackageHeader.deserialize(rawData);
      if(header == null)
            return null;
      AtomicInteger offset = new AtomicInteger(header.size());
      String description = extractDescription(rawData, offset);
      if(description == null)
            return null;
      PackageInfo info = PackageInfo.fromJson(description);
      byte[] payload = new byte[rawData.length - offset.get()];//probably no payload
      System.arraycopy(rawData, offset.get(), payload, 0, payload.length);
      payload = decrypt(payload, convertEncryption(header.getEncryption()));
      payload = uncompress(payload, convertArchive(header.getArchive()));
      return new PackageAssembly(header, info, payload);
}
private static @NotNull EncryptionType convertEncryption(int index){
      if(index >= EncryptionType.values().length)
            index = EncryptionType.None.ordinal();
      return EncryptionType.values()[index];
}
private static @NotNull ArchiveType convertArchive(int index){
      if(index >= ArchiveType.values().length)
            index = ArchiveType.None.ordinal();
      return ArchiveType.values()[index];
}
/**As said above, assume that String is encoded by ASCII
 * @return null if rawData has incorrect format
 * */
private static @Nullable String extractDescription(byte[] rawData, AtomicInteger offset){
      ByteBuffer buffer = ByteBuffer.wrap(rawData, offset.get(), rawData.length - offset.get());
      String result = null;
      int index = 0;
      boolean hasPayload;
      do {
            hasPayload = buffer.getInt(index) == BODY_SIGN;
            index += 4;
      }while(hasPayload && index < buffer.capacity());
      if(hasPayload){
            StringBuilder strText = new StringBuilder();
            for(int i = 0; i < (index - 8); i += 4)
                  strText.append((char)buffer.get());
            result = strText.toString();
      }
      offset.set(index);
      return result;
}
private static int getControlSum(byte[] payload) {
      return 0x11;
}
public static PackageAssembly valueOf(@NotNull PackageHeader header, @NotNull PackageInfo info,@NotNull byte[] payload){
      return new PackageAssembly(header, info, payload);
}
public static PackageAssembly valueOf(@NotNull PackageHeader header, @NotNull byte[] payload){
      return new PackageAssembly(header, payload);
}
final private PackageHeader header;
private String description;
private byte[] payload;

}
