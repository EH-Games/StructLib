package com.ehgames.struct;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;

import com.ehgames.struct.adapters.LengthSize;

final class StructUtils {

	static <T> T createObj(Class<T> cls) {
		Constructor<T> defaultConstructor = null;
		try {
			defaultConstructor = cls.getDeclaredConstructor();
			defaultConstructor.setAccessible(true);
			
			return defaultConstructor.newInstance();
		} catch(ReflectiveOperationException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(defaultConstructor != null) {
				defaultConstructor.setAccessible(false);
			}
		}
	}

	static int getSizeType(Field f) {
		// first time I've ever used Optional. It's not ?. but it's tolerable -EH (6/2/22)
		int value = Optional.ofNullable(f).map(fd -> fd.getAnnotation(LengthSize.class)).map(a -> a.value()).orElse(4);
		if(value != 1 && value != 2 && value != 4) {
			value = 4;
		}
		return value;
	}
	
	private StructUtils() {}
}
