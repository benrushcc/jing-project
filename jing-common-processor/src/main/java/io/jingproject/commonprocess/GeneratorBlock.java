package io.jingproject.commonprocess;

import java.util.ArrayList;
import java.util.List;

public final class GeneratorBlock {
    private List<GeneratorLine> lines;
    private int indent = 0;

    public GeneratorBlock addLine(String content) {
        if (content == null) {
            throw new AnnotationProcessorException("content is null");
        }
        if (lines == null) {
            lines = new ArrayList<>();
        }
        lines.add(new GeneratorLine(content, indent));
        return this;
    }

    public GeneratorBlock newLine() {
        if (lines == null) {
            lines = new ArrayList<>();
        }
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

    public boolean isEmpty() {
        return lines == null || lines.isEmpty();
    }
}

