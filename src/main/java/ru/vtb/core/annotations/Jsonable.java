package ru.vtb.core.annotations;

import ru.vtb.core.utils.CommonHelper;

public interface Jsonable {

    default String toJson() {
        return CommonHelper.serializeToJsonString(this);
    }
}
