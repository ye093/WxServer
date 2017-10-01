package cn.life.auth;

import cn.life.config.Constant;
import cn.life.dbhelper.MongoDBManager;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

/**
 * 获取运营平台授权码
 * Created by yejy_ on 2017-10-01.
 */
public class WxAuthCodeInterface {

    public static void getAuthCode(RoutingContext routingContext) {
        Session session = routingContext.session();
        String openId = session.get(Constant.OPEN_ID);
        Integer auth = session.get(Constant.AUTH);

        if (openId == null || auth == null) {
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("code", 2).put("message", "not login or auth error").toString());
            return;
        }
        //此处向授权表更新授权码
        //获取五位随机数并更新授权表
        int randomCode = (int) ((Math.random() * 9 + 1) * 10000);
        //有效期为60秒
        long deadLine = System.currentTimeMillis() + 60000;
        MongoClient mongoClient = MongoDBManager.getWriteClient(routingContext.vertx());

        JsonObject updateObj = new JsonObject();
        updateObj.put("code", randomCode)
                .put("deadLine", deadLine)
                .put("valid", true)
                .put("auth", auth);
        mongoClient.updateCollectionWithOptions("author", new JsonObject().put("openId", openId),
                new JsonObject().put("$set", updateObj), new UpdateOptions(true), updateRes -> {
                    try {
                        if (updateRes.succeeded()) {
                            JsonObject data = new JsonObject().put("randomCode", randomCode)
                                    .put("deadLine", deadLine);
                            routingContext.response()
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject()
                                            .put("code", 1).put("data", data).toString());
                        } else {
                            routingContext.response()
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject()
                                            .put("code", 0).put("error", "update error!").toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        mongoClient.close();
                    }
                });
    }

    public static void checkAuthCode(RoutingContext routingContext) {
        Session session = routingContext.session();
        String openId = session.get(Constant.OPEN_ID_WEB);
        Integer auth = session.get(Constant.AUTH_WEB);
        if (openId != null && auth != null) {
            routingContext.reroute(HttpMethod.GET, "/web/gonggao.html");
            return;
        }

        try {
            int randomCode = Integer.parseInt(routingContext.request().getParam("authCode"));
            long currentTime = System.currentTimeMillis();
            //在有效时间范围之内
            JsonObject geObj = new JsonObject().put("$gt", currentTime);
            MongoClient mongoClient = MongoDBManager.getWriteClient(routingContext.vertx());
            //查询完之后更改valid为false
            JsonObject updateObj = new JsonObject().put("valid", false);
            mongoClient.findOneAndUpdate("author",
                    new JsonObject().put("$and", new JsonArray()
                            .add(new JsonObject().put("code", randomCode))
                            .add(new JsonObject().put("deadLine", geObj)
                                                 .put("valid", true))
                    ),
                    new JsonObject().put("$set", updateObj),
                    result -> {
                        try {
                            if (result.succeeded()) {
                                JsonObject resObj = result.result();
                                if (resObj == null) {
                                    routingContext.response()
                                            .putHeader("content-type", "application/json")
                                            .end(new JsonObject()
                                                    .put("code", 0).put("error", "code invalid!").toString());
                                } else {
                                    String openId0 = resObj.getString("openId");
                                    int auth0 = resObj.getInteger("auth", 0);
                                    session.put(Constant.OPEN_ID_WEB, openId0).put(Constant.AUTH_WEB, auth0);
                                    routingContext.reroute(HttpMethod.GET, "/web/gonggao.html");
                                }
                            } else {
                                routingContext.response()
                                        .putHeader("content-type", "application/json")
                                        .end(new JsonObject()
                                                .put("code", 0).put("error", "result error!").toString());
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            routingContext.response()
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject()
                                            .put("code", 0).put("error", "other error!").toString());
                        } finally {
                            mongoClient.close();
                        }
                    });
        } catch (Exception e) {
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("code", 0).put("error", "params error!").toString());
        }
    }
}
