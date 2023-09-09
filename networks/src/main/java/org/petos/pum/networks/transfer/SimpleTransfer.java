package org.petos.pum.networks.transfer;

/**
 * The main purpose of this class ― introduce embeddable entities for more complex transfer entities
 */
public interface SimpleTransfer<T> {
//construct from self instance bytes to transfer
byte[] serialize();

/**
 * Accept bytes to return to viable state
 * this method will be invoked exactly after construction. The main requirement for SimpleTransfer is a existence of dedicated functionality to construct.
 * @return constructed element (probably self but aligned)
 */
T deserialize(byte[] bytes);
}
