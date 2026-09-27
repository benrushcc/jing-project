package io.jingproject.common.conf;

import java.util.*;

public sealed interface Cfg {
    record CfgItem(String value) implements Cfg {
        // string is already immutable
        public CfgItem asImmutable() {
            return this;
        }
    }

    record CfgList(List<String> value) implements Cfg {
        // make the underlying list immutable
        public CfgList asImmutable() {
            return new CfgList(List.copyOf(value));
        }
    }

    record CfgObject(Map<String, Cfg> value) implements Cfg {

        private value record Frame(
                CfgObject object,
                Iterator<Map.Entry<String, Cfg>> iterator) {
        }

        // make the underlying map and every nested Cfg in the map immutable
        public CfgObject asImmutable() {
            IdentityHashMap<CfgObject, CfgObject> result = new IdentityHashMap<>();
            ArrayDeque<Frame> stack = new ArrayDeque<>();
            stack.push(new Frame(this, value.entrySet().iterator()));
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (frame.iterator().hasNext()) {
                    Map.Entry<String, Cfg> entry = frame.iterator().next();
                    Cfg child = entry.getValue();
                    if (child instanceof CfgObject childObject && !result.containsKey(childObject)) {
                        stack.push(new Frame(childObject, childObject.value().entrySet().iterator()));
                    }
                    continue;
                }
                CfgObject object = frame.object();
                Map<String, Cfg> immutableMap = new HashMap<>(object.value().size());
                for (Map.Entry<String, Cfg> entry : object.value().entrySet()) {
                    Cfg immutableChild = switch (entry.getValue()) {
                        case CfgItem cfgItem -> cfgItem.asImmutable();
                        case CfgList cfgList -> cfgList.asImmutable();
                        case CfgObject cfgObject -> {
                            CfgObject immutableObject = result.get(cfgObject);
                            if (immutableObject == null) {
                                throw new IllegalStateException("missing immutable child");
                            }
                            yield immutableObject;
                        }
                    };
                    immutableMap.put(entry.getKey(), immutableChild);
                }
                CfgObject immutableObject = new CfgObject(Map.copyOf(immutableMap));
                result.put(object, immutableObject);
                stack.pop();
            }
            return result.get(this);
        }
    }
}