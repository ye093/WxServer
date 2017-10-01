package cn.life.auth;

import cn.life.dbhelper.MongoDBManager;
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

    public static void authCode(RoutingContext routingContext) {
        Session session = routingContext.session();
        String openId = session.get("openid");
        Integer auth = session.get("auth");

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
                .put("deadLine", deadLine);
        mongoClient.updateCollectionWithOptions("auth", new JsonObject().put("openId", openId),
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
        try {
            int randomCode = Integer.parseInt(routingContext.request().getParam("authCode"));
            long currentTime = System.currentTimeMillis();
            JsonObject geObj = new JsonObject().put("$gt", currentTime);
            MongoClient mongoClient = MongoDBManager.getReadClient(routingContext.vertx());
            mongoClient.findOneAndDelete("auth", new JsonObject().put("randomCode", randomCode).put("deadLine", geObj),
                    result -> {
                        try {
                            if (result.succeeded()) {
                                JsonObject resObj = result.result();
                                String openId = resObj.getString("openId");
                                session.put("openid", openId);
                                routingContext.reroute("/web/gonggao.html");
                            } else {
                                routingContext.response()
                                        .putHeader("content-type", "application/json")
                                        .end(new JsonObject()
                                                .put("code", 0).put("error", "error!").toString());
                            }
                        } catch (Exception e) {
                            routingContext.response()
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject()
                                            .put("code", 0).put("error", "error!").toString());
                        } finally {
                            mongoClient.close();
                        }
                    });
        } catch (Exception e) {
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("code", 0).put("error", "error!").toString());
        }
    }
}
