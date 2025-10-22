package io.jingproject.common;

import java.util.ServiceLoader;

public final class NetFactory {
    private NetFactory() {
        throw new UnsupportedOperationException("utility class");
    }

    public static NetFacade getInstance() {
        class Holder {
            static final NetFacade INSTANCE = Anchor.compute(NetFacade.class, () -> {
                ServiceLoader<NetFacade> nc = ServiceLoader.load(NetFacade.class);
                return nc.findFirst().orElseGet(DefaultNetImpl::new);
            });
        }
        return Holder.INSTANCE;
    }

    private static final class DefaultNetImpl implements NetFacade {
        @Override
        public void handle(NetEvent event) {

        }
    }
}
