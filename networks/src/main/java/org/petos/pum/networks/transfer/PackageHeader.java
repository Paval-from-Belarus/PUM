package org.petos.pum.networks.transfer;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class PackageHeader {

public final static char SIGN_HEAD = 0xAA33;
public final static char SIGN_BODY = 0xFFFF;
public final static char SIGN_TAIL = 0xCC55;
public final static int EMPTY_HASH = -1;
public final static int BYTES_PER_HEADER_CNT = 26;

{
      signHead = SIGN_HEAD;
      signBody = SIGN_BODY;
      signTail = SIGN_TAIL;
      payloadHash = 0;
      headerHash = EMPTY_HASH;
}

public PackageHeader(int id, int version) {
      this.id = id;
      this.version = version;
}

public int getId() {
      return this.id;
}

public int getVersion() {
      return this.version;
}

public int getPayloadHash() {
      return payloadHash;
}

public void setPayloadHash(int hash) {
      this.payloadHash = hash;
}
public void setEncryptionType(char type) {
      encryptionType = type;
}

public void setArchiveType(char type) {
      archiveType = type;
}

public char getEncryption() {
      return encryptionType;
}

public char getArchive() {
      return archiveType;
}

public int size() {
      return BYTES_PER_HEADER_CNT;
}

public byte[] serialize() {
      ByteBuffer buffer = ByteBuffer.allocate(BYTES_PER_HEADER_CNT);
      buffer.putChar(signHead)
	  .putInt(id)
	  .putInt(version);
      buffer.putChar(signBody)
	  .putChar(encryptionType)
	  .putChar(archiveType)
	  .putInt(payloadHash)
	  .putInt(EMPTY_HASH)
	  .putChar(signTail);
      int headerHash = getControlSum(buffer.array());
      buffer.putInt(BYTES_PER_HEADER_CNT - 6, headerHash);
      return buffer.array();
}

//calculate header hash in assumption that headerHash is equal 0xFF...FF
//the method uses crc32 algo
static int getControlSum(byte[] payload) {
      long hash = -1;//set to all bits equals 1
      for (byte b : payload) {
	    hash ^= b;
	    for (int j = 0; j < 8; j++) {
		  if ((hash & 1L) != 0) {
			hash = (hash >> 1) ^ 0xEDB88320L;
		  } else {
			hash = (hash >> 1);
		  }
	    }

      }
      int result = (int) (hash ^ 0xFFFFFFFFL);
      return result;
}


final private char signHead;
//unique id of current package
final private int id;
//unique id of version of current package
final private int version;
//ids of all packages that used to resolve dependencies
//this field should be used to quick install additional dependencies
final private char signBody;
//the following two field are optional
//if they are set to zero ― no encryption/archivation was used
//otherwise, you should order of PackageAssembly to determine the real encryption/archive type
private char encryptionType;
private char archiveType;
private int payloadHash;
private int headerHash; //initially headerHash is equal 0xFFFF
private final char signTail;

public static @Nullable PackageHeader deserialize(byte[] header) {
      if (header.length < BYTES_PER_HEADER_CNT)
	    return null;
      ByteBuffer buffer = ByteBuffer.allocate(BYTES_PER_HEADER_CNT);
      buffer.put(header, 0, BYTES_PER_HEADER_CNT);
      buffer.position(0);
      char signHeader = buffer.getChar();
      if (signHeader != SIGN_HEAD)
	    return null;
      int id = buffer.getInt();
      int version = buffer.getInt();
      char signBody = buffer.getChar();
      if (signBody !=  SIGN_BODY){
	    return null;
      }
      char encryption = buffer.getChar();
      char archive = buffer.getChar();
      int payloadHash = buffer.getInt();
      int headerHash = buffer.getInt(); //to check the header integrity
      buffer.putInt(BYTES_PER_HEADER_CNT - 6, EMPTY_HASH);
      if (headerHash != getControlSum(buffer.array())){
	    return null;
      }
      char tailHash = buffer.getChar();
      if (tailHash != SIGN_TAIL){
	    return null;
      }
      //initialization
      PackageHeader packageHeader = new PackageHeader(id, version);
      packageHeader.setArchiveType(archive);
      packageHeader.setEncryptionType(encryption);
      packageHeader.setPayloadHash(payloadHash);
      return packageHeader;
}
}
