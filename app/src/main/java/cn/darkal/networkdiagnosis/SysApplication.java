package cn.darkal.networkdiagnosis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.CrashReport;

import net.gotev.uploadservice.UploadService;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.proxy.dns.AdvancedHostResolver;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Created by xuzhou on 2016/8/10.
 */
public class SysApplication extends MultiDexApplication {
    public static Boolean isInitProxy = false;
    public static int proxyPort = 8888;
    public BrowserMobProxy proxy;

    @Override
    public void onCreate() {
        super.onCreate();
        initProxy();
        // Gradle automatically generates proper variable as below.
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(getApplicationContext());
        strategy.setAppChannel("GitHub");
        Bugly.init(getApplicationContext(), "db9f598223", true);
    }

    public void initProxy() {
        try {
            FileUtils.forceMkdir(new File(Environment.getExternalStorageDirectory() + "/har"));
//            FileUtils.cleanDirectory(new File(Environment.getExternalStorageDirectory() + "/har"));
//            FileUtils.forceDelete(new File(Environment.getExternalStorageDirectory() + "/test.har"));
        } catch (IOException e) {
            // test.har文件不存在
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                startProxy();

                Intent intent = new Intent();
                intent.setAction("proxyfinished");
                sendBroadcast(intent);
            }
        }).start();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("~~~","onTerminate");
                proxy.stop();
            }
        }).start();
    }

    public void startProxy(){
        try {
            proxy = new BrowserMobProxyServer();
            proxy.setTrustAllServers(true);
            proxy.start(8888);
        } catch (Exception e) {
            // 防止8888已被占用
            Random rand = new Random();
            int randNum = rand.nextInt(1000) + 8000;
            proxyPort = randNum;

            proxy = new BrowserMobProxyServer();
            proxy.setTrustAllServers(true);
            proxy.start(randNum);
        }
        Log.e("~~~", proxy.getPort() + "");

        proxy.enableHarCaptureTypes(CaptureType.REQUEST_HEADERS, CaptureType.REQUEST_COOKIES,
                CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_HEADERS, CaptureType.REQUEST_COOKIES,
                CaptureType.RESPONSE_CONTENT);

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(new Date(System.currentTimeMillis()));
        proxy.newHar(time);

        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // 设置hosts
        if(shp.getString("system_host", "").length()>0){
            AdvancedHostResolver advancedHostResolver = proxy.getHostNameResolver();
            for (String temp : shp.getString("system_host", "").split("\\n")){
                if(temp.split(" ").length==2) {
                    advancedHostResolver.remapHost(temp.split(" ")[1],temp.split(" ")[0]);
                    Log.e("~~~~remapHost ",temp.split(" ")[1] +" " + temp.split(" ")[0]);
                }
            }
            proxy.setHostNameResolver(advancedHostResolver);
        }

        isInitProxy = true;
    }
}
