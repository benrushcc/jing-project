package io.jingproject.net;

import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing")
public interface NetLib {
    int MUX_NONE_FLAG                   = 0x0000;
    int MUX_READABLE_FLAG               = 0x0001;
    int MUX_WRITEABLE_FLAG              = 0x0002;
    int MUX_READABLE_AND_WRITEABLE_FLAG = MUX_READABLE_FLAG | MUX_WRITEABLE_FLAG;
}
