package cn.life.login;

import cn.life.config.ServiceConfig;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * Created by yejinyun on 2017/9/22.
 */
public class LoginInterface {

    public static void onLogin(RoutingContext routingContext) {
        JsonObject jsonObject = routingContext.getBodyAsJson();
        requestSessionKey(String.valueOf(jsonObject.getValue("code")), routingContext);
    }

    private static void requestSessionKey(String jsCode, RoutingContext routingContext) {
        String params = "?appid="
                + ServiceConfig.APP_ID
                + "&secret=" + ServiceConfig.APP_SE
                + "&js_code=" + jsCode
                + "&grant_type=authorization_code";
        String url = ServiceConfig.WX_BASE_URL + ServiceConfig.JS_CODE2SESSION + params;
        WebClient client = WebClient.create(routingContext.vertx());
        client
                .getAbs(url)
                .ssl(true)
                .as(BodyCodec.jsonObject())
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<JsonObject> response = ar.result();
                        JsonObject body = response.body();
                        System.out.println("login response: " + body);
                        if (body != null && body.containsKey("openid")) {
                            Session session = routingContext.session();
                            session.put("openid", body.getString("openid", ""));
                            session.put("session_key", body.getString("session_key", ""));

                            //响应
                            JsonObject json = new JsonObject();
                            json.put("code", 1);
                            json.put("message", "success");
                            HttpServerResponse serverResponse = routingContext.response();
                            serverResponse.putHeader("content-type", "application/json");
                            serverResponse.end(json.toString());
                        } else {
                            //响应
                            JsonObject json = new JsonObject();
                            json.put("code", 0);
                            json.put("error", body);
                            HttpServerResponse serverResponse = routingContext.response();
                            serverResponse.putHeader("content-type", "application/json");
                            serverResponse.end(json.toString());
                        }
                    } else {
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                        //响应
                        JsonObject json = new JsonObject();
                        json.put("code", 0);
                        json.put("error", "cause Error");
                        HttpServerResponse serverResponse = routingContext.response();
                        serverResponse.putHeader("content-type", "application/json");
                        serverResponse.end(json.toString());
                    }
                });
    }
}
