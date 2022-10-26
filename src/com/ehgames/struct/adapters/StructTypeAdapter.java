package com.ehgames.struct.adapters;

import java.nio.ByteBuffer;

public interface StructTypeAdapter<T> {
	public T read(ByteBuffer data, Object container);
	
	public void write(ByteBuffer data, T obj, Object container);
}
