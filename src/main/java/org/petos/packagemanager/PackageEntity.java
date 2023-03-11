package org.petos.packagemanager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;

public class PackageEntity {
public static EncryptionType encryption = EncryptionType.RC4;
public static ArchiveType archive = ArchiveType.Lzma;

public enum EncryptionType {RC4}
public enum ArchiveType {Lzma}

private PackageEntity(PackageHeader header, PackageInfo info, byte[] payload){
      this.header = header;
      this.info = info;
      this.payload = payload;
}
private PackageEntity(DataPackage dataPackage, int packageId, int versionId) {
      header = new PackageHeader(packageId, versionId);
      header.setEncryptionType((char) encryption.ordinal());
      header.setArchiveType((char) archive.ordinal());
      info = dataPackage.info;
      payload = dataPackage.payload;
      header.setPayloadHash(getControlSum(payload));
}

public String serialize() {
      byte[] header = this.header.serialize();
      String result =
          bytesToString(header) + info.toJson() + bytesToString(payload);
      return result;
}

public static @Nullable PackageEntity deserialize(String rawEntity) {
      PackageHeader header;
      PackageInfo info;
      PackageEntity entity;
      //convert header
      String pattern = String.valueOf((char)PackageHeader.SIGN_TAIL);
      int headBorder = rawEntity.indexOf(pattern);
      ByteBuffer bytes = ByteBuffer.allocate((headBorder + 1) * 2);
      for(char letter : rawEntity.substring(0, headBorder + 1).toCharArray())
            bytes.putChar(letter);
      header = PackageHeader.deserialize(bytes.array());
      if(header == null)
            return null;
      //package info
      int jsonBorder = rawEntity.indexOf('}', headBorder);
      String rawJson = rawEntity.substring(headBorder + 1, jsonBorder + 1);
      info = PackageInfo.fromJson(rawJson);
      if(info == null)
            return null;
      //payload
      bytes = ByteBuffer.allocate(rawEntity.length() * 2 - jsonBorder * 2);
      for(char letter : rawEntity.substring(jsonBorder + 1).toCharArray()){
            bytes.putChar(letter);
      }
      if(getControlSum(bytes.array()) != header.getPayloadHash())
            return null;
      //entity
      entity = new PackageEntity(header, info, bytes.array());
      return entity;
}

private @NotNull String bytesToString(@NotNull byte[] bytes) {
      int buffSize = (bytes.length / 2) + ((bytes.length % 2 == 0) ? 0 : 1);
      CharBuffer buffer = CharBuffer.allocate(buffSize);
      for (int i = 0; i < bytes.length; i += 2) {
	    char letter = (char) (bytes[i] << 8 | bytes[i + 1]);
	    buffer.append(letter);
      }
      if (buffer.position() < buffer.capacity())
	    buffer.put((char) ((int) bytes[bytes.length - 1]));

      return String.valueOf(buffer.array());
}

public static PackageEntity valueOf(DataPackage dataPackage, int packageId, int versionId) {
      PackageEntity entity = new PackageEntity(dataPackage, packageId, versionId);
      return entity;
}

private static int getControlSum(byte[] payload){
      return 0x11;
}
final private PackageHeader header;
final private PackageInfo info;
private byte[] payload;

}
