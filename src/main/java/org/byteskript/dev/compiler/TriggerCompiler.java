package org.byteskript.dev.compiler;

import mx.kenzie.foundation.ClassBuilder;
import mx.kenzie.foundation.MethodBuilder;
import mx.kenzie.foundation.Type;
import mx.kenzie.foundation.WriteInstruction;
import mx.kenzie.foundation.language.PostCompileClass;
import org.byteskript.skript.api.Library;
import org.byteskript.skript.compiler.*;
import org.byteskript.skript.compiler.structure.PreVariable;
import org.byteskript.skript.compiler.structure.TriggerTree;
import org.byteskript.skript.error.ScriptCompileError;
import org.byteskript.skript.error.ScriptParseError;
import org.byteskript.skript.lang.element.StandardElements;
import org.byteskript.skript.lang.syntax.entry.Trigger;
import org.byteskript.skript.lang.syntax.function.NoArgsFunctionMember;
import org.byteskript.skript.runtime.Skript;
import org.byteskript.skript.runtime.internal.CompiledScript;
import org.byteskript.skript.runtime.type.AtomicVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriggerCompiler extends SimpleSkriptCompiler implements ElementCompiler<Runnable> {
    
    private final WriteInstruction wrap = WriteInstruction
        .invokeStatic(new Type(AtomicVariable.class), new Type(AtomicVariable.class), "wrap", CommonTypes.OBJECT);
    private final java.util.regex.Pattern pattern = Pattern.compile("^(?<space>\\s*+)(?=\\S)");
    
    public TriggerCompiler(Library... libraries) {
        super(libraries);
    }
    
    public Runnable create(final String content, final Skript runtime) throws Throwable {
        final int index = getAnonymous();
        final Type type = new Type("skript.trigger_" + index);
        final PostCompileClass[] classes = this.compile(content, type);
        return (Runnable) runtime.getLoader().loadClass(type.dotPath(), classes[0].code()).newInstance();
    }
    
    @Override
    public PostCompileClass[] compile(String source, Type path) {
        final FileContext context = this.assemble(source, path);
        final ClassBuilder builder = context.getBuilder();
        builder.addInterfaces(new Type(Runnable.class));
        builder.addConstructor().writeCode(WriteInstruction.loadThis())
            .writeCode(WriteInstruction.invokeSpecial(new Type(CompiledScript.class)))
            .writeCode(WriteInstruction.returnEmpty());
        return context.compile();
    }
    
    protected FileContext assemble(String string, Type owner) {
        final FileContext context = new FileContext(owner);
        for (final Library library : this.getLibraries()) {
            context.addLibrary(library);
            for (final Type type : library.getTypes()) {
                context.registerType(type);
            }
        }
        this.prepareClass(string, context);
        return context;
    }
    
    protected void prepareClass(String string, FileContext context) {
        final ClassBuilder builder = context.getBuilder();
        final MethodBuilder method = builder.addMethod("run").setReturnType(void.class).setModifiers(0x0001);
        final List<String> lines = this.sanitiseSource(string);
        this.evaluate(method, lines, context);
        method.writeCode(WriteInstruction.returnEmpty());
    }
    
    protected List<String> sanitiseSource(final String source) {
        return this.adjustOffset(this.removeComments(source));
    }
    
    protected void evaluate(final MethodBuilder method, final List<String> lines, FileContext context) {
        context.lineNumber = 0;
        context.addFlag(AreaFlag.IN_FUNCTION);
        context.addFlag(AreaFlag.IN_TRIGGER);
        context.createUnit(StandardElements.SECTION);
        context.addSection(new NoArgsFunctionMember());
        context.addSection(new Trigger());
        context.setMethod(method);
        final TriggerTree tree = new TriggerTree(context.getSection(1), context.getVariables());
        context.createTree(tree);
        method.writeCode(this.prepareVariables(tree));
        context.setState(CompileState.CODE_BODY);
        for (String line : lines) {
            context.lineNumber++;
            context.line = null;
            if (line.isBlank()) continue;
            if (context.getMethod() != null) {
                context.getMethod().writeCode(WriteInstruction.lineNumber(context.lineNumber));
            }
            try {
                this.compileLine(line, context);
            } catch (ScriptParseError | ScriptCompileError ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new ScriptCompileError(context.lineNumber, "Unknown error during compilation:", ex);
            }
        }
        context.removeFlag(AreaFlag.IN_FUNCTION);
        context.removeFlag(AreaFlag.IN_TRIGGER);
        context.closeAllTrees();
        context.emptyVariables();
    }
    
    private List<String> adjustOffset(final List<String> lines) {
        final Matcher matcher = pattern.matcher(lines.get(0));
        final boolean found = matcher.find();
        final String indent;
        if (found) indent = matcher.group("space");
        else return lines;
        if (indent.length() == 0) return lines;
        final List<String> trimmed = new ArrayList<>();
        for (final String line : lines) {
            trimmed.add(line.substring(indent.length()));
        }
        return trimmed;
    }
    
    private WriteInstruction prepareVariables(TriggerTree context) {
        return (writer, visitor) -> {
            int i = 0;
            for (PreVariable variable : context.getVariables()) {
                if (!variable.skipPreset()) {
                    if (variable.atomic) {
                        visitor.visitInsn(1); // push null
                        wrap.accept(writer, visitor);
                        visitor.visitVarInsn(58, i); // astore
                    } else {
                        visitor.visitInsn(1); // push null
                        visitor.visitVarInsn(58, i); // astore
                    }
                }
                i++;
            }
        };
    }
}
