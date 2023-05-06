package packages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class construct transfer message form from components.
 * From DataPackage delivered by Storage class. According to assembly type, this class produce
 * different types of output messages<br>
 * <h3>the main assumption: all characters are encoded by ASCII</h3>
 */
public class PackageAssembly {
public static class VerificationException extends Exception{
      VerificationException(String msg){
            super(msg);
      }
}
private static final int BODY_SIGN = 0xCCEEDDFF;
public enum AssemblyType {Info, Package}
public enum EncryptionType {None, RC4, RSA}
public enum ArchiveType {None, Lzma}
private EncryptionType encryption = EncryptionType.RC4;
private ArchiveType archive = ArchiveType.Lzma;
private PackageAssembly(PackageHeader header, byte[] payload){
      header.setArchiveType((char) ArchiveType.None.ordinal());
      header.setEncryptionType((char) EncryptionType.None.ordinal());
      //no encryption and no archive
      this.header = header;
      this.payload = payload;
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
public byte[] getPayload(){
      return payload;
}
public Integer getId(){
      return header.getId();
}
public Integer getVersion(){
      return header.getVersion();
}

public byte[] serialize() {
      compress();
      encrypt();
      int controlSum = getControlSum(payload);
      header.setPayloadHash(controlSum);
      byte[] header = this.header.serialize();
      ByteBuffer buffer = ByteBuffer.allocate(header.length + payload.length );
      buffer.put(header);
      buffer.put(payload);
      return buffer.array();
}
private void compress(){
      payload = payload;//compress
}
private void encrypt(){
      payload = payload;
}

public static @NotNull byte[] decrypt(byte[] payload, EncryptionType type) throws VerificationException {
      return payload;
}
public static @NotNull byte[] uncompress(byte[] payload, ArchiveType type) throws VerificationException {
      return payload;
}
public static @NotNull PackageAssembly deserialize(byte[] rawData) throws VerificationException {
      PackageHeader header = PackageHeader.deserialize(rawData);
      PackageAssembly result;
      if (header != null){
            byte[] payload = new byte[rawData.length - header.size()];//probably no payload
            System.arraycopy(rawData, header.size(), payload, 0, payload.length);
            payload = decrypt(payload, convertEncryption(header.getEncryption())); //throws
            payload = uncompress(payload, convertArchive(header.getArchive())); //throws
            result = new PackageAssembly(header, payload);
      } else {
            throw new VerificationException("The package header is damaged");
      }
      return result;
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
@Deprecated
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
public static PackageAssembly valueOf(@NotNull PackageHeader header, @NotNull byte[] payload){
      return new PackageAssembly(header, payload);
}
final private PackageHeader header;
private byte[] payload;

}
