package transfer;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Serializer {
public static final int BYTES_PER_SIGN = 2;

public enum FieldType {
      Boolean, Byte, Short, Char, Int, Long, String, Array, Object;
      private short code;
      private Class<?> clazz = null;
      private int size = -1; //predefined size of value

      static {
	    Boolean.code = 1;
	    Byte.code = 1;
	    Short.code = 2;
	    Char.code = 2;
	    Int.code = 3;
	    Long.code = 4;
	    String.code = 5;
	    Array.code = 6;
	    Object.code = 7; //not used as independent value but only to start

	    Boolean.size = 1;
	    Byte.size = 1;
	    Short.size = 2;
	    Char.size = 2;
	    Int.size = 4;
	    Long.size = 8;

	    Boolean.clazz = java.lang.Boolean.class;
	    Byte.clazz = java.lang.Boolean.class;
	    Short.clazz = java.lang.Short.class;
	    Char.clazz = java.lang.Character.class;
	    Int.clazz = java.lang.Integer.class;
	    Long.clazz = java.lang.Long.class;
	    String.clazz = java.lang.String.class;
	    Array.clazz = java.lang.Object[].class; //the dummy class to determine that array is lang native class
      }

      public short code() {
	    return code;
      }

      /**
       * @return true if the type can return the predefined type
       */
      public boolean hasSize() {
	    return size != -1;
      }

      public boolean hasCode() {
	    return this != Object;
      }

      public Class<?> getJavaClass() {
	    return clazz;
      }

      /**
       * @return the size of field in bytes
       */
      public int size() {
	    return size;
      }
}

public Serializer() {
      MINIMAL_REG_CODE = FieldType.Object.code();//the initial value to start with
      codeMapper = new HashMap<>();
      fieldMapper = new HashMap<>();
      reverseMapper = new HashMap<>();
      for (FieldType field : FieldType.values()) {
	    if (field.getJavaClass() != null) { //each predefined class except object has known class
		  fieldMapper.put(field.getJavaClass(), field);
		  reverseMapper.putIfAbsent(field.code(), field.getJavaClass()); //predefined types can override self
	    }
      }
}

public boolean isValidCode(int code) {
      return code >= MINIMAL_REG_CODE && code <= Short.MAX_VALUE;
}

public Serializer register(int code, Class<?> clazz) {
      if (!isValidCode(code)) {
	    throw new IllegalArgumentException("The code is invalid");
      }
      codeMapper.put(clazz, (short) code);
      fieldMapper.put(clazz, FieldType.Object);
      reverseMapper.put((short) code, clazz);
      return this;
}

public <T> T construct(byte[] source, Class<T> origin) {
      return construct(ByteBuffer.wrap(source), origin);
}

/**
 * The Serializer should properly set (if source holds some Object types)
 */
private <T> T construct(ByteBuffer buffer, Class<T> origin) {
      T instance;
      try {
	    instance = origin.getDeclaredConstructor().newInstance();
      } catch (NoSuchMethodException e) {
	    throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
	    throw new RuntimeException(e);
      } catch (InstantiationException e) {
	    throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
	    throw new RuntimeException(e);
      }
      List<Field> fields = collectFields(origin);
      for (Field field : fields) {
	    short code = buffer.getShort();
	    Class<?> clazz = reverseMapper.get(code);
	    Object value;
	    if (clazz != null) {
		  FieldType type = fieldMapper.get(clazz);
		  if (type == null) {
			throw new IllegalStateException("Unknown field code " + code);
		  }
		  if (type.hasSize()) {
			value = constructPrimitive(buffer, type);
		  } else {
			value = constructComposite(buffer, clazz);
		  }
	    } else {
		  throw new IllegalStateException("The class by code " + code + " is not specified");
	    }
	    try {
		  field.setAccessible(true);
		  field.set(instance, value);
	    } catch (IllegalAccessException e) {
		  throw new RuntimeException(e);
	    }
      }
      return instance;
}

private @NotNull Object constructPrimitive(ByteBuffer wrapper, FieldType type) {
      assert wrapper.position() < wrapper.capacity();
      return switch (type) {
	    case Boolean -> wrapper.get() != 0;
	    case Byte -> wrapper.get();
	    case Short, Char -> wrapper.getShort();
	    case Int -> wrapper.getInt();
	    case Long -> wrapper.getLong();
	    default ->
		throw new IllegalArgumentException("Impossible construct primitive value from composite bytes " + type.getJavaClass());
      };
}

private @NotNull Object constructComposite(ByteBuffer wrapper, Class<?> clazz) {
      FieldType type = fieldMapper.get(clazz);
      assert wrapper.position() < wrapper.capacity() && type != null;
      return switch (type) {
	    case String -> {
		  int length = wrapper.getInt();
		  yield toString(wrapper.array(), wrapper.position(), length);
	    }
	    case Array -> constructArray(wrapper, clazz);
	    case Object -> construct(wrapper, clazz);
	    default ->
		throw new IllegalArgumentException("Impossible construct composite value from primitive bytes " + type.getJavaClass());
      };
}
private @NotNull Object constructArray(ByteBuffer wrapper, Class<?> array) {
      int length = wrapper.getInt();
      short code = wrapper.getShort();
      Class<?> element = array.getComponentType();
//      Map.Entry<FieldType, Short> fieldInfo = collectInfo(element);
//      FieldType type = fieldInfo.getKey();
//      assert fieldInfo.getValue() == code && fieldMapper.get(element) == type;
      Object instance = Array.newInstance(element, length);
      for (int i = 0; i < length; i++) {
	    Array.set(instance, i, construct(wrapper, element));
      }
      return instance;
}
/**
 * @param object -> the value that is supposed to be serialized. Commonly, there are two ways to serialize.
 *               If object was registered serialization append the type code.
 *               If object type is not specified then only fields will be serialized
 */
public byte[] serialize(Object object) {
      boolean isRegistered = fieldMapper.get(object.getClass()) != null;
      byte[] bytes;
      if (!isRegistered) {
	    bytes = serializeObject(object);
      } else {
	    bytes = serializeBatch(new Object[]{object});
      }
      return bytes;
}

private byte[] serializeBatch(Object[] objects) {
      List<byte[]> bytesBatch = new ArrayList<>();
      for (Object object : objects) {
	    ByteBuffer buffer;
	    Map.Entry<FieldType, Short> fieldInfo = collectInfo(object.getClass());
	    FieldType type = fieldInfo.getKey();
	    Short code = fieldInfo.getValue();
	    if (type.hasSize()) {
		  buffer = ByteBuffer.allocate(BYTES_PER_SIGN + type.size());
		  buffer.putShort(code).put(serializePrimitive(object, type));
	    } else {
		  byte[] bytes = serializeComposite(object, type);
		  buffer = ByteBuffer.allocate(BYTES_PER_SIGN + bytes.length);
		  buffer.putShort(code).put(bytes);
	    }
	    bytesBatch.add(buffer.array());
      }
      int dataSize = bytesBatch.stream().reduce(0, (last, buffer) -> last + buffer.length, Integer::sum);
      ByteBuffer hugeBuffer = ByteBuffer.allocate(dataSize);
      bytesBatch.forEach(hugeBuffer::put);
      return hugeBuffer.array();
}

private Map.Entry<FieldType, Short> collectInfo(Class<?> objClass) {
      short code;
      FieldType type;
      if (objClass.isArray()) {
	    type = FieldType.Array;
      } else {
	    type = fieldMapper.get(objClass);
      }
      if (type != null) {
	    if (type.hasCode()) {
		  code = type.code();
	    } else {
		  code = codeMapper.get(objClass);
	    }
      } else {
	    throw new IllegalStateException("Cannot serialize unknown class: " + objClass);
      }
      return Map.entry(type, code);
}

private byte[] serializeArray(Object[] objects, Class<?> element) {
      assert element != Object.class && objects.getClass().getComponentType() == element;
      Map.Entry<FieldType, Short> fieldInfo = collectInfo(element);
      FieldType type = fieldInfo.getKey();
      Short code = fieldInfo.getValue();
      ByteBuffer hugeBuffer;
      if (!type.hasSize()) {
	    List<byte[]> bytesBatch = new ArrayList<>();
	    for (Object object : objects) {
		  bytesBatch.add(serializeComposite(object, type));
	    }
	    int commonSize = bytesBatch.stream().reduce(0, (sum, buffer) -> sum + buffer.length, Integer::sum);
	    hugeBuffer = ByteBuffer.allocate(BYTES_PER_SIGN + commonSize);
	    hugeBuffer.putShort(code);
	    bytesBatch.forEach(hugeBuffer::put);
      } else {
	    hugeBuffer = ByteBuffer.allocate(BYTES_PER_SIGN + type.size() * objects.length);
	    hugeBuffer.putShort(code);
	    for (Object object : objects) {
		  hugeBuffer.put(serializePrimitive(object, type));
	    }
      }
      return hugeBuffer.array();

}

private byte[] serializeComposite(Object value, FieldType type) {
      ByteBuffer buffer;
      switch (type) {
	    case String -> {
		  String source = (String) value;
		  buffer = ByteBuffer.allocate(4 + source.length()); //the length of the string has restriction is 4 bytes
		  buffer.putInt(source.length()).put(toBytes(source));
	    }
	    case Array -> {
		  Object[] objects = multiplyArray(value);
		  Class<?> element = objects.getClass().getComponentType();
		  byte[] payload = serializeArray(objects, element);
		  buffer = ByteBuffer.allocate(4 + payload.length);
		  buffer.putInt(objects.length).put(payload);
	    }
	    case Object -> buffer = ByteBuffer.wrap(serializeObject(value));
	    default -> throw new IllegalArgumentException("The primitive type is passed");
      }
      return buffer.array();
}

private Object[] multiplyArray(@NotNull Object array) {
      assert array.getClass().isArray();
      final int length = Array.getLength(array);
      Object[] result = new Object[length];
      for (int i = 0; i < result.length; i++) {
	    result[i] = Array.get(array, i);
      }
      return result;
}

private byte[] serializeObject(@NotNull Object value) {
      Class<?> clazz = value.getClass();
      byte[] result;
      List<Field> fields = collectFields(clazz);
      List<Object> values = new ArrayList<>();
      for (Field field : fields) {
	    field.setAccessible(true);
	    try {
		  values.add(field.get(value));
	    } catch (IllegalAccessException e) {
		  throw new RuntimeException(e); //something incredible
	    }
      }
      result = serializeBatch(values.toArray(Object[]::new));
      return result;
}

private byte[] serializePrimitive(Object value, FieldType type) {
      ByteBuffer buffer = ByteBuffer.allocate(type.size());
      switch (type) {
	    case Boolean -> buffer.put((byte) ((boolean) value ? 1 : 0));
	    case Byte -> buffer.put((byte) value);
	    case Short, Char -> buffer.putShort((short) value);//short and char has same size
	    case Int -> buffer.putInt((int) value);
	    case Long -> buffer.putLong((long) value);
	    default -> throw new IllegalArgumentException("Non primitive value passed");
      }
      return buffer.array();
}

private List<Field> collectFields(@NotNull Class<?> origin) {
      return Arrays.stream(origin.getDeclaredFields())
		 .filter(field -> !Modifier.isTransient(field.getModifiers()))
		 .sorted(Comparator.comparing(Field::getName))
		 .toList();
}

public static @NotNull byte[] toBytes(String line) {
      return line.getBytes(StandardCharsets.US_ASCII);
}

public static @NotNull byte[] toBytes(Integer... values) {
      ByteBuffer buffer = ByteBuffer.allocate(values.length * 4);
      for (Integer value : values) {
	    buffer.putInt(value);
      }
      return buffer.array();
}

public static @NotNull String toString(@NotNull byte[] bytes) {
      CharBuffer buffer = CharBuffer.allocate(bytes.length);
      for (byte letter : bytes) {
	    buffer.put((char) letter);
      }
      return String.valueOf(buffer.array());
}

public static @NotNull String toString(@NotNull byte[] bytes, int offset, int length) {
      assert offset + length <= bytes.length;
      CharBuffer buffer = CharBuffer.allocate(length);
      for (int i = offset; i < offset + length; i++) {
	    buffer.put((char) bytes[i]);
      }
      return String.valueOf(buffer.array());
}

private final short MINIMAL_REG_CODE;
//Options for serialization
private final Map<Class<?>, FieldType> fieldMapper;
private final Map<Class<?>, Short> codeMapper; //the mapper is used only for Object field
//Options for deserialize byte input
private final Map<Short, Class<?>> reverseMapper; //return class by code values
}
