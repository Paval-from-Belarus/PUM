package transfer;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Some notes. The generalized classes, such as Number, cannot be interpreted explicitly.
 */
public class Serializer {
public static final int BYTES_PER_SIGN = 2;
public static final int FIRST_FREE_CODE = FieldType.Object.code() + 1;
public enum FieldType {
      Boolean, Byte, Short, Char, Int, Long, Float, Double, String, Array, Object;
      private short code;
      private Class<?> clazz = null;
      private int size = -1; //predefined size of value

      static {
	    Boolean.code = 0;
	    Byte.code = 1;
	    Short.code = 2;
	    Char.code = 2;
	    Int.code = 3;
	    Long.code = 4;
	    Float.code = 5;
	    Double.code = 6;
	    String.code = 7;
	    Array.code = 8;
	    Object.code = 9; //not used as independent value but only to start

	    Boolean.size = 1;
	    Byte.size = java.lang.Byte.BYTES;
	    Short.size = java.lang.Short.BYTES;
	    Char.size = java.lang.Character.BYTES;
	    Int.size = java.lang.Integer.BYTES;

	    Long.size = java.lang.Long.BYTES;
	    Float.size = java.lang.Float.BYTES;
	    Double.size = java.lang.Double.BYTES;

	    Boolean.clazz = java.lang.Boolean.class;
	    Byte.clazz = java.lang.Byte.class;
	    Short.clazz = java.lang.Short.class;
	    Char.clazz = java.lang.Character.class;
	    Int.clazz = java.lang.Integer.class;
	    Long.clazz = java.lang.Long.class;
	    Float.clazz = java.lang.Float.class;
	    Double.clazz = java.lang.Double.class;

	    String.clazz = java.lang.String.class;
	    Array.clazz = java.lang.Object[].class; //the dummy class to determine that array is lang native class
      }

