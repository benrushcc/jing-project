package io.jingproject.bindings.alloc;

import io.jingproject.bindings.PosixBindings;
import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Os;
import io.jingproject.common.Utils;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

// base class of an arena that reserves a large virtual memory up front and
// commits pages on demand, serving as the backing store of ArenaAllocator

// not thread-safe: one arena must be used by a single thread at a time
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

    public long slice(long size, long alignment) {
        long alignedPos = Utils.alignUp(pos, alignment);
        long newPos = Math.addExact(alignedPos, size);
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
        return alignedPos;
    }

    public void shrink(long previous) {
        if(pos == addr) {
            return ;
        }
        long pageSize = pageSize();
        if(peak < pos) {
            peak = Utils.alignUp(pos, pageSize);
        }
        if(previous == addr) {
            if(++cnt >= count) {
                long mid = ((committed & addr) + ((committed ^ addr) >> 1)) & -pageSize; // calculate the average and safely align it down to a pageSize boundary
                if(peak <= mid) {
                    uncommit(mid, committed - mid);
                    committed = mid;
                }
                peak = -1L;
                cnt = 0;
            }
        }
        pos = previous;
    }

    protected abstract long granularity();

    protected abstract long pageSize();

    protected abstract long reserve(long size);

    protected abstract void release();

    protected abstract void commit(long addr, long size);

    protected abstract void uncommit(long addr, long size);

    @Override
    public void close() {
        release();
    }

    // windows arena backed by VirtualAlloc and VirtualFree
    static final class WinArenaBase extends ArenaBase {
        private static final WinBindings WIN_BINDINGS = Libs.impl(WinBindings.class);
        private static final long GRANULARITY;
        private static final long PAGESIZE;
        private static final int MEM_RESERVE;
        private static final int MEM_COMMIT;
        private static final int MEM_DECOMMIT;
        private static final int MEM_RELEASE;
        private static final int PAGE_READWRITE;

        static {
            if(WIN_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize windows system bindings");
            }
            GRANULARITY = WIN_BINDINGS.winAllocateGranularity();
            PAGESIZE = WIN_BINDINGS.winPageSize();
            if(GRANULARITY < 0L || Long.bitCount(GRANULARITY) != 1 || PAGESIZE < 0L || Long.bitCount(PAGESIZE) != 1) {
                throw new ExceptionInInitializerError("granularity and pageSize must be positive and power of two");
            }
            MEM_RESERVE = WIN_BINDINGS.winMemReserve();
            MEM_COMMIT = WIN_BINDINGS.winMemCommit();
            MEM_DECOMMIT = WIN_BINDINGS.winMemDecommit();
            MEM_RELEASE = WIN_BINDINGS.winMemRelease();
            PAGE_READWRITE = WIN_BINDINGS.winPageReadWrite();
        }

        WinArenaBase(long size, int count, int initialPages) {
            super(size, count, initialPages);
        }

        @Override
        protected long granularity() {
            return GRANULARITY;
        }

        @Override
        protected long pageSize() {
            return PAGESIZE;
        }

        @Override
        protected long reserve(long size) {
            long addr = WIN_BINDINGS.winVirtualAlloc(0L, size, MEM_RESERVE, PAGE_READWRITE);
            if(NativeSegmentAccess.isErrPtr(addr)) {
                int err = NativeSegmentAccess.errCode(addr);
                throw new ForeignException("failed to reserve memory, err : " + err);
            }
            return addr;
        }

        @Override
        protected void release() {
            int r = WIN_BINDINGS.winVirtualFree(addr, 0L, MEM_RELEASE);
            if(r < 0) {
                throw new ForeignException("failed to release memory, err : " + -r);
            }
        }

        @Override
        protected void commit(long addr, long size) {
            long r = WIN_BINDINGS.winVirtualAlloc(addr, size, MEM_COMMIT, PAGE_READWRITE);
            if(NativeSegmentAccess.isErrPtr(r)) {
                int err = NativeSegmentAccess.errCode(r);
                throw new ForeignException("failed to commit memory, err : " + err);
            }
        }

        @Override
        protected void uncommit(long addr, long size) {
            int r = WIN_BINDINGS.winVirtualFree(addr, size, MEM_DECOMMIT);
            if(r < 0) {
                int err = -r;
                throw new ForeignException("failed to uncommit memory, err : " + err);
            }
        }
    }

    // posix arena backed by mmap/munmap
    static final class PosixArenaBase extends ArenaBase {
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);
        private static final long PAGESIZE;
        private static final int PROT_READ;
        private static final int PROT_WRITE;
        private static final int MAP_PRIVATE;
        private static final int MAP_ANONYMOUS;
        private static final int MADV_FREE;

        static {
            if(POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize posix system bindings");
            }
            PAGESIZE = POSIX_BINDINGS.posixPageSize();
            if(PAGESIZE < 0L || Long.bitCount(PAGESIZE) != 1) {
                throw new ExceptionInInitializerError("pageSize must be positive and power of two");
            }
            PROT_READ = POSIX_BINDINGS.posixProtRead();
            PROT_WRITE = POSIX_BINDINGS.posixProtWrite();
            MAP_PRIVATE = POSIX_BINDINGS.posixMapPrivate();
            MAP_ANONYMOUS = POSIX_BINDINGS.posixMapAnonymous();
            MADV_FREE = POSIX_BINDINGS.posixMadvFree();
        }

        PosixArenaBase(long size, int count, int initialPages) {
            super(size, count, initialPages);
        }

        @Override
        protected long granularity() {
            return PAGESIZE;
        }

        @Override
        protected long pageSize() {
            return PAGESIZE;
        }

        @Override
        protected long reserve(long size) {
            long addr = POSIX_BINDINGS.posixMmap(0L, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0L);
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
                int err = -r;
                throw new ForeignException("failed to release memory, err : " + err);
            }
        }

        @Override
        protected void commit(long addr, long size) {
            // no op
        }

        @Override
        protected void uncommit(long addr, long size) {
            int r = POSIX_BINDINGS.posixMadvise(addr, size, MADV_FREE);
            if(r < 0) {
                int err = -r;
                throw new ForeignException("failed to uncommit memory, err : " + err);
            }
        }
    }
}
