package com.ehgames.struct.adapters;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotiation specifying the size of array lengths or enum values
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface LengthSize {
	/**
	 * should be 1, 2, or 4. any other value will be interpreted as 4
	 */
	int value();
}
