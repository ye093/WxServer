package cn.life.pageConfig;

import cn.life.dbhelper.MongoDBManager;
import cn.life.qiniu.DownloadService;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by yejy_ on 2017-09-24.
 */
public class PageConfigInterface {

    public static void pageMessage(RoutingContext routingContext) {
        String pageName = routingContext.request().getParam("page");
        if (pageName == null || pageName.length() <= 0) {
            JsonObject json = new JsonObject();
            json.put("code", 0);
            json.put("error", "params error");
            HttpServerResponse serverResponse = routingContext.response();
            serverResponse.putHeader("content-type", "application/json");
            serverResponse.end(json.toString());
            return;
        }
        MongoClient mongoClient = MongoDBManager.getReadClient(routingContext.vertx());
        mongoClient.findOne("icons", new JsonObject().put("page", pageName),
                new JsonObject().put("_id", false), result -> {
                    if (result.succeeded()) {
                        JsonObject object = result.result();
                        JsonArray iconsJson = object.getJsonArray("icons");
                        JsonObject iconsObj = new JsonObject();
                        for (int i = 0; i < iconsJson.size(); i++) {
                            String iconsJsonString = iconsJson.getString(i);
                            mapUrlToJson(iconsJsonString, iconsObj);
                        }
                        JsonObject json = new JsonObject();
                        json.put("code", 1);
                        json.put("data", iconsObj);
                        HttpServerResponse serverResponse = routingContext.response();
                        serverResponse.putHeader("content-type", "application/json");
                        serverResponse.end(json.toString());
                        mongoClient.close();
                    } else {
                        JsonObject json = new JsonObject();
                        json.put("code", 0);
                        json.put("error", "page not found");
                        HttpServerResponse serverResponse = routingContext.response();
                        serverResponse.putHeader("content-type", "application/json");
                        serverResponse.end(json.toString());
                        mongoClient.close();
                    }
                });
    }

    private static void mapUrlToJson(String iconName, JsonObject obj) {
        String imageUrl = DownloadService.privateDownloadUrl(iconName);
        iconName = iconName.substring(0, iconName.indexOf("."));
        obj.put(iconName, imageUrl);
    }
}
