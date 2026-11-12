package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_bindings", supportedOS = {Os.WINDOWS})
public interface WinLogIoBindings {
    @Downcall(methodName = "jing_log_win_stdout_handle",critical = true)
    long stdoutHandle();

    @Downcall(methodName = "jing_log_win_stderr_handle",critical = true)
    long stderrHandle();

    @Downcall(methodName = "jing_log_win_open_append")
    long openAppend(MemorySegment fileNameCStr);

    @Downcall(methodName = "jing_log_win_write")
    long write(long handle, MemorySegment buf, long len);

    @Downcall(methodName = "jing_log_win_flush")
    int flush(long handle);

    @Downcall(methodName = "jing_log_win_file_size")
    long fileSize(long handle);

    @Downcall(methodName = "jing_log_win_close")
    int close(long handle);
}