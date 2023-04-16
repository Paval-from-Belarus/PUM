package org.petos.packagemanager.packages;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class PackageHeader {

public final static int SIGN_HEAD = 0xAA33;
public final static int SIGN_BODY = 0xFFFF;
public final static int SIGN_TAIL = 0xCC55;
public final static int MinBytesPerHeader = 26;

{
      signHead = SIGN_HEAD;
      signBody = SIGN_BODY;
      signTail = SIGN_TAIL;
      archiveType = 0;
      encryptionType = 0;
      dependencies = new int[0];
      payloadHash = 0;
      headerHash = -1;
}

public PackageHeader(int id, int version) {
      this.id = id;
      this.version = version;
}

public PackageHeader(int id, int version, int[] dependencies) {
      this(id, version);
      this.dependencies = dependencies;
}
public int getPayloadHash(){
      return payloadHash;
}
public void setPayloadHash(int hash) {
      this.payloadHash = hash;
}

public void setDependencies(int[] dependencies) {
      this.dependencies = dependencies;
}

public void setEncryptionType(char type) {
      encryptionType = type;
}

public void setArchiveType(char type) {
      archiveType = type;
}
public char getEncryption(){
      return encryptionType;
}
public char getArchive(){
      return archiveType;
}
public int size(){
      return MinBytesPerHeader + dependencies.length * 4;
}

public byte[] serialize() {
      ByteBuffer buffer = ByteBuffer.allocate(MinBytesPerHeader + dependencies.length * 4);
      buffer.putChar(signHead)
	  .putInt(id)
	  .putInt(version);
      for (int value : dependencies)
	    buffer.putInt(value);
      buffer.putChar(signBody)
	  .putChar(encryptionType)
	  .putChar(archiveType)
	  .putInt(payloadHash)
	  .putInt(headerHash);
      int headerHash = getHeaderHash(buffer.array());
      buffer.putInt(buffer.capacity() - 6, headerHash);
      buffer.putChar(signTail);
      return buffer.array();
}
//calculate header hash in assumption that headerHash is equal 0xFF...FF
private int getHeaderHash(byte[] header) {
      return 1;
}


final private char signHead;
//unique id of current package
final private int id;
//unique id of version of current package
final private int version;
//ids of all packages that used to resolve dependencies
//this field should be used to quick install additional dependencies
private int[] dependencies;
final private char signBody;
//the following two field are optional
//if they are set to zero ― no encryption/archivation was used
//otherwise, you should order of PackageAssembly to determine the real encryption/archive type
private char encryptionType;
private char archiveType;
private int payloadHash;
private int headerHash; //initially headerHash is equal 0xFFFF
private char signTail;

public static @Nullable PackageHeader deserialize(byte[] header){
      if(header.length < MinBytesPerHeader)
	    return null;
      char signHeader = (char) ((header[0] << 8) | (header[1]));
      if(signHeader != SIGN_HEAD)
	    return null;
      //check id
      int id = (header[2] << 24) | (header[3] << 16) | (header[4] << 8) | (header[5]);
      int version = (header[6] << 24) | (header[7] << 16) | (header[8] << 8) | (header[9]);
      //dependencies
      List<Integer> dependencies = new ArrayList<>();
      int index = 10;
      int sign;
      do {
	    sign = (header[index] << 8) | (header[index + 1]);
	    if(sign ==  (short) SIGN_BODY){
		  index += 2;
		  break;
	    }
	    sign = (header[index] << 24) | (header[index + 1] << 16) | (header[index + 2] << 8) | (header[index + 3]);
	    index += 4;
	    dependencies.add(sign);
      }while(sign != (short) SIGN_BODY && index < 12 + dependencies.size() * 4);
      if(sign != (short) SIGN_BODY)
	    return null;
      //additional fields
      char encryption = (char)((header[index] << 8) | (header[index + 1]));
      index += 2;

      char archive = (char)((header[index] << 8) | (header[index + 1]));
      index += 2;
      //check payloadHash
      int payloadHash = (header[index] << 24) | (header[index + 1] << 16) | (header[index + 2] << 8) | (header[index + 3]);
      index += 4;
      int headerHash =  (header[index] << 24) | (header[index + 1] << 16) | (header[index + 2] << 8) | (header[index + 3]);
      //check tailSign
      index += 4;
      sign = (header[index] << 8) | (header[index + 1]);
      if(sign != (short) SIGN_TAIL)
	    return null;

      //initialization
      PackageHeader packageHeader = new PackageHeader(id, version);
      IntBuffer depBuffer = IntBuffer.allocate(dependencies.size());
      dependencies.forEach(depBuffer::put);
      packageHeader.setDependencies(depBuffer.array());
      packageHeader.setArchiveType(archive);
      packageHeader.setEncryptionType(encryption);
      packageHeader.setPayloadHash(payloadHash);
      return packageHeader;
}
}
