package cn.life.auth;

import io.vertx.core.json.JsonObject;

/**
 * Created by yejinyun on 2017/9/30.
 */
public class RoleMessage {
    public static JsonObject getRoleMessage(int auth) {
        JsonObject roleMsg = new JsonObject();
        if (auth == 9999) {
            //系统管理员
            roleMsg.put("role", "system");
            roleMsg.put("name", "系统管理员");
        }
        return roleMsg;
    }
}
