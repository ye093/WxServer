package cn.life.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.util.Auth;

/**
 * Created by yejinyun on 2017/9/19.
 */
public class QNFileManager {

    public static final void deleteFile(String bucket, String key) {
        Configuration cfg = new Configuration(Zone.zone0());
        Auth auth = Auth.create(Keys.AK, Keys.SK);
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            bucketManager.delete(bucket, key);
        } catch (QiniuException ex) {
            //如果遇到异常，说明删除失败
            System.err.println(ex.code());
            System.err.println(ex.response.toString());
        }
    }
}
