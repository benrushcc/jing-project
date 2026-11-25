package io.jingproject.net;

import io.jingproject.common.ConfigurationFactory;

public record NetConfig (
        String coordinatorThreadName,
        String reactorThreadNamePrefix,
        String workerThreadNamePrefix,
        int pairs,
        long reactorArenaSize,
        int reactorMaxEvents,
        int reactorBufferSize,
        int shutdownTimeout,
        int workerQueueSize
) {
    private static final long KB = 1024L;
    private static final long MB = 1024L * KB;
    private static final long GB = 1024L * MB;
    private static final int REACTOR_MIN_EVENTS = 16;

    public static NetConfig create() {
        String coordinatorThreadName = ConfigurationFactory.conf("jing.net.coor.threadname", "net-coor");
        if(coordinatorThreadName == null || coordinatorThreadName.isBlank()) {
            throw new NetException("empty coordinator thread name not allowed");
        }
        String reactorThreadNamePrefix = ConfigurationFactory.conf("jing.net.reactor.threadprefix", "net-reactor-");
        if(reactorThreadNamePrefix == null || reactorThreadNamePrefix.isBlank()) {
            throw new NetException("empty reactor thread name prefix not allowed");
        }
        String workerThreadNamePrefix = ConfigurationFactory.conf("jing.net.worker.threadprefix", "net-worker-");
        if(workerThreadNamePrefix == null || workerThreadNamePrefix.isBlank()) {
            throw new NetException("empty worker thread name prefix not allowed");
        }
        int pairs = ConfigurationFactory.confAsInt("jing.net.pairs", 4);
        if(pairs <= 0) {
            throw new NetException("illegal pairs value from configuration : " + pairs);
        }
        long reactorArenaSize = parseSize("jing.net.reactor.arenasize", GB);
        if(reactorArenaSize < 0L) {
            throw new NetException("reactor arena size can not be negative");
        }
        int reactorMaxEvents = ConfigurationFactory.confAsInt("jing.net.reactor.maxevents", 1024);
        if(reactorMaxEvents <= REACTOR_MIN_EVENTS) {
            throw new NetException("reactor max events too small : " + reactorMaxEvents);
        }
        return null;
    }

    private static long parseSize(String key, long defaultValue) {
        String conf = ConfigurationFactory.conf(key, "");
        if(conf == null || conf.isBlank()) {
            return defaultValue;
        }
        try {
            int len = conf.length();
            if(len > 2 && (conf.charAt(len - 1) == 'B' || conf.charAt(len - 1) == 'b')) {
                long v = Long.parseLong(conf.substring(0, len - 2));
                char c = conf.charAt(len - 2);
                if(c == 'G' || c == 'g') {
                    return Math.multiplyExact(v, GB);
                }
                if(c == 'M' || c == 'm') {
                    return Math.multiplyExact(v, MB);
                }
                if(c == 'K' || c == 'k') {
                    return Math.multiplyExact(v, KB);
                }
                return v;
            }
            return Long.parseLong(conf);
        } catch (NumberFormatException e) {
            throw new NetException("illegal number format of size value from configuration : " + conf);
        } catch (ArithmeticException e) {
            throw new NetException("size value out of range : " + conf);
        }
    }

}
