package com.textcraft.compiler;

/** OOP: Enum - the three runnable languages, with template, file extension and Judge0 name prefix. */
public enum Language {
    PYTHON("Python", "py", "Python (3", "python3",
            "print(\"Hello, World!\")\n"),
    CPP("C++", "cpp", "C++ (GCC", "g++",
            "#include <iostream>\n\nint main() {\n    std::cout << \"Hello, World!\" << std::endl;\n    return 0;\n}\n"),
    JAVA("Java", "java", "Java (OpenJDK", "javac",
            "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}\n");

    private final String label;
    private final String extension;
    private final String judge0Prefix;
    private final String localCommand;
    private final String template;

    Language(String label, String extension, String judge0Prefix, String localCommand, String template) {
        this.label = label;
        this.extension = extension;
        this.judge0Prefix = judge0Prefix;
        this.localCommand = localCommand;
        this.template = template;
    }

    public String label() { return label; }
    public String extension() { return extension; }
    public String judge0Prefix() { return judge0Prefix; }
    public String localCommand() { return localCommand; }
    public String template() { return template; }

    @Override
    public String toString() {
        return label;
    }

    public static Language fromExtension(String ext) {
        for (Language l : values()) {
            if (l.extension.equalsIgnoreCase(ext)) return l;
        }
        return null;
    }
}
