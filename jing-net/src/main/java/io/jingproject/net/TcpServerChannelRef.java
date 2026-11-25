package io.jingproject.net;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class TcpServerChannelRef {
    private static final VarHandle SEQ_VH;
    @SuppressWarnings("unused")
    private static int SEQ;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(TcpServerChannelRef.class, MethodHandles.lookup());
            SEQ_VH = lookup.findStaticVarHandle(TcpServerChannelRef.class, "SEQ", int.class);
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private TcpServerChannelRef() {

    }

    public Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private int seq = 0;
        private Coordinator coordinator = null;

        public Builder seq(int seq) {
            this.seq = seq;
            return this;
        }

        public Builder coordinator(Coordinator coordinator) {
            this.coordinator = coordinator;
            return this;
        }

        public TcpServerChannelRef build() {
            // TODO
            if(coordinator == null) {
                throw new NetException("coordinator must be set");
            }
            NetConfig config = coordinator.config();
            int pairs = config.pairs();
            if(seq < 0 || seq > pairs) {
                throw new NetException("seq out of range : " + seq);
            }
            if(seq == 0) {
                int v = (int) SEQ_VH.getAndAdd(1);
                seq = v % pairs;
            }
            return new TcpServerChannelRef();
        }
    }
}
