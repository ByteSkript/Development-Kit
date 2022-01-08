package org.byteskript.dev.compiler;

import org.byteskript.skript.runtime.Skript;

public interface ElementCompiler<Type> {
    
    Type create(final String content, final Skript runtime) throws Throwable;
    
}
