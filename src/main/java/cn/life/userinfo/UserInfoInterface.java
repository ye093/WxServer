package cn.life.userinfo;

import cn.life.auth.RoleMessage;
import cn.life.config.ServiceConfig;
import cn.life.dbhelper.MongoDBManager;
import cn.life.qiniu.DownloadService;
import cn.life.util.WXBizDataCrypt;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

/**
 * Created by yejy_ on 2017-09-23.
 * 获取用户信息
 */
public class UserInfoInterface {

    /**
     * 登录，从微信提取用户信息
     */
    public static void parseUserMessage(RoutingContext routingContext) {
        Session session = routingContext.session();
        String openId = session.get("openid");
        String sessionKey = session.get("session_key");
        if (openId == null || sessionKey == null) {
            //响应
            JsonObject json = new JsonObject();
            json.put("code", 2);
            json.put("message", "not login");
            HttpServerResponse serverResponse = routingContext.response();
            serverResponse.putHeader("content-type", "application/json");
            serverResponse.end(json.toString());
            return;
        }

        JsonObject jsonObject = routingContext.getBodyAsJson();
        String encryptedData = jsonObject.getString("encryptedData");
        String iv = jsonObject.getString("iv");
        if (encryptedData == null || iv == null) {
            JsonObject json = new JsonObject();
            json.put("code", 0);
            json.put("message", "params error");
            HttpServerResponse serverResponse = routingContext.response();
            serverResponse.putHeader("content-type", "application/json");
            serverResponse.end(json.toString());
            return;
        }
        //解析用户信息
        WXBizDataCrypt wxBizDataCrypt = new WXBizDataCrypt(ServiceConfig.APP_ID, sessionKey);
        String data = wxBizDataCrypt.decryptData(encryptedData, iv);
        if (data == null) {
            JsonObject json = new JsonObject();
            json.put("code", 0);
            json.put("error", "decryptData error");
            HttpServerResponse serverResponse = routingContext.response();
            serverResponse.putHeader("content-type", "application/json");
            serverResponse.end(json.toString());
            return;
        }
        System.out.println(data);
        JsonObject useData = new JsonObject(data);
        String nickName = useData.getString("nickName", "");
        String avatarUrl = useData.getString("avatarUrl", "");
        int gender = useData.getInteger("gender", 0);
        String city = useData.getString("city", "");
        String province = useData.getString("province", "");
        String openId2 = useData.getString("openId", "");


        MongoClient mongoClient = MongoDBManager.getWriteClient(routingContext.vertx());

        JsonObject setJson = new JsonObject().put("$set",
                new JsonObject().put("wxUserInfo",
                        new JsonObject().put("name", nickName)
                                .put("url", avatarUrl)
                                .put("gender", gender)
                                .put("city", city)
                                .put("province", province)));
        mongoClient.updateCollectionWithOptions("users", new JsonObject().put("openId", openId2),
                setJson, new UpdateOptions(true), result -> {
                    if (result.succeeded()) {
                        getUserInfoFromDB(routingContext, mongoClient, openId2);
                    } else {
                        mongoClient.close();
                        JsonObject json = new JsonObject().put("code", 0)
                                .put("error", "update user info error");
                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .end(json.toString());
                    }
                });
    }

