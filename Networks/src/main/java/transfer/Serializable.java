package transfer;

public interface Serializable<T> {
      byte[] serialize();
      T valueOf(byte[] bytes);
}
