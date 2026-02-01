package io.jingproject.marshall;

import java.util.ArrayList;
import java.util.List;

public final class MarshallData {
    private final List<MarshallInfo> infos;

    private MarshallData(List<MarshallInfo> infos) {
        this.infos = infos;
    }

    record MarshallTempInfo(Class<?> type, String strName) {

    }

    public static class MarshallDataBuilder {
        private final List<MarshallTempInfo> tempInfos = new ArrayList<>();
        private final List<Integer> strHashes = new ArrayList<>();
        private final List<Integer> utf8Hashes = new ArrayList<>();

        public MarshallDataBuilder withField(Class<?> type, String strName) {
            tempInfos.add(new MarshallTempInfo(type, strName));
            return this;
        }

        public MarshallDataBuilder strHash(int... hashes) {
            if(strHashes.isEmpty()) {
                throw new AssertionError();
            }
            for (int hash : hashes) {
                strHashes.add(hash);
            }
            return this;
        }

        public MarshallDataBuilder utf8Hash(int... hashes) {
            if(!utf8Hashes.isEmpty()) {
                throw new AssertionError();
            }
            for (int hash : hashes) {
                utf8Hashes.add(hash);
            }
            return this;
        }

        public MarshallData build() {
            return new MarshallData(List.of());
        }
    }

    public static MarshallDataBuilder newBuilder() {
        return new MarshallDataBuilder();
    }
}