    /**
     * 获取用户信息 name,url,gender,phone,personalitySignature
     */
    public static void getUserInfo(RoutingContext routingContext) {
        String openId = routingContext.session().get("openid");
        if (openId == null) {
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("code", 2).put("message", "not login").toString());
            return;
        }
        MongoClient mongoClient = MongoDBManager.getReadClient(routingContext.vertx());
        getUserInfoFromDB(routingContext, mongoClient, openId);
    }

    /**
     * 更新用户信息 name,url,gender,phone,personalitySignature
     */
    public static void updateUserInfo(RoutingContext routingContext) {
        String openId = routingContext.session().get("openid");
        if (openId == null) {
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("code", 2).put("message", "not login").toString());
            return;
        }
        try {
            JsonObject jsonObject = routingContext.getBodyAsJson();
            String name = String.valueOf(jsonObject.getValue("showName"));
            int gender = Integer.parseInt(String.valueOf(jsonObject.getValue("gender")));
            long phone = Long.parseLong(String.valueOf(jsonObject.getValue("showPhone")));
            String province = String.valueOf(jsonObject.getValue("province"));
            String city = String.valueOf(jsonObject.getValue("city"));
            String county = String.valueOf(jsonObject.getValue("county"));
            String town = String.valueOf(jsonObject.getValue("town"));
            String detail = String.valueOf(jsonObject.getValue("detail"));
            String personalitySignature = String.valueOf(jsonObject.getValue("personalitySignature"));
            JsonObject user = new JsonObject().put("$set",
                    new JsonObject().put("userInfo.name", name)
                                    .put("userInfo.gender", gender)
                                    .put("userInfo.phone", phone)
                                    .put("userInfo.personalitySignature", personalitySignature)
                                    .put("address.province", province)
                                    .put("address.city", city)
                                    .put("address.county", county)
                                    .put("address.town", town)
                                    .put("address.detail", detail));

            MongoClient mongoClient = MongoDBManager.getWriteClient(routingContext.vertx());
            mongoClient.updateCollection("users", new JsonObject().put("openId", openId),
                    user, res -> {
                        if (res.succeeded()) {
                            mongoClient.close();
                            routingContext.response()
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject()
                                            .put("code", 1).put("message", "update user info success").toString());
                        } else {
                            mongoClient.close();
                            routingContext.response()
                                    .putHeader("content-type", "application/json")
                                    .end(new JsonObject()
                                            .put("code", 0).put("error", "update user info error").toString());
                        }
                    });
        } catch (Exception e) {
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("code", 0).put("error", "params type error").toString());
        }
    }

    /**
     * 从数据库中提练用户信息 name,url,gender,phone,personalitySignature
     */
    private static void getUserInfoFromDB(RoutingContext routingContext, final MongoClient mongoClient, String openId) {
        mongoClient.findOne("users", new JsonObject().put("openId", openId), new JsonObject(),
                res -> {
                    if (res.succeeded()) {
                        JsonObject resultObj = res.result();
                        JsonObject objectId = resultObj.getJsonObject("_id");
                        JsonObject wxUserInfo = resultObj.getJsonObject("wxUserInfo");
                        JsonObject userInfo = resultObj.getJsonObject("userInfo");
                        //地址信息
                        JsonObject addressInfo = resultObj.getJsonObject("address");
                        String province = "", city = "", county = "", town = "", detail = "";
                        if (addressInfo != null) {
                            province = addressInfo.getString("province", "");
                            city = addressInfo.getString("city", "");
                            county = addressInfo.getString("county", "");
                            town = addressInfo.getString("town", "");
                            detail = addressInfo.getString("detail", "");
                        } else if (wxUserInfo != null) {
                            province = wxUserInfo.getString("province", "");
                            city = wxUserInfo.getString("city", "");
                        }
                        JsonObject address = new JsonObject()
                                .put("province", province)
                                .put("city", city)
                                .put("county", county)
                                .put("town", town)
                                .put("detail", detail);

                        JsonObject user = new JsonObject();
                        if (userInfo == null) {
                            if (wxUserInfo == null) {
                                mongoClient.close();
                                routingContext.response()
                                        .putHeader("content-type", "application/json")
                                        .end(new JsonObject()
                                                .put("code", 0)
                                                .put("error", "not found user message")
                                                .toString());
                                return;
                            } else {
                                user.put("name", wxUserInfo.getString("name", ""))
                                        .put("url", wxUserInfo.getString("url", ""))
                                        .put("gender", wxUserInfo.getInteger("gender", 0));
                            }
                        } else {
                            String url = "";
                            String headPic = userInfo.getString("headPic");
                            if (headPic == null || headPic.isEmpty()) {
                                url = wxUserInfo != null ? wxUserInfo.getString("url", "") : "";
                            } else {
                                url = DownloadService.privateDownloadUrl(headPic);
                            }
                            String name = userInfo.getString("name");
                            if (name == null || name.isEmpty()) {
                                name = wxUserInfo != null ? wxUserInfo.getString("name", "") : "";
                            }
                            int gender = userInfo.getInteger("gender", -1);
                            if (gender == -1) {
                                gender = wxUserInfo != null ? wxUserInfo.getInteger("gender", 0) : 0;
                            }
                            user.put("name", name)
                                    .put("url", url)
                                    .put("gender", gender)
                                    .put("phone", userInfo.getLong("phone", 0L))
                                    .put("personalitySignature", userInfo.getString("personalitySignature", ""));
                        }
                        JsonObject data = new JsonObject();
                        String id = objectId.getString("$oid");
                        data.put("objectId", id).put("user", user).put("address", address);

                        //权限信息
                        int auth = resultObj.getInteger("auths", -1);
                        if (auth != -1){
                            Session session = routingContext.session();
                            session.put("auth", auth);
                            data.put("identity",  RoleMessage.getRoleMessage(auth));
                        }
                        mongoClient.close();
                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .end(new JsonObject()
                                        .put("code", 1)
                                        .put("data", data).toString());
                    } else {
                        mongoClient.close();
                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .end(new JsonObject()
                                        .put("code", 0)
                                        .put("error", "connect db error")
                                        .toString());
                    }
                });
    }
}
