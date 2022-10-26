package com.ehgames.struct;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.ehgames.struct.adapters.BasicCharset;

public final class StringUtils {
	/**
	 * reads a four character code(FourCC)
	 * @param buf The ByteBuffer to read characters from
	 * @return The FourCC that was read as a String
	 */
	public static String readFourCC(ByteBuffer buf) {
		return readNullTerminatedString(buf, BasicCharset.ASCII, 4, 4);
	}
	
	/**
	 * Reads a null terminated ASCII String of any length
	 * @param buf The ByteBuffer to read characters from
	 * @return The String that was read
	 */
	public static String readNullTerminatedString(ByteBuffer buf) {
		return readNullTerminatedString(buf, BasicCharset.ASCII, 0, -1);
	}
	
	/**
	 * Reads a null terminated String with the given charset of any length
	 * @param buf The ByteBuffer to read characters from
	 * @param charset A {@link BasicCharset} denoting the charset the String should be read as
	 * @return The String that was read
	 */
	public static String readNullTerminatedString(ByteBuffer buf, BasicCharset charset) {
		return readNullTerminatedString(buf, charset, 0, -1);
	}
	
	/**
	 * Reads a null terminated ASCII string with the specified minimum and maximum lengths
	 * @param buf
	 * @param minLength The minimum length of the String to be read.
	 * 	Negative values will be treated as zero
	 * @param maxLength The maximum length of the String to be read.
	 *	Zero and negative values will be treated as no limit.
	 *	A positive value smaller than minLength will be treated as minLength
	 * @return The String that was read
	 */
	public static String readNullTerminatedString(ByteBuffer buf, int minLength, int maxLength) {
		return readNullTerminatedString(buf, BasicCharset.ASCII, minLength, maxLength);
	}
	
	/**
	 * Reads a null terminated string with the given charset and the specified minimum and maximum lengths
	 * @param buf The ByteBuffer to read characters from
	 * @param charset A {@link BasicCharset} denoting the charset the String should be read as
	 * @param minLength The minimum length of the String to be read.
	 * 	Negative values will be treated as zero
	 * @param maxLength The maximum length of the String to be read.
	 *	Zero and negative values will be treated as no limit.
	 *	A positive value smaller than minLength will be treated as minLength
	 * @return The String that was read
	 */
	public static String readNullTerminatedString(ByteBuffer buf, BasicCharset charset, int minLength, int maxLength) {
		// validate variables
		boolean utf16 = charset == BasicCharset.UTF16;
		if(minLength < 0) {
			minLength = 0;
		}
		// needs to be handled before max < min test or else we'd get a fixed length string unintentionally
		if(maxLength <= 0) {
			maxLength = Integer.MAX_VALUE;
		}
		if(maxLength < minLength) {
			maxLength = minLength;
		}
		
		Charset cset = utf16 ? null : charset == BasicCharset.ASCII ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8;
		
		// read the string
		String str;
		int start = buf.position();
		int length = 0;
		int charSize = utf16 ? Character.BYTES : Byte.BYTES;
		
		if(utf16) {
			StringBuilder sb = new StringBuilder();
			for(; length < maxLength; length++) {
				char c = buf.getChar();
				if(c == 0) break;
				sb.append(c);
			}
			str = sb.toString();
		} else {
			for(; length < maxLength; length++) {
				byte b = buf.get(start + length);
				if(b == 0) break;
			}
			
			byte[] chars = new byte[length];
			buf.get(chars);
			str = new String(chars, cset);
		}
		
		// ensure minimum characters read
		if(length < minLength) {
			int toSkip = charSize * (minLength - length);
			buf.position(buf.position() + toSkip);
		}
			
		return str;
	}
	
	private StringUtils() {}
}
