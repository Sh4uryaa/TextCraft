package com.textcraft.compiler;

import static org.junit.jupiter.api.Assertions.*;

import com.textcraft.model.CompileResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class LocalRunnerTest {
    @Test
    void templatesExistForEveryLanguage() {
        for (Language l : Language.values()) assertTrue(l.template().contains("Hello, World!"));
        assertEquals(Language.CPP, Language.fromExtension("CPP"));
        assertNull(Language.fromExtension("zzz"));
    }

    @Test
    void javaRunsLocallyAndReadsInput() throws Exception {
        Assumptions.assumeTrue(LocalProcessRunner.isAvailable(Language.JAVA));
        String src = "import java.util.Scanner; public class Main { public static void main(String[] a) {"
                + " System.out.println(\"Hi \" + new Scanner(System.in).nextLine()); } }";
        CompileResult r = new LocalProcessRunner().run(Language.JAVA, src, "Sam\n");
        assertTrue(r.isSuccess(), r.combinedOutput());
        assertTrue(r.getStdout().contains("Hi Sam"));
    }

    @Test
    void compileErrorsAreReported() throws Exception {
        Assumptions.assumeTrue(LocalProcessRunner.isAvailable(Language.JAVA));
        CompileResult r = new LocalProcessRunner().run(Language.JAVA, "public class Main { oops }", "");
        assertFalse(r.isSuccess());
        assertFalse(r.getCompileOutput().isEmpty());
    }
}
