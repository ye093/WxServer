package cn.life.qiniu;

import cn.life.config.ServiceConfig;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;

/**
 * 生成客户端上传凭证
 */
public class UploadToken {

    /**
     * 生成凭证，fileName 不为null时，覆盖图片名
     */
    public static String generateOverrideToken(String fileName) {
        Auth auth = Auth.create(Keys.AK, Keys.SK);
        StringMap putPolicy = new StringMap();
        putPolicy.put("callbackUrl", ServiceConfig.BASE_SERVICE_URL + "qiniu/picresult");
        putPolicy.put("callbackBody",
                "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"bucket\":\"$(bucket)\",\"fsize\":$(fsize),\"objectid\":\"$(x:objectid)\",\"type\":\"$(x:type)\"}");
        putPolicy.put("callbackBodyType", "application/json");
        long expireSeconds = 3600;
        String upToken = auth.uploadToken(QiNiuConfig.YEJY_BUCKET, fileName, expireSeconds, putPolicy);
        return upToken;
    }


}
