import org.byteskript.dev.compiler.TriggerCompiler;
import org.byteskript.skript.error.ScriptParseError;
import org.byteskript.skript.runtime.Skript;
import org.junit.BeforeClass;
import org.junit.Test;

public class TriggerCompilerTest {
    
    private static Skript skript;
    private static TriggerCompiler compiler;
    
    @BeforeClass
    public static void start() {
        skript = new Skript();
        compiler = new TriggerCompiler();
    }
    
    @Test
    public void basic() throws Throwable {
        final Runnable runnable = compiler.create("""
            assert true
            assert 1 is 1
            """, skript);
        assert runnable != null;
        skript.runScript(runnable);
    }
    
    @Test
    public void parseError() throws Throwable {
        try {
            compiler.create("""
                assert true
                eat seeds
                assert 1 is 1
                """, skript);
        } catch (ScriptParseError error) {
            assert error.getLine() == 2;
            assert error.getDetails().line.equals("eat seeds");
            return;
        }
        assert false : "Parsing succeeded erroneously.";
    }
    
    @Test
    public void messyIndentation() throws Throwable {
        // somehow this works, and I don't know why, but I would like to see whether it stops working ever
        compiler.create("""
            assert true
              assert true
              assert true
             assert true
             if true:
                    assert true
             if true:
              assert true
             else:
              assert false
            assert 1 is 1
            """, skript).run();
    }
    
}
