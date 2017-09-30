package cn.life.qiniu;

import com.qiniu.util.Auth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by yejinyun on 2017/9/19.
 */
public class DownloadService {

    /**
     * 返回私密空间的下载连接
     */
    public static  String privateDownloadUrl(String fileName) {
        String encodedFileName = null;
        try {
            encodedFileName = URLEncoder.encode(fileName, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (encodedFileName == null) {
            return null;
        }
        String publicUrl = String.format("%s/%s", QiNiuConfig.DOMAIN_OF_BUCKET, encodedFileName);
        Auth auth = Auth.create(Keys.AK, Keys.SK);
        long expireInSeconds = 3600;//1小时，可以自定义链接过期时间
        String finalUrl = auth.privateDownloadUrl(publicUrl, expireInSeconds);
        return finalUrl;
    }
}
