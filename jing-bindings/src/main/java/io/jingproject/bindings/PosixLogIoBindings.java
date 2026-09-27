package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_bindings", supportedOS = {Os.LINUX, Os.MACOS})
public interface PosixLogIoBindings {
    @Downcall(methodName = "jing_log_posix_open_append")
    int openAppend(MemorySegment fileNameCStr);

    @Downcall(methodName = "jing_log_posix_write")
    long write(int fd, MemorySegment buf, long len);

    @Downcall(methodName = "jing_log_posix_flush")
    int flush(int fd);

    @Downcall(methodName = "jing_log_posix_file_size")
    long fileSize(int fd);

    @Downcall(methodName = "jing_log_posix_close")
    int close(int fd);
}