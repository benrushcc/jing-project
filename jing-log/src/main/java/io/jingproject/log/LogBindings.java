package io.jingproject.log;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing")
public interface LogBindings {

    @Downcall(methodName = "jing_win_ansi_support", constant = true, critical = true)
    int winAnsiSupport();

    @Downcall(methodName = "jing_std_output_dword", constant = true, critical = true)
    int stdOutputDword();

    @Downcall(methodName = "jing_std_error_dword", constant = true, critical = true)
    int stdErrorDword();

    @Downcall(methodName = "jing_get_std_handle", critical = true)
    void getStdHandle(int handle, MemorySegment jingResult);

    @Downcall(methodName = "jing_create_file")
    void createFile(MemorySegment fileName, MemorySegment jingResult);

    @Downcall(methodName = "jing_write_file")
    void writeFile(MemorySegment handle, MemorySegment buf, int len, MemorySegment jingResult);

    @Downcall(methodName = "jing_flush_file")
    void flushFile(MemorySegment handle, MemorySegment jingResult);

    @Downcall(methodName = "jing_stdout_fileno",constant = true, critical = true)
    int stdOutputFileno();

    @Downcall(methodName = "jing_stderr_fileno",constant = true, critical = true)
    int stdErrorFileno();

    @Downcall(methodName = "jing_open_fd")
    void openFd(MemorySegment fileName, MemorySegment jingResult);

    @Downcall(methodName = "jing_write_fd")
    void writeFd(int fd, MemorySegment buf, long size, MemorySegment jingResult);
}
