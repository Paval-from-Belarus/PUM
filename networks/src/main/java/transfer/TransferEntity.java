package transfer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TransferEntity {
/**
 * If this param is set then fields in <code>Entity</code> will be sorted according the order
 * promoted by <code>@TransferOrder</code> annotation
 */
boolean selective() default false;

/**
 * Fields with null values will not be passed to a byte stream. By this way, it's possible to save save bytes
 */
boolean ignoreNullable() default false;
}