      public static boolean isFreeCode(int code) {
	    return code < Boolean.code || code > Object.code;
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

public static class SerializeException extends IllegalStateException {
      public SerializeException(String msg) {
	    super(msg);
      }

      public SerializeException(Throwable t) {
	    super(t);
      }
}

public Serializer() {
      codeMapper = new HashMap<>(); //for external classes only
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
      return FieldType.isFreeCode(code);
}

public boolean isValidClass(Class<?> clazz) {
      return clazz != Object.class && clazz != Number.class;
}

public Serializer register(int code, Class<?> clazz) {
      if (!(isValidCode(code) && isValidClass(clazz))) {
	    throw new IllegalArgumentException("The code is invalid");
      }
      codeMapper.put(clazz, (short) code);
      fieldMapper.put(clazz, FieldType.Object);
      reverseMapper.put((short) code, clazz);
      return this;
}

public <T> T construct(byte[] source, Class<T> origin) {
      boolean isRegistered = fieldMapper.get(origin) != null;
      T result;
      if (!isRegistered) {
	    result = constructObject(ByteBuffer.wrap(source), origin);
      } else {
	    ByteBuffer buffer = ByteBuffer.wrap(source);
	    short code = buffer.getShort();
	    Class<?> clazz = reverseMapper.get(code);
	    if (clazz == origin) {
		  result = constructObject(buffer, origin);
	    } else {
		  throw new SerializeException("the source buffer holds incorrect values");
	    }
      }
      return result;
}

/**
 * The Serializer should properly set (if source holds some Object types)
 */
private <T> T constructObject(ByteBuffer buffer, Class<T> origin) {
      T instance;
      try {
	    Constructor<T> constructor = origin.getDeclaredConstructor();
	    constructor.setAccessible(true);
	    instance = constructor.newInstance();
      } catch (Exception e) {
	    throw new SerializeException(e);
      }
      List<Field> fields = collectFields(origin);
      for (Field field : fields) {
	    short code = buffer.getShort();
	    Class<?> clazz = reverseMapper.get(code);
	    FieldType type;
	    Object value;
	    if (clazz != null && (type = fieldMapper.get(clazz)) != null) {
		  if (type.hasSize()) {
			value = constructPrimitive(buffer, type);
		  } else {
			value = constructComposite(buffer, clazz);
		  }
	    } else {
		  throw new SerializeException("The class by code " + code + " is not specified");
	    }
	    try {
		  field.setAccessible(true);
		  field.set(instance, value);
	    } catch (IllegalAccessException e) {
		  throw new SerializeException(e);
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

private @NotNull Object constructComposite(ByteBuffer buffer, Class<?> clazz) {
      FieldType type;
      if (clazz.isArray()) {
	    type = fieldMapper.get(FieldType.Array.getJavaClass());
      } else {
	    type = fieldMapper.get(clazz);
      }
      assert buffer.position() < buffer.capacity() && type != null;
      return switch (type) {
	    case String -> constructString(buffer);
	    case Array ->
		constructArray(buffer); //the clazz is Object[].class. Dummy instance of class to simplify algo
	    case Object -> constructObject(buffer, clazz);
	    default ->
		throw new IllegalArgumentException("Impossible construct composite value from primitive bytes " + type.getJavaClass());
      };
}

private @NotNull Object constructString(ByteBuffer buffer) {
      final int length = buffer.getInt();
      final int offset = buffer.position();
      String result = toString(buffer.array(), offset, length);
      buffer.position(offset + length);
      return result;
}

private @NotNull Object constructArray(ByteBuffer buffer) {
      final int length = buffer.getInt();
      final short code = buffer.getShort(); //element code
      Class<?> element = reverseMapper.get(code);
      Object instance;
      if (element != null) {
	    FieldType type = fieldMapper.get(element);
	    assert type != null;
	    if (element.isArray()) { //the initial condition for multi-dimensional arrays
		  Map.Entry<Class<?>, int[]> dimensions = findArrayDimensions(buffer);
		  element = Array.newInstance(dimensions.getKey(), dimensions.getValue()).getClass();
	    }
	    instance = Array.newInstance(element, length);
	    for (int i = 0; i < length; i++) {
		  if (type.hasSize()) {
			Array.set(instance, i, constructPrimitive(buffer, type));
		  } else {
			Array.set(instance, i, constructComposite(buffer, element));
		  }
	    }
      } else {
	    throw new SerializeException("Unknown class with code " + code);
      }
      return instance;
}

/**
 * @return the piece class and dimensional count
 */
public Map.Entry<Class<?>, int[]> findArrayDimensions(final ByteBuffer buffer) {
      buffer.mark();
      Class<?> element = FieldType.Array.getJavaClass();
      int nestLevel = 0; //invocation of its function means at least two dimension
      while (element != null && element.isArray()) {
	    buffer.getInt(); //skip length bytes
	    short code = buffer.getShort();
	    element = reverseMapper.get(code);
	    nestLevel += 1;
      }
      if (element == null) {
	    throw new SerializeException("The bytes has unresolvable multi-dimensional array");
      }
      buffer.reset();
      return Map.entry(element, new int[nestLevel]);
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
	    bytes = serializeObject(object); //serialize and save the type
      } else {
	    bytes = serializeFields(new Object[]{object}); //pure serialization
      }
      return bytes;
}

/**
 * This method serialize the batch of any data (generally fields of certain object)
 */
private byte[] serializeFields(Object[] objects) {
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
	    throw new SerializeException("Cannot serialize unknown class: " + objClass);
      }
      return Map.entry(type, code);
}

private byte[] serializeArray(Object[] objects, Class<?> element) {
      ByteBuffer hugeBuffer;
      Map.Entry<FieldType, Short> fieldInfo = collectInfo(element);
      FieldType type = fieldInfo.getKey();
      Short code = fieldInfo.getValue();
      if (!type.hasSize()) {
	    List<byte[]> bytesBatch = new ArrayList<>();
	    for (Object object : objects) {
		  bytesBatch.add(serializeComposite(object, type));
	    }
	    int commonSize = bytesBatch.stream().reduce(0, (sum, buffer) -> sum + buffer.length, Integer::sum);
	    hugeBuffer = ByteBuffer.allocate(4 + BYTES_PER_SIGN + commonSize);
	    hugeBuffer.putInt(objects.length).putShort(code);
	    bytesBatch.forEach(hugeBuffer::put);
      } else {
	    hugeBuffer = ByteBuffer.allocate(4 + BYTES_PER_SIGN + type.size() * objects.length);
	    hugeBuffer.putInt(objects.length).putShort(code);
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
		  Class<?> element = mapElementType(value.getClass());
		  Object[] objects = multiplyArray(value); //interpret the array as array of objects
		  byte[] payload = serializeArray(objects, element);
		  buffer = ByteBuffer.wrap(payload);
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
private Class<?> mapElementType(@NotNull Class<?> array) {
      Class<?> element = array.getComponentType();
      if (fieldMapper.get(element) == null) {
	    System.out.println("catched");
      }
      return element;
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
      result = serializeFields(values.toArray(Object[]::new));
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
		 .filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
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
//Options for serialization
private final Map<Class<?>, FieldType> fieldMapper;
private final Map<Class<?>, Short> codeMapper; //the mapper is used only for Object field
//Options for deserialize byte input
private final Map<Short, Class<?>> reverseMapper; //return class by code values
}
