package ru.vtb.core.data.common_data;

import lombok.Getter;
import lombok.Setter;
import ru.vtb.core.common.EnvConstants;

@Getter
@Setter
public class UserData extends EnvConstants {

    protected String userName;
    protected String password;

    public UserData(String userName) {
        this.userName = userName;
        this.password = DEFAULT_PASSWORD;
    }

    public UserData(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }
}
