package cn.life.dbhelper;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * Created by yejy_ on 2017-09-24.
 */
public class MongoDBManager {
    private static final String DB_NAME = "weiXinPro";
    private static final String WRITE_USER = "readWriteUser";
    private static final String WRITE_PWD = "yejy093";

    private static final String READ_USER = "readUser";
    private static final String READ_PWD = "yejy093";

    public static MongoClient getWriteClient(Vertx vertx) {
        JsonObject config = new JsonObject()
                .put("host", "127.0.0.1")
                .put("port", 27017)
                .put("db_name", DB_NAME)
                .put("useObjectId", true)
                .put("connection_string", "mongodb://127.0.0.1:27017")
                .put("username", WRITE_USER)
                .put("password", WRITE_PWD)
                .put("authSource", DB_NAME);
        return MongoClient.createShared(vertx, config, "VERTX_CLIENT");
    }

    public static MongoClient getReadClient(Vertx vertx) {
        JsonObject config = new JsonObject()
                .put("host", "127.0.0.1")
                .put("port", 27017)
                .put("db_name", DB_NAME)
                .put("useObjectId", true)
                .put("connection_string", "mongodb://localhost:27017")
                .put("username", READ_USER)
                .put("password", READ_PWD)
                .put("authSource", "weiXinPro");
        return MongoClient.createShared(vertx, config, "VERTX_CLIENT");
    }
}
