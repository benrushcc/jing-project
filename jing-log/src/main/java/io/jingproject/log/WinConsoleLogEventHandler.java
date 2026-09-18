package io.jingproject.log;

import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Os;
import io.jingproject.ffm.Libs;

/**
 * ConsoleLogEventHandler is designed to output logs to stdout and stderr on windows operating system
 */
public final class WinConsoleLogEventHandler implements LogEventHandler {
    private static final WinBindings WIN_BINDINGS = Libs.impl(WinBindings.class);

    public WinConsoleLogEventHandler() {

    }

    @Override
    public void handle(LogEvent logEvent) {

    }
}
