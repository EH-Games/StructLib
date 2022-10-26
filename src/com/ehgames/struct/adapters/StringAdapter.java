package com.ehgames.struct.adapters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface StringAdapter {
	BasicCharset charset() default BasicCharset.ASCII;
	/**
	 * If true, indicates that this string's length is denoted by a null-terminating character.
	 * Otherwise, it is determined using a preceeding length variable whose size can be set using the @LengthSize annotation
	 */
	boolean nullTerminated() default true;
	/**
	 * Maximum length of this string in characters.
	 * Zero and negative values are treated as no limit.
	 * Positive values less than minLength will be treated as minLength
	 */
	int maxLength() default -1;
	/**
	 * Minimum length of this string in characters.
	 * Negative values are treated as zero.
	 */
	int minLength() default 0;
	/**
	 * Alignment in bytes from the start of this string's characters to the start of the next variable.
	 * Zero and negative values will be treated as unaligned(1-byte alignment)
	 */
	int align() default 1;
}
