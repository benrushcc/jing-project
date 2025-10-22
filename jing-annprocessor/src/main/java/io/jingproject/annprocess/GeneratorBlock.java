package io.jingproject.annprocess;

import java.util.ArrayList;
import java.util.List;

public final class GeneratorBlock {
    private final List<GeneratorLine> lines = new ArrayList<>();
    private int indent = 0;

    public GeneratorBlock addLine(String content) {
        lines.add(new GeneratorLine(content, indent));
        return this;
    }

    public GeneratorBlock newLine() {
        lines.add(new GeneratorLine("", indent));
        return this;
    }

    public GeneratorBlock indent() {
        int currentIndent = indent;
        indent = Math.addExact(currentIndent, 1);
        return this;
    }

    public GeneratorBlock unindent() {
        int currentIndent = indent;
        indent = Math.subtractExact(currentIndent, 1);
        return this;
    }

    public int currentIndent() {
        return indent;
    }

    public List<GeneratorLine> lines() {
        return lines;
    }
}

