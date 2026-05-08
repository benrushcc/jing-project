package io.jingproject.commonprocess;

/**
 * Represents a line of generated source code.
 *
 * @param content the actual source code content of this line
 * @param indent  the number of indentation levels before the content, where each level equals 4 spaces
 */
public record GeneratorLine(
        String content,
        int indent
) {
}
