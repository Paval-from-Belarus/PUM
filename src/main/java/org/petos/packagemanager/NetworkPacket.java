package org.petos.packagemanager;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

public class NetworkPacket {
private final static int CMD_SIGN = 0xAACC7799;
public static int BytesPerCommand = 8;

public enum CommandType {Decline, Approve, GetAll, GetName, GetId, GetVersion, Publish, Unknown}

private String data;
private CommandType type;
public NetworkPacket(CommandType type){
      this.type = type;
      data = "";
}
public NetworkPacket(CommandType type, String data){
      this.type = type;
      this.data = data;
}
public void setData(String data){
      this.data = data;
}
public String data(){
      return this.data;
}
public byte[] code(){
      return NetworkPacket.getCommand(this.type);
}
@NotNull
public static byte[] getCommand(CommandType type) {
      ByteBuffer buffer = ByteBuffer.allocate(BytesPerCommand);
      buffer.putInt(CMD_SIGN);
      int commandSign = type.ordinal();
      buffer.putInt(4, commandSign);
      return buffer.array();
}
public static CommandType convertCommand(byte[] command){
      int code = 0;
      for(int i = 2; i < command.length; i++)
	    code = (code << 8) | command[i];
      int ordNum = code;
      Optional<CommandType> result = Arrays.stream(CommandType.values())
	  .filter(type -> type.ordinal() == ordNum)
	  .findAny();
      return result.orElse(CommandType.Unknown);
}
}
