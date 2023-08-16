package org.petos.pum.server.network;

import transfer.NetworkExchange;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Paval Shlyk
 * @since 15/08/2023
 */
public class RequestHeaders {
/**
 * Each packet has specific type (depends on either Request or Response Packet)
 * By legacy issues, each type is <code>NetworkExchange.RequestType</code> or
 * <code>NetworkExchange.ResponseType</code>
 *
 * @see NetworkExchange
 */
public static final String PACKET_TYPE = "PACKET_TYPE";
//The NetworkHeaders layout in NetworkPacket <code>code</code> byte
//^---------------------27------------------------^ //because least 5 bits are reserved
//|                                               |
//+---3---+---3---+-------------------------------+
//|       |       |                               |
//+--+----+---+---+-------------------------------+
//   |        |
//   |        |
//   |        |   TransferFormat
//   |        +---->
//   | ResponseFormat
//   +--->

/**
 * Determine the format in which other side (server or client) want to achieve response
 */
public enum ResponseFormat {
      Default, //Only status -> without any additional info
      Compact, //short info format
      Verbose; //verbose info format
      private static final int CODE_MASK = 0b11;
      private static final int CODE_SHIFT = 0;

      public static ResponseFormat[] valuesOf(int code) {
	    return RequestHeaders
		       .valuesOf(code, CODE_SHIFT, CODE_MASK, ResponseFormat.values())
		       .toArray(ResponseFormat[]::new);
      }
}

/**
 * Specify ways by which additional info
 */
public enum TransferFormat {
      Default, //no payload
      Attached, //packet has attached payload -> a bit verbose, because NetworkPacket already know
      Polled; //packet has polled payload
      private static final int CODE_MASK = 0b11;
      private static final int CODE_SHIFT = 3;

      public static TransferFormat[] valuesOf(int code) {
	    return RequestHeaders
		       .valuesOf(code, CODE_SHIFT, CODE_MASK, TransferFormat.values())
		       .toArray(TransferFormat[]::new);
      }
}

public static final String RESPONSE_FORMAT = "RESPONSE_FORMAT";
public static final String TRANSFER_FORMAT = "TRANSFER_FORMAT";

//By conventional rule, first member of enum type should be default enum
private static <T extends Enum<T>> List<T> valuesOf(int code, int shift, int mask, T[] values) {
      int flagsCnt = Math.min((code >> shift) & mask, values.length);
      return new ArrayList<>(Arrays.asList(values).subList(0, flagsCnt));
}
}
