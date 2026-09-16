package io.jingproject.bindings.alloc;

import io.jingproject.bindings.PosixBindings;
import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Os;
import io.jingproject.common.Utils;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemorySegment;

// base class of an arena that reserves a large virtual memory up front and
// commits pages on demand, serving as the backing store of ArenaAllocator.
// not thread-safe: one arena must be used by a single thread at a time.
@Fragile
public sealed abstract class ArenaBase implements AutoCloseable permits ArenaBase.WinArenaBase, ArenaBase.PosixArenaBase {

    public static ArenaBase newInstance(long size, int count, int initialPages) {
        if(size <= 0L) {
            throw new IllegalArgumentException("size must be positive");
        }
        if(count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        if(initialPages <= 0) {
            throw new IllegalArgumentException("initialPages must be positive");
        }
        if (Os.current() == Os.WINDOWS) {
            return new WinArenaBase(size, count, initialPages);
        }
        return new PosixArenaBase(size, count, initialPages);
    }

    protected final long addr;
    protected final long bound;
    private final int count;
    private long pos;
    private long committed;
    private long peak;
    private int cnt;

    protected ArenaBase(long size, int count, int initialPages) {
        long alignedSize = Utils.alignUp(size, granularity());
        long initialCommit = Math.multiplyExact(initialPages, pageSize());
        if(alignedSize < initialCommit) {
            throw new ForeignException("size too small : " + alignedSize);
        }
        this.addr = reserve(alignedSize);
        this.bound = Math.addExact(addr, alignedSize);
        this.count = count;
        this.pos = addr;
        this.committed = Math.addExact(addr, initialCommit);
        this.peak = -1L;
        this.cnt = 0;
        try {
            commit(addr, initialCommit);
        } catch (ForeignException e) {
            release();
            throw e;
        }
    }

    public long pos() {
        return pos;
    }

    public long used() {
        return committed - addr;
    }

    public MemorySegment slice(long byteSize, long byteAlignment) {
        if(byteSize <= 0L) {
            throw new IllegalArgumentException("byteSize must be positive : " + byteSize);
        }
        long alignedPos = Utils.alignUp(pos, byteAlignment);
        long newPos = Math.addExact(alignedPos, byteSize);
        if(newPos > bound) {
            throw new ForeignException("arena out of range : " + newPos);
        }
        if(newPos > committed) {
            long grown = Math.addExact(committed, committed) - addr;
            long newCommitted = newPos > grown ? Utils.alignUp(newPos, pageSize()) : grown;
            if(newCommitted > bound) {
                throw new ForeignException("arena out of range : " + newCommitted);
            }
            commit(committed, newCommitted - committed);
            committed = newCommitted;
        }
        pos = newPos;
        return NativeSegmentAccess.reinterpret(MemorySegment.ofAddress(alignedPos), byteSize);
    }

    public void shrink(long previousPos) {
        if(pos == addr) {
            return ;
        }
        long pageSize = pageSize();
        if(peak < pos) {
            peak = Utils.alignUp(pos, pageSize);
        }
        if(previousPos == addr) {
            if(++cnt >= count) {
                long mid = (Math.addExact(committed, addr) / 2L) & -pageSize; // safe align down
                if(peak <= mid) {
                    uncommit(mid, committed - mid);
                    committed = mid;
                }
                peak = -1L;
                cnt = 0;
            }
        }
        pos = previousPos;
    }

    // allocation granularity of the platform.
    protected abstract long granularity();

    // page size of the platform.
    protected abstract long pageSize();

    // reserves size bytes and returns the base address.
    protected abstract long reserve(long size);

    // releases the reserved range.
    protected abstract void release();

    // backs the given range with physical memory.
    protected abstract void commit(long addr, long size);

    // returns the given range to the operating system.
    protected abstract void uncommit(long addr, long size);

    // releases the whole arena.
    @Override
    public void close() {
        release();
    }

    // windows arena backed by VirtualAlloc and VirtualFree.
    static final class WinArenaBase extends ArenaBase {
        private static final WinBindings WIN_BINDINGS = Libs.impl(WinBindings.class);

        static {
            if(WIN_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize WIN_BINDINGS");
            }
            long granularity = WIN_BINDINGS.winAllocateGranularity();
            long pageSize = WIN_BINDINGS.winPageSize();
            if(granularity < 0L || Long.bitCount(granularity) != 1 || pageSize < 0L || Long.bitCount(pageSize) != 1) {
                throw new ExceptionInInitializerError("granularity and pageSize must be positive and power of two");
            }
        }

        WinArenaBase(long size, int count, int initialPages) {
            super(size, count, initialPages);
        }

        @Override
        protected long granularity() {
            return WIN_BINDINGS.winAllocateGranularity();
        }

        @Override
        protected long pageSize() {
            return WIN_BINDINGS.winPageSize();
        }

        @Override
        protected long reserve(long size) {
            int memReserve = WIN_BINDINGS.winMemReserve();
            int pageReadWrite = WIN_BINDINGS.winPageReadWrite();
            long addr = WIN_BINDINGS.winVirtualAlloc(0L, size, memReserve, pageReadWrite);
            if(NativeSegmentAccess.isErrPtr(addr)) {
                int err = NativeSegmentAccess.errCode(addr);
                throw new ForeignException("failed to reserve memory, err : " + err);
            }
            return addr;
        }

        @Override
        protected void release() {
            int memRelease = WIN_BINDINGS.winMemRelease();
            int r = WIN_BINDINGS.winVirtualFree(addr, 0L, memRelease);
            if(r < 0) {
                throw new ForeignException("failed to release memory, err : " + -r);
            }
        }

        @Override
        protected void commit(long addr, long size) {
            int memCommit = WIN_BINDINGS.winMemCommit();
            int pageReadWrite = WIN_BINDINGS.winPageReadWrite();
            long r = WIN_BINDINGS.winVirtualAlloc(addr, size, memCommit, pageReadWrite);
            if(NativeSegmentAccess.isErrPtr(r)) {
                int err = NativeSegmentAccess.errCode(r);
                throw new ForeignException("failed to commit memory, err : " + err);
            }
        }

        @Override
        protected void uncommit(long addr, long size) {
            int memDecommit = WIN_BINDINGS.winMemDecommit();
            int r = WIN_BINDINGS.winVirtualFree(addr, size, memDecommit);
            if(r < 0) {
                throw new ForeignException("failed to uncommit memory, err : " + -r);
            }
        }
    }

    // posix arena backed by mmap/munmap. commit is a no-op because the range is
    // mapped up front; uncommit returns pages to the os via madvise.
    static final class PosixArenaBase extends ArenaBase {
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);

        static {
            if(POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize POSIX_BINDINGS");
            }
            long pageSize = POSIX_BINDINGS.posixPageSize();
            if(pageSize < 0L || Long.bitCount(pageSize) != 1) {
                throw new ExceptionInInitializerError("pageSize must be positive and power of two");
            }
        }

        PosixArenaBase(long size, int count, int initialPages) {
            super(size, count, initialPages);
        }

        @Override
        protected long granularity() {
            return POSIX_BINDINGS.posixPageSize();
        }

        @Override
        protected long pageSize() {
            return POSIX_BINDINGS.posixPageSize();
        }

        @Override
        protected long reserve(long size) {
            int protRead = POSIX_BINDINGS.posixProtRead();
            int protWrite = POSIX_BINDINGS.posixProtWrite();
            int mapPrivate = POSIX_BINDINGS.posixMapPrivate();
            int mapAnonymous = POSIX_BINDINGS.posixMapAnonymous();
            long addr = POSIX_BINDINGS.posixMmap(0L, size, protRead | protWrite, mapPrivate | mapAnonymous, -1, 0L);
            if (NativeSegmentAccess.isErrPtr(addr)) {
                int err = NativeSegmentAccess.errCode(addr);
                throw new ForeignException("failed to reserve memory, err : " + err);
            }
            return addr;
        }

        @Override
        protected void release() {
            int r = POSIX_BINDINGS.posixMunmap(addr, bound - addr);
            if(r < 0) {
                throw new ForeignException("failed to release memory, err : " + -r);
            }
        }

        @Override
        protected void commit(long addr, long size) {
            // no op
        }

        @Override
        protected void uncommit(long addr, long size) {
            int madvFree = POSIX_BINDINGS.posixMadvFree();
            int r = POSIX_BINDINGS.posixMadvise(addr, size, madvFree);
            if(r < 0) {
                throw new ForeignException("failed to uncommit memory, err : " + -r);
            }
        }
    }
}
