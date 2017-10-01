package cn.life.main;

import cn.life.auth.WxAuthCodeInterface;
import cn.life.config.Constant;
import cn.life.login.LoginInterface;
import cn.life.pageConfig.PageConfigInterface;
import cn.life.qiNiuPostService.QiNiuServiceInterface;
import cn.life.userinfo.UserInfoInterface;
import cn.life.util.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

/**
 * Created by yejy_ on 2017-09-10.
 */
public class Server extends AbstractVerticle {

    public static void main(String[] args) {
        Runner.runExample(Server.class);
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        //session for save User login message
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(BodyHandler.create());

        //运维管理页面
        router.route("/static/*").handler(StaticHandler.create());
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();
        TemplateHandler templateHandler = TemplateHandler.create(engine);

        router.get("/gg").handler(routingContext -> {
            Session session = routingContext.session();
            String openId = session.get(Constant.OPEN_ID_WEB);
            Integer auth = session.get(Constant.AUTH_WEB);
            if (openId != null && auth != null) {
                routingContext.reroute(HttpMethod.GET, "/web/gonggao.html");
                return;
            }
            routingContext.reroute("/web/index.html");
        });

        //验证授权码
        router.post("/authcode").handler(WxAuthCodeInterface::checkAuthCode);
        router.get("/authcode").handler(WxAuthCodeInterface::checkAuthCode);


        router.get("/web/*").handler(templateHandler);

        //----------------监听第三方回调开始--------------------------------
        //监听来自七牛服务器的信息
        router.post("/qiniu/picresult").consumes("application/json")
                .produces("application/json").handler(QiNiuServiceInterface::picUploadResult);
        //----------------监听第三方回调结束--------------------------------


        //----------------用户自定义接口开始--------------------------------

        //用户登录
        router.post("/wxlogin").consumes("application/json").produces("application/json").handler(LoginInterface::onLogin);
        //微信更新获取用户信息
        router.post("/userinfo").consumes("application/json")
                .produces("application/json").handler(UserInfoInterface::parseUserMessage);

        //获取用户信息
        router.post("/getuserinfo").consumes("application/json")
                .produces("application/json").handler(UserInfoInterface::getUserInfo);

        //更新用户信息
        router.post("/updateuserinfo").consumes("application/json")
                .produces("application/json").handler(UserInfoInterface::updateUserInfo);
        //用户获取上传凭证
        router.get("/file/uploadtoken").produces("application/json").handler(QiNiuServiceInterface::getUploadToken);
        //根据图片名，获取图片连接
        router.post("/file/getimageurl").produces("application/json").handler(QiNiuServiceInterface::getImageUrl);

        //获取页面配置信息
        router.get("/pageconfig").produces("application/json").handler(PageConfigInterface::pageMessage);

        //获取运营平台授权码
        router.get("/getauthcode").produces("application/json").handler(WxAuthCodeInterface::getAuthCode);
        //----------------用户自定义接口结束--------------------------------

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }


}
