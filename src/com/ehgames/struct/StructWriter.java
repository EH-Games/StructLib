package com.ehgames.struct;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.ehgames.struct.adapters.BasicCharset;
import com.ehgames.struct.adapters.StringAdapter;
import com.ehgames.struct.adapters.StructAdapter;
import com.ehgames.struct.adapters.StructTypeAdapter;

public class StructWriter {
	@SuppressWarnings("unchecked")
	public static <T> void write(ByteBuffer data, T obj) {
		Class<T> cls = (Class<T>) obj.getClass();
		write(data, cls, obj);
	}
	
	public static <T> void write(ByteBuffer data, Class<? extends T> cls, T obj) {
		if(cls.isArray()) {
			writeArray(data, obj, cls, null);
		} else {
			writeNonArrayContainer(data, cls, obj);
		}
	}

	private static <T> void writeNonArrayContainer(ByteBuffer data, Class<? extends T> cls, T obj) {
		Class<?> superclass = cls.getSuperclass();
		if(superclass != null & superclass != Object.class) {
			writeNonArrayContainer(data, superclass, obj);
		}
			
		Field[] fields = cls.getDeclaredFields();
		try {
			for(Field f : fields) {
				writeSingleField(data, f, obj);
			}
		} catch(ReflectiveOperationException roe) {
			roe.printStackTrace();
		}
	}
	
	private static void writeSingleField(ByteBuffer data, Field f, Object obj) throws ReflectiveOperationException {
		int modifiers = f.getModifiers();
		if(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
			return;
		}
		
		f.setAccessible(true);
		try {
			Object value = f.get(obj);
			
			StructAdapter annotation = f.getAnnotation(StructAdapter.class);
			if(annotation != null) {
				writeWithAdapter(data, annotation, value, obj);
			} else {
				Class<?> type = f.getType();
				if(type.isPrimitive()) {
					writePrimitive(data, value, type);
					
				} else if(type.isArray()) {
					writeArray(data, value, type, f);
					
				} else if(type.isEnum()) {
					writeEnum(data, value, type, f);
					
				} else if(type == String.class) {
					writeString(data, value.toString(), f);
					
				} else {
					write(data, type, value);
				}
			}
		} finally {
			f.setAccessible(false);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T> void writeWithAdapter(ByteBuffer data, StructAdapter annotation, Object value, Object obj) {
		StructTypeAdapter<T> adapter = (StructTypeAdapter<T>) StructUtils.createObj(annotation.value());
		adapter.write(data, (T) value, obj);
	}
	
	private static void putSizedField(ByteBuffer data, int length, Field f) {
		int size = StructUtils.getSizeType(f);
		if(size == 1) {
			data.put((byte) length);
		} else if(size == 2) {
			data.putShort((short) length);
		} else {
			data.putInt(length);
		}
	}
	
	private static int getEnumIndex(Object value, Class<?> type) {
		Object[] enumVals = type.getEnumConstants();
		for(int i = 0; i < enumVals.length; i++) {
			if(enumVals[i] == type) return i;
		}
		return -1;
	}
	
	private static void writeEnum(ByteBuffer data, Object value, Class<?> type, Field f) {
		int index = getEnumIndex(value, type);
		putSizedField(data, index, f);
	}
	
	private static void writeString(ByteBuffer data, String str, Field f) {
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
		
		// validate variables
		boolean utf16 = bchar == BasicCharset.UTF16;
		boolean utf8 = bchar == BasicCharset.UTF8;
		if(minLength < 0) {
			minLength = 0;
		}
		if(maxLength < minLength) {
			maxLength = minLength;
		}
		if(maxLength <= 0) {
			maxLength = Integer.MAX_VALUE;
		}
		if(align < 1) {
			align = 1;
		}

		// we need the byte data now if dealing with UTF8 strings
		byte[] bytes = null;
		if(!utf16) {
			Charset charset = utf8 ? StandardCharsets.UTF_8 : StandardCharsets.US_ASCII;
			bytes = str.getBytes(charset);
		}
		
		// calculate length
		int length, writeLength;
		if(utf8) {
			writeLength = length = bytes.length;
		} else {
			writeLength = length = str.length();
		}

		if(nullTerminated) {
			// validate length
			if(length > maxLength) {
				if(utf8) {
					int strlen = str.length();
					int lengthPre = 0;
					for(int i = 0; i < strlen; i++) {
						int lengthPost = lengthPre;
						
						// testing using UTF8 rules rather than the other possibility of removing 1 character from the end repeatedly
						char c = str.charAt(i);
						if(c <= 0x7F) {
							lengthPost++;
						} else if(c <= 0x7FF) {
							lengthPost += 2;
						} else if(Character.isHighSurrogate(c)) {
							lengthPost += 4;
							++i;
						} else {
							lengthPost += 3;
						}

						if(lengthPost > maxLength) break;
						lengthPre = lengthPost;
					}
					writeLength = lengthPre;
				} else {
					writeLength = maxLength;					
				}
			}
			// have to do check again because of utf8
			if(length < maxLength) {
				if(length < minLength) {
					// pad with nulls to min length
					length = minLength;
				} else {
					// add null terminating character
					length++;
				}
			}
		} else {
			// write length
			putSizedField(data, length, f);
		}
		
		// write the string
		int start = data.position();
		int end = start + length;
		if(utf16) {
			end += length;
			char[] chars = new char[writeLength];
			str.getChars(0, writeLength, chars, 0);
			CharBuffer buf = data.asCharBuffer(); 
			buf.put(chars);
			while(writeLength++ < length) {
				buf.put('\0');
			}
			data.position(end);
		} else {
			data.put(bytes, 0, writeLength);
			while(writeLength++ < length) {
				data.put((byte) 0);				
			}
		}
		
		// handle alignment
		int bytesWritten = end - start;
		int mod = bytesWritten % align;
		if(mod != 0) {
			data.position(end + (align - mod));
		}
	}
	
	private static void writePrimitive(ByteBuffer data, Object obj, Class<?> type) {
		if(type == Integer.TYPE) data.putInt((Integer) obj);
		if(type == Long.TYPE) data.putLong((Long) obj);
		if(type == Short.TYPE) data.putShort((Short) obj);
		if(type == Byte.TYPE) data.put((Byte) obj); 
		if(type == Boolean.TYPE) data.put((byte) ((Boolean) obj ? 1 : 0));
		if(type == Float.TYPE) data.putFloat((Float) obj);
		if(type == Double.TYPE) data.putDouble((Double) obj);
		if(type == Character.TYPE) data.putChar((Character) obj);
	}
	
	private static void writeArray(ByteBuffer data, Object array, Class<?> arrayType, Field f) {
		int length = Array.getLength(array);
		Class<?> elemType = arrayType.getComponentType();
		
		if(f == null || !Modifier.isFinal(f.getModifiers())) {
			putSizedField(data, length, f);
		}
		
		if(elemType.isPrimitive()) {
			for(int i = 0; i < length; i++) {
				Object arrayElem = Array.get(array, i);
				writePrimitive(data, arrayElem, elemType);
			}
		} else {
			for(int i = 0; i < length; i++) {
				Object arrayElem = Array.get(array, i);
				write(data, elemType, arrayElem);
			}
		}
	}
}