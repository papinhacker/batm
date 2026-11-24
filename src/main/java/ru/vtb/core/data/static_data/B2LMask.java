package ru.vtb.core.data.static_data;

import ru.vtb.core.utils.B2LHelper;

public enum B2LMask {

    _298cto("298cto"),
    CORP_038cto("CORP_038cto"),
    CORP_038ctd("CORP_038ctd"),
    VTB24_038cto("VTB24_038cto"),
    OPS_038ctd("OPS_038ctd"),
    D_038cto("D_038cto"),
    _038cts("038cts");

    private final String name;

    B2LMask(String name) {
        this.name = name;
    }

    public static B2LMask recognize(String b2lFilename) {
        b2lFilename = B2LHelper.substringFileMask(b2lFilename);
        for (B2LMask mask : values()) {
            if (b2lFilename.equals(mask.name)) {
                return mask;
            }
        }
        throw new RuntimeException("Unable to recognize file mask of file - " + b2lFilename);
    }

    public boolean isCorp() {
        return this.equals(CORP_038cto) || this.equals(CORP_038ctd) || this.equals(_038cts);
    }

    public String getName() {
        return this.name;
    }

}
