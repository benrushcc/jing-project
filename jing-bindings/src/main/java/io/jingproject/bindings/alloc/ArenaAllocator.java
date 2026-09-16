package io.jingproject.bindings.alloc;

import io.jingproject.common.anno.Fragile;

import java.lang.foreign.MemorySegment;

// allocator handing out slices of an ArenaBase and rewinding the arena to the
// position captured at construction when closed.
@Fragile
public final class ArenaAllocator implements Allocator {
    private final ArenaBase arenaBase;
    private final long pos;
    private boolean closed = false;

    // captures the current arena position as the rewind target of close().
    public ArenaAllocator(ArenaBase arenaBase) {
        this.arenaBase = arenaBase;
        this.pos = arenaBase.pos();
    }

    // delegates to the underlying arena slice operation.
    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if(closed) {
            throw new IllegalStateException("arena allocator already closed");
        }
        return arenaBase.slice(byteSize, byteAlignment);
    }

    // rewinds the arena to the position captured at construction.
    @Override
    public void close() {
        if(closed) {
            throw new IllegalStateException("arena allocator already closed");
        }
        arenaBase.shrink(pos);
        closed = true;
    }
}
