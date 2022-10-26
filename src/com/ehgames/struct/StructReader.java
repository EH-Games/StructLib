package com.ehgames.struct;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.ehgames.struct.adapters.BasicCharset;
import com.ehgames.struct.adapters.StringAdapter;
import com.ehgames.struct.adapters.StructAdapter;
import com.ehgames.struct.adapters.StructTypeAdapter;

public class StructReader {
	// could maybe do nested classes using isMemberClass
	
	@SuppressWarnings("unchecked")
	public static <T> void read(ByteBuffer data, T obj) {
		Class<T> cls = (Class<T>) obj.getClass();
		read(data, cls, obj);
	}
	
	public static <T> T read(ByteBuffer data, Class<T> cls) {
		T obj = StructUtils.createObj(cls);
		if(obj != null) {
			read(data, cls, obj);
		}
		return obj;
	}
	
	public static <T> void read(ByteBuffer data, Class<? extends T> cls, T obj) {
		if(cls.isArray()) {
			readArray(data, cls.getComponentType(), null, obj);
			return;
		}
		
		readNonArrayContainer(data, cls, obj);
	}
	
	private static <T> void readNonArrayContainer(ByteBuffer data, Class<? extends T> cls, T obj) {
		Class<?> superclass = cls.getSuperclass();
		if(superclass != null & superclass != Object.class) {
			readNonArrayContainer(data, superclass, obj);
		}
			
		Field[] fields = cls.getDeclaredFields();
		try {
			for(Field f : fields) {
				readSingleField(data, f, obj);
			}
		} catch(ReflectiveOperationException roe) {
			roe.printStackTrace();
		}
	}
	
	private static void readSingleField(ByteBuffer data, Field f, Object obj) throws ReflectiveOperationException {
		int modifiers = f.getModifiers();
		if(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
			return;
		}
		
		f.setAccessible(true);
		try {
			StructAdapter annotation = f.getAnnotation(StructAdapter.class);
			if(annotation != null) {
				StructTypeAdapter<?> adapter = StructUtils.createObj(annotation.value());
				Object value = adapter.read(data, obj);
				f.set(obj, value);
			} else {
				Class<?> type = f.getType();
				if(type.isPrimitive()) {
					Object val = readPrimitive(data, type);
					f.set(obj, val);
					
				} else if(type.isArray()) {
					readArray(data, type, obj, f);
					
				} else if(type.isEnum()) {
					readEnum(data, type, obj, f);
					
				} else if(type == String.class) {
					readString(data, obj, f);
					
				} else {
					Object value = read(data, type);
					f.set(obj, value);
				}
			}
		} finally {
			f.setAccessible(false);
		}
	}
	
	private static int getSizedField(ByteBuffer data, Field f) {
		int size = StructUtils.getSizeType(f);
		if(size == 1) return data.get() & 0xFF;
		if(size == 2) return data.getShort() & 0xFFFF;
		return data.getInt();
	}
	
	private static void readEnum(ByteBuffer data, Class<?> type, Object obj, Field f) throws ReflectiveOperationException {
		int value = getSizedField(data, f);
		Object[] enumVals = type.getEnumConstants();
		if(value < 0 || value >= enumVals.length) {
			System.err.println("Value of " + value + " is out of range for enum " + type.getName());
		} else {
			f.set(obj, enumVals[value]);
		}
	}
	
	private static void readString(ByteBuffer data, Object obj, Field f) throws ReflectiveOperationException {
		String str = readString(data, f);
		f.set(obj, str);
	}
	
	private static String readString(ByteBuffer data, Field f) {
		// collect the variables
		BasicCharset bchar = BasicCharset.ASCII;
		int minLength = 0;
		int maxLength = -1;
		int align = 1;
		boolean nullTerminated = false;
		if(f != null) {
			StringAdapter adapter = f.getAnnotation(StringAdapter.class);
			if(adapter != null) {
				bchar = adapter.charset();
				minLength = adapter.minLength();
				maxLength = adapter.maxLength();
				align = adapter.align();
				nullTerminated = adapter.nullTerminated();
			}
		}	
		
		// read the string
		String str;
		int start = data.position();
		if(nullTerminated) {
			// variable validaton is performed in the helper method
			str = StringUtils.readNullTerminatedString(data, bchar, minLength, maxLength);
		} else {
			// validate variables
			boolean utf16 = bchar == BasicCharset.UTF16;
			if(minLength < 0) {
				minLength = 0;
			}
			if(maxLength < minLength) {
				maxLength = minLength;
			}
			if(maxLength <= 0) {
				maxLength = Integer.MAX_VALUE;
			}
			
			int length = getSizedField(data, f);
			Charset charset = utf16 ? null : bchar == BasicCharset.ASCII ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8;
			
			if(utf16) {
				char[] buf = new char[length];
				data.asCharBuffer().get(buf);
				// we need to make sure position is accurate
				// at least for reading the next variable, but also for alignment
				data.position(start + length * Character.BYTES);
				str = new String(buf);
			} else {
				byte[] buf = new byte[length];
				data.get(buf);
				str = new String(buf, charset);
			}
		}
		
		// handle alignment
		int end = data.position();
		int bytesRead = end - start;
		if(align < 1) {
			align = 1;
		}
		int mod = bytesRead % align;
		if(mod != 0) {
			data.position(end + (align - mod));
		}
		
		return str;
	}
	
	private static Object readPrimitive(ByteBuffer data, Class<?> type) {
		if(type == Integer.TYPE) return data.getInt();
		if(type == Long.TYPE) return data.getLong();
		if(type == Short.TYPE) return data.getShort();
		if(type == Byte.TYPE) return data.get(); 
		if(type == Boolean.TYPE) return data.get() != 0;
		if(type == Float.TYPE) return data.getFloat();
		if(type == Double.TYPE) return data.getDouble();
		if(type == Character.TYPE) return data.getChar();
		return null;
	}
	
	private static void readArray(ByteBuffer data, Class<?> type, Object container, Object array) {
		int length = Array.getLength(array);
		
		// FIXME this will almost certainly break with arrays of anything other than primitives and basic objects
		if(type.isPrimitive()) {
			for(int i = 0; i < length; i++) {
				Object arrayElem = readPrimitive(data, type);
				Array.set(array, i, arrayElem);
			}
		} else {
			for(int i = 0; i < length; i++) {
				Object arrayElem = read(data, type);
				Array.set(array, i, arrayElem);
			}
		}
	}
	
	private static void readArray(ByteBuffer data, Class<?> type, Object obj, Field f) throws ReflectiveOperationException {
		Object array;
		Class<?> elemClass = type.getComponentType();
			
		if(Modifier.isFinal(f.getModifiers())) {
			array = f.get(obj);
		} else {
			int length = getSizedField(data, f);
			array = Array.newInstance(elemClass, length);
			f.set(obj, array);
		}
		
		readArray(data, elemClass, obj, array);
	}
}