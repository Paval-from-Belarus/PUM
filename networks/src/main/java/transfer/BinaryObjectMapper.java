package transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

/**
 * Some notes. The generalized classes, such as Number, cannot be interpreted explicitly.
 */
public class BinaryObjectMapper {
public static final int BYTES_PER_SIGN = 2;
public static final int FIRST_FREE_CODE = FieldType.Object.code() + 1;
public static final byte[] CODE_WRAPPER = new byte[2];//the array, that can be used to store

public enum FieldType {
      Boolean, Byte, Short, Char, Int, Long, Float, Double, String, Array, Object;
      private short code;
      private Class<?> clazz = null;
      private int size = -1; //predefined size of value
      private static final short NULL_CODE = (short) (1 << 15);

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
	    Object.clazz = java.lang.Object.class;
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

      public Optional<Class<?>> getPrimitiveClass() {
	    Class<?> clazz = switch (this) {
		  case Boolean -> java.lang.Boolean.TYPE;
		  case Byte -> java.lang.Byte.TYPE;
		  case Short -> java.lang.Short.TYPE;
		  case Char -> java.lang.Character.TYPE;
		  case Int -> java.lang.Integer.TYPE;
		  case Long -> java.lang.Long.TYPE;
		  case Float -> java.lang.Float.TYPE;
		  case Double -> java.lang.Double.TYPE;
		  case String, Array, Object -> null;
	    };
	    return Optional.ofNullable(clazz);
      }

      public static short nullCode(short code) {
	    return (short) (code | NULL_CODE);
      }

      public static boolean isNullCode(short code) {
	    return (code | NULL_CODE) == code;
      }

