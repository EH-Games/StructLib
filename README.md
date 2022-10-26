# StructLib

Java library for reading structured data from a ByteBuffer, generally with a single method call by the user

## Basic Usage

```java
java.nio.ByteBuffer data;

// create ByteBuffer, fill it with data, and set its byte order here

ExampleType object = com.ehgames.struct.StructReader.read(data, ExampleType.class);
```

There are three overloads of the `read` method in `com.ehgames.struct.StructReader`
- `<T> T read(ByteBuffer, Class<T>)`
- `<T> void read(ByteBuffer, T)`
- `<T> void read(ByteBuffer, Class<? extends T>, T)`

The first method creates a new object to read the data into, while the other two read data into an existing object.

All non-transient instance variables are read into the object, including private ones and ones declared in parent classes, starting with the class furthest up the hierarchy.
The byte order specified in the ByteBuffer is respected, meaning primitive types can be parsed as either little or big endian.

## Parsing Arrays

Java does not have fixed sized arrays like some other languages.
To give an array a fixed size, it should be declared final and initialized in its containing class with said size.
Otherwise, the default behavior of this library is to read a 32-bit length before reading the array.
The size of the length variable can be overridden with the `@LengthSize` annotation.

## Parsing Enum Values

While Java's enum values are basically instances of the enum class itself, many other languages treat them as integer values with a possibly non-standard size.
By default, enum values will be read as a 32-bit value.
This can be overridden with the `@LengthSize` annotation.

## Parsing Strings

There is a large number of ways a String can be stored in various languages.
The default method is by assuming it is an ASCII string preceeded by a 32-bit length with no alignment, minimum length, or maximum length.
Like with arrays and enum values, the length variable's size can be set with the `@LengthSize` annotation.
There also exists the `@StringAdapter` annotation.
It allows users to specify charset, alignment, minumum length, maximum length, and whether the string is null terminated.
If null terminated, no length variable is read and the size is determined by whatever occurs first of a null terminating character or the maximum length.
If not null terminated, minimum and maximum length are ignored.

## Defining Custom Parsers

Sometimes it becomes necessary to parse objects in a more specialized way. For that purpose, the `StructAdapter` and `StructTypeAdapter` classes exist.
Simply implement `StructTypeAdapter` for the proper type and then add the `StructAdapter` annotation to the relevant variables with your class for the parameter.