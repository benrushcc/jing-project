package io.jingproject.annprocess;

import java.util.ArrayList;
import java.util.List;

public final class GeneratorBlock {
    private final List<GeneratorLine> lines = new ArrayList<>();
    private int indent = 0;

    public GeneratorBlock addLine(String content) {
        if(content == null) {
            throw new AnnotationProcessorException("content is null");
        }
        lines.add(new GeneratorLine(content, indent));
        return this;
    }

    public GeneratorBlock newLine() {
        lines.add(new GeneratorLine("", indent));
        return this;
    }

    public GeneratorBlock indent() {
        int currentIndent = indent;
        indent = Math.incrementExact(currentIndent);
        return this;
    }

    public GeneratorBlock unindent() {
        int currentIndent = indent;
        indent = Math.decrementExact(currentIndent);
        return this;
    }

    public int currentIndent() {
        return indent;
    }

    public List<GeneratorLine> lines() {
        return lines;
    }
}

