# ByteSkript Development Kit

Todo.

The development kit is a set of advanced resources available for library creators.
These are _not_ available within the standard ByteSkript Jar/runtime, but addons may selectively package them for compiling or runtime.

## Compilers

The development kit has multiple specialised compilers available for non-standard tasks or usage of the Skript language.

### Trigger Compiler
Rather than looking for code in a proper file structure, this compiles the source as though it is nested inside a `trigger` block.

This is designed for situations where a simple line-by-line script is more appropriate than an entire program.

### Baker Compiler
This is a meta-compiler that takes the output of the standard ByteSkript compiler and **bakes** it.

The baking process works similar to Java's JIT, in that it traces what is actually happening in the runtime scenario and refactors the code into a more efficient version.

