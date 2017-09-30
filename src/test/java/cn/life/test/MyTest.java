package cn.life.test;

import org.junit.Test;

/**
 * Created by yejinyun on 2017/9/19.
 */
public class MyTest {

    @Test
    public void test() {
        String iconName = "ye.png";
        System.out.println(iconName.substring(0, iconName.indexOf(".")));
    }
}