      public static short cleanCode(short code) {
	    return (short) (code & ~NULL_CODE);
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

public BinaryObjectMapper() {
      codeMapper = new HashMap<>(); //for external classes only
      fieldMapper = new HashMap<>();
      reverseMapper = new HashMap<>();
      for (FieldType field : FieldType.values()) {
	    if (field != FieldType.Object) { //each predefined class except object has known class
		  fieldMapper.put(field.getJavaClass(), field);
		  field.getPrimitiveClass().ifPresent(clazz -> fieldMapper.put(clazz, field));//put primitive class-type to successfully serialize data
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

public BinaryObjectMapper register(int code, Class<?> clazz) {
      if (!(isValidCode(code) && isValidClass(clazz))) {
	    throw new IllegalArgumentException("A code or a class are invalid");
      }
      boolean isFree = false;
      boolean canUpdate = codeMapper.containsKey(clazz) && reverseMapper.containsKey((short) code);
      if (!canUpdate) {
	    isFree = !codeMapper.containsKey(clazz) && !reverseMapper.containsKey((short) code);
      }
      if (canUpdate || isFree) {
	    codeMapper.put(clazz, (short) code);
	    fieldMapper.put(clazz, FieldType.Object);
	    reverseMapper.put((short) code, clazz);
      }
      return this;
}

public <T> T construct(byte[] source, Class<T> origin) {
      ByteBuffer buffer = ByteBuffer.wrap(source).mark();
      T instance;
      if (origin.isArray() && buffer.getShort() == FieldType.Array.code()) {
	     instance = origin.cast(constructArray(buffer, origin));
      } else {
	    instance = constructObject(buffer, origin);
      }
      return instance;
}

@SuppressWarnings("unchecked")
private <T> T createInstance(Class<T> origin) throws Exception {
      T instance;
      Optional<Constructor<?>> constructor = Arrays.stream(origin.getDeclaredConstructors()).filter(c -> c.getParameterCount() == 0).findAny();
      if (constructor.isPresent()) {
	    constructor.get().setAccessible(true);
	    instance = (T) constructor.get().newInstance();
      } else {
	    Field f = Unsafe.class.getDeclaredField("theUnsafe");
	    f.setAccessible(true);
	    Unsafe unsafe = (Unsafe) f.get(null);
	    instance = (T) unsafe.allocateInstance(origin);
      }
      return instance;
}

//check that field by specific type can be placed on origin place
//the main purpose of this method is to support the skipping nullable field
private boolean areCompatible(Class<?> origin, FieldType type) {
      return switch (type) {
	    case Array -> origin.isArray();
	    case Object -> !origin.isPrimitive();
	    case String -> origin == type.getJavaClass(); //String has sealed class
	    case Byte, Boolean, Short, Char, Int, Long, Float, Double ->
		origin.isPrimitive() || origin.isAssignableFrom(type.getJavaClass());
      };
}

/**
 * The Serializer should properly set (if source holds some Object types)
 */
private <T> T constructObject(ByteBuffer buffer, Class<T> origin) {
      T instance;
      try {
	    instance = createInstance(origin);
      } catch (Exception e) {
	    throw new SerializeException(e);
      }
      if (SimpleTransfer.class.isAssignableFrom(origin)) {
	    int length = buffer.getInt();
	    byte[] bytes = new byte[length];
	    System.arraycopy(buffer.array(), buffer.position(), bytes, 0, length);
	    instance = ((SimpleTransfer<T>) instance).deserialize(bytes);
	    buffer.position(buffer.position() + bytes.length);
      } else {
	    completeObjectByFields(buffer, instance);
      }
      return instance;
}

private <T> void completeObjectByFields(ByteBuffer buffer, T instance) {
      List<Field> fields = collectFields(instance.getClass());
      for (Field field : fields) {
	    FieldType type;
	    Object value = Optional.empty();
	    buffer.mark();
	    short code = buffer.getShort();
	    if (FieldType.isNullCode(code)) {
		  value = null;
		  code = FieldType.cleanCode(code);//the code is not nullable more
	    }
	    Class<?> clazz = reverseMapper.get(code);
	    if (clazz != null && (type = fieldMapper.get(clazz)) != null) {
		  if (!areCompatible(field.getType(), type)) {
			buffer.reset();
			continue;
		  }
		  if (type == FieldType.Array) {
			clazz = field.getType(); //construct an array according the field requirements
		  }
		  if (value != null) {
			if (type.hasSize()) {
			      value = constructPrimitive(buffer, type);
			} else {
			      value = constructComposite(buffer, clazz);
			}
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
	    if (buffer.position() == buffer.capacity()) {
		  break;
	    }
      }
}

private @NotNull Object constructPrimitive(ByteBuffer wrapper, FieldType type) {
      assert wrapper.position() < wrapper.capacity();
      return switch (type) {
	    case Boolean -> wrapper.get() != 0;
	    case Byte -> wrapper.get();
	    case Short, Char -> wrapper.getShort();
	    case Int -> wrapper.getInt();
	    case Long -> wrapper.getLong();
	    case Float -> wrapper.getFloat();
	    case Double -> wrapper.getDouble();
	    case String, Array, Object ->
		throw new IllegalArgumentException("Impossible construct primitive value from composite bytes " + type.getJavaClass());
      };
}

private @NotNull Object constructComposite(ByteBuffer buffer, Class<?> clazz) {
      FieldType type;
      if (clazz.isArray()) {
	    type = FieldType.Array;
      } else {
	    type = fieldMapper.get(clazz);
      }
      assert buffer.position() < buffer.capacity() && type != null;
      return switch (type) {
	    case String -> constructString(buffer);
	    case Array ->
		constructArray(buffer, clazz); //the clazz is Object[].class. Dummy instance of class to simplify algo
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

private @NotNull Object constructArray(ByteBuffer buffer, Class<?> localArray) {
      final int length = buffer.getInt();
      final short code = buffer.getShort();
      Class<?> element = reverseMapper.get(FieldType.cleanCode(code));
      Object instance;
      if (element != null) {
	    FieldType type = fieldMapper.get(element);
	    assert type != null;
	    element = localArray.getComponentType(); //the tricky way to accept data according local data format
	    if (FieldType.isNullCode(code)) {
		  instance = constructNullableArray(buffer, element, type, length);
	    } else {
		  instance = constructPureArray(buffer, element, type, length);
	    }
      } else {
	    throw new SerializeException("Unknown class with code " + code);
      }
      return instance;
}

private @NotNull Object constructPureArray(ByteBuffer buffer, Class<?> element, FieldType type, int length) {
      Object instance = Array.newInstance(element, length);
      for (int i = 0; i < length; i++) {
	    if (type.hasSize()) {
		  Array.set(instance, i, constructPrimitive(buffer, type));
	    } else {
		  Array.set(instance, i, constructComposite(buffer, element));
	    }
      }
      return instance;
}

private @NotNull Object constructNullableArray(ByteBuffer buffer, Class<?> element, FieldType type, int length) {
      Object instance = Array.newInstance(element, length);
      for (int i = 0; i < length; i++) {
	    short code = buffer.getShort();
	    Object value = null;
	    if (!FieldType.isNullCode(code)) {
		  if (type.hasSize()) {
			value = constructPrimitive(buffer, type);
		  } else {
			value = constructComposite(buffer, element);
		  }
	    }
	    Array.set(instance, i, value);
      }
      return instance;
}

/**
 * @param object -> the value that is supposed to be serialized. Commonly, there are two ways to serialize.
 *               If object was registered serialization append the type code.
 *               If object type is not specified then only fields will be serialized
 */
public byte[] serialize(@NotNull Object object) {
      byte[] bytes;
      if (object.getClass().isArray()) {
	    Object[] elements = multiplyArray(object);
	    bytes = serializeArray(elements, object.getClass().getComponentType());
	    bytes = ByteBuffer.allocate(BYTES_PER_SIGN + bytes.length).putShort(FieldType.Array.code()).put(bytes).array();
      } else {
	    bytes = serializeObject(object);
      }
      return bytes;
}

/**
 * This method serialize the batch of any data (generally fields of certain object)
 */
private byte[] serializeFields(List<Pair<Class<?>, Object>> fields) {
      List<byte[]> bytesBatch = new ArrayList<>();
      for (var field : fields) {
	    ByteBuffer buffer;
	    Map.Entry<FieldType, Short> fieldInfo;
	    if (field.getRight() != null) {
		  fieldInfo = collectInfo(field.getRight().getClass());
	    } else {
		  fieldInfo = collectInfo(field.getLeft());//if impossible to access value -> let's access the field type
	    }
	    FieldType type = fieldInfo.getKey();
	    Short code = fieldInfo.getValue();
	    if (field.getRight() != null) {
		  if (type.hasSize()) {
			buffer = ByteBuffer.allocate(BYTES_PER_SIGN + type.size());
			buffer.putShort(code).put(serializePrimitive(field.getRight(), type));
		  } else {
			byte[] bytes = serializeComposite(field.getRight(), type);
			buffer = ByteBuffer.allocate(BYTES_PER_SIGN + bytes.length);
			buffer.putShort(code).put(bytes);
		  }
	    } else {
		  buffer = ByteBuffer.allocate(BYTES_PER_SIGN).putShort(FieldType.nullCode(type.code()));
	    }
	    bytesBatch.add(buffer.array());
      }
      int dataSize = bytesBatch.stream().reduce(0, (last, buffer) -> last + buffer.length, Integer::sum);
      ByteBuffer hugeBuffer = ByteBuffer.allocate(dataSize);
      bytesBatch.forEach(hugeBuffer::put);
      return hugeBuffer.array();
}

/**
 * Collect all known information about class and convert to available form
 */
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

private byte[] serializeNullableArray(Object[] objects, FieldType type, Short code) {
      ByteBuffer hugeBuffer;
      final short NULLABLE_CODE = FieldType.nullCode(code);
      List<byte[]> bytesBatch = new ArrayList<>();
      for (@Nullable Object object : objects) {
	    short elementCode = code;
	    if (object == null) {
		  elementCode = NULLABLE_CODE;
	    }
	    bytesBatch.add(ByteBuffer.allocate(BYTES_PER_SIGN).putShort(elementCode).array());
	    if (object != null) {
		  if (type.hasSize()) { //the cycle will invert
			bytesBatch.add(serializePrimitive(object, type));
		  } else {
			bytesBatch.add(serializeComposite(object, type));
		  }
	    }
      }
      int commonSize = bytesBatch.stream().reduce(0, (sum, buffer) -> sum + buffer.length, Integer::sum);
      hugeBuffer = ByteBuffer.allocate(4 + BYTES_PER_SIGN + commonSize); //two types ????
      hugeBuffer.putInt(objects.length).putShort(NULLABLE_CODE);
      bytesBatch.forEach(hugeBuffer::put);
      return hugeBuffer.array();
}

private byte[] serializePureArray(Object[] objects, FieldType type, Short code) {
      ByteBuffer hugeBuffer;
      if (!type.hasSize()) {
	    List<byte[]> bytesBatch = new ArrayList<>();
	    for (@NotNull Object object : objects) {
		  bytesBatch.add(serializeComposite(object, type));
	    }
	    int commonSize = bytesBatch.stream().reduce(0, (sum, buffer) -> sum + buffer.length, Integer::sum);
	    hugeBuffer = ByteBuffer.allocate(4 + BYTES_PER_SIGN + commonSize);
	    hugeBuffer.putInt(objects.length).putShort(code);
	    bytesBatch.forEach(hugeBuffer::put);
      } else {
	    hugeBuffer = ByteBuffer.allocate(4 + BYTES_PER_SIGN + type.size() * objects.length); //let's make the max performance
	    hugeBuffer.putInt(objects.length).putShort(code);
	    for (@NotNull Object object : objects) {
		  hugeBuffer.put(serializePrimitive(object, type));
	    }
      }
      return hugeBuffer.array();
}

private byte[] serializeArray(Object[] objects, Class<?> element) {
      byte[] result;
      Map.Entry<FieldType, Short> fieldInfo = collectInfo(element);
      FieldType type = fieldInfo.getKey();
      Short code = fieldInfo.getValue();
      boolean hasNullables = !this.skipNullables && Arrays.stream(objects).anyMatch(Objects::isNull);
      if (hasNullables) {
	    result = serializeNullableArray(objects, type, code);
      } else {
	    result = serializePureArray(objects, type, code);
      }
      return result;
}

private byte[] serializeComposite(@NotNull Object value, FieldType type) {
      ByteBuffer buffer;
      switch (type) {
	    case String -> {
		  String source = (String) value;
		  buffer = ByteBuffer.allocate(4 + source.length()); //the length of the string has restriction is 4 bytes
		  buffer.putInt(source.length()).put(toBytes(source));
	    }
	    case Array -> {
		  Class<?> element = value.getClass().getComponentType();
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


private byte[] serializeObject(@NotNull Object value) {
      Class<?> clazz = value.getClass();
      byte[] result;
      if (!SimpleTransfer.class.isAssignableFrom(clazz)) {
	    List<Field> fields = collectFields(clazz, value);
	    List<Pair<Class<?>, Object>> entries = new ArrayList<>();
	    for (Field field : fields) {
		  field.setAccessible(true);
		  try {
			entries.add(Pair.of(field.getType(), field.get(value)));
		  } catch (IllegalAccessException e) {
			throw new RuntimeException(e); //something incredible
		  }
	    }
	    result = serializeFields(entries);
      } else {
	    byte[] bytes = ((SimpleTransfer<?>) value).serialize();
	    ByteBuffer buffer = ByteBuffer.allocate(4 + bytes.length);
	    buffer.putInt(bytes.length).put(bytes);
	    result = buffer.array();
      }
      return result;
}

private byte[] serializePrimitive(@NotNull Object value, FieldType type) {
      ByteBuffer buffer = ByteBuffer.allocate(type.size());
      switch (type) {
	    case Boolean -> buffer.put((byte) ((boolean) value ? 1 : 0));
	    case Byte -> buffer.put((byte) value);
	    case Short, Char -> buffer.putShort((short) value);//short and char has same size
	    case Int -> buffer.putInt((int) value);
	    case Long -> buffer.putLong((long) value);
	    case Float -> buffer.putFloat((float) value);
	    case Double -> buffer.putDouble((double) value);
	    case String, Array, Object -> throw new IllegalArgumentException("Non primitive value passed");
      }
      return buffer.array();
}

private List<Field> collectFields(@NotNull Class<?> origin) {
      TransferEntity annotation = origin.getAnnotation(TransferEntity.class);
      Stream<Field> fields = Arrays.stream(origin.getDeclaredFields())
				 .filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
				 .sorted(Comparator.comparing(Field::getName)); //to be more precise and definite
      if (annotation != null && annotation.selective()) {
	    fields = fields.sorted(this::sortSelective);
      }
      return fields.toList();
}

private List<Field> collectFields(@NotNull Class<?> origin, Object instance) {
      List<Field> fields = collectFields(origin); //all declared fields
      TransferEntity annotation = origin.getAnnotation(TransferEntity.class);
      skipNullables = false;//as default value
      if (annotation != null && annotation.ignoreNullable()) {
	    fields = fields.stream().filter(field -> filterNullable(field, instance)).toList();
	    skipNullables = true;//globals property to check that no nullables
      }
      return fields;
}

private int sortSelective(Field left, Field right) {
      TransferOrder leftOrder = left.getAnnotation(TransferOrder.class);
      TransferOrder rightOrder = right.getAnnotation(TransferOrder.class);
      int result = 0; //as default, all fields are equals
      if (leftOrder != null && rightOrder != null) {
	    result = leftOrder.value() - rightOrder.value();
      }
      if (leftOrder != null || rightOrder != null) { //the annotated fields has higher priority
	    result = leftOrder != null ? -1 : 1;
      }
      return result;
}

private boolean filterNullable(Field field, Object instance) {
      Object result = 0; //
      try {
	    field.setAccessible(true);
	    if (!field.getType().isPrimitive())
		  result = field.get(instance);
      } catch (Exception e) {
	    throw new IllegalStateException("Field is not accessible");
      }
      return result != null; //not null values will be passed
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

private boolean skipNullables;
//Options for serialization
private final Map<Class<?>, FieldType> fieldMapper;
private final Map<Class<?>, Short> codeMapper; //the mapper is used only for Object field
//Options for deserialize byte input
private final Map<Short, Class<?>> reverseMapper; //return class by code values
}
