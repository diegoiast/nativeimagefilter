# Welcome

This small app shows how to take an Android Bitmap and serialize it to C++ using JNI, 
and then manipulate it, and return it to Java land. The API contains 2 implemented filters, 
and another one still unimplemented.

Code should be trivial to understand.

# Notes
- There is a design flaw in the API - it creates a new object after each
      apply, which means that you cannot chain too much effects, otherwise
      OOM will be triggered. This is the reason why I resized  the image.
- Maybe I should make the API work on the original memory buffer, and
      then allocating/releasing memory locally on native... that shuold work
      even though not as I imagined.
- I am unsure if the chainability feature really works. I tried
      inverting twise, and it does seem to revert to the original image,
      but binary and then invert does not really seem to work.
- The JNI and C code should be splitted - and I need to make a Linux
      version to test under valgrind.
- I need to add unit tests - this is the reason for the
      putpixel/getpixel methods. However - the machine I am working on has a
      problem and adding new files just fails on Android Studio... I should
      fix this ASAP.
- If native code returns a valid error code... I am no always getting
      that information in Java. For example from input/ouput buffer will
      throw the same exception.
- I am unsure about the top panel in the UI... this works on my phone,
      but I defined this using dp... not smart. This will break in some
      scenarios.
- The JNI entry could be prettier. I know how to do this, but currently too 
  lazy. Will not gain any benefit... but will be prettier. (the cost - I will
  have to read java class members which is another point of failure).

# Contact for quesitons

If you have questions, my email is Diego Iastrubni - diegoiast+jni@gmail.com 
