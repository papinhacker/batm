package ru.vtb.core.annotations;

import org.apache.commons.lang3.StringUtils;
import ru.vtb.core.common.EnvConstants;
import ru.vtb.core.data.common_data.PreconditionsConstants;
import ru.vtb.core.data.common_data.UserData;

import java.lang.reflect.Method;

public class PreconditionAnnotationParser {

    public static UserData parseUser(Method method) {
        UserData userData = null;
        if (method.getDeclaringClass().isAnnotationPresent(BaseState.class)) {
            BaseState baseState = method.getDeclaringClass().getAnnotation(BaseState.class);
            if (!baseState.user().isEmpty()) {
                userData = getUser(baseState.page());
            }
        }
        if (method.isAnnotationPresent(Precondition.class)) {
            Precondition precondition = method.getAnnotation(Precondition.class);
            if (!precondition.user().isEmpty()) {
                userData = getUser(precondition.user());
            }
        }
        return userData;
    }

    public static String parseUrl(Method method) {
        String url = null;
        if (method.getDeclaringClass().isAnnotationPresent(BaseState.class)) {
            BaseState baseState = method.getDeclaringClass().getAnnotation(BaseState.class);
            if (!baseState.page().isEmpty()) {
                url = getPageUrl(baseState.page());
            }
        }
        if (method.isAnnotationPresent(Precondition.class)) {
            Precondition precondition = method.getAnnotation(Precondition.class);
            if (!precondition.page().isEmpty()) {
                url = getPageUrl(precondition.page());
            }
        }
        return url;
    }

    private static UserData getUser(String user) {
        switch (user) {
            case PreconditionsConstants.SolarUI.User.ADMIN_USER:
                return new UserData(EnvConstants.BASIC_USER_LOGIN);
            case PreconditionsConstants.SolarUI.User.BASIC_USER:
                return new UserData("name");
            default:
                return null;
        }
    }

    private static String getPageUrl(String page) {
        if (StringUtils.isEmpty(page)) {
            return null;
        }
        switch (page) {
            case PreconditionsConstants.SolarUI.Page.LOGIN_PAGE:
                return EnvConstants.LOGIN_PAGE;
            case PreconditionsConstants.SolarUI.Page.HOME_PAGE:
                return "work please";
            default:
                return page;
        }
    }
}
