package com.aithea;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.aithea.Utils.getFileName;
import static com.aithea.Utils.getTime;

/**
 * Created by julian on 04.05.17.
 */
public class Main {

    public static void main(String[] args) throws Exception{
        String s = "ОК";
        System.setProperty("file.encoding", "UTF-8");
        System.out.println(s);
        System.out.println(new String(s.getBytes("UTF-8"), "UTF-8"));
        if(!new File("/data").exists()){
            System.out.println("Data doesn't exist");
            TimeUnit.DAYS.sleep(100);
        }
        TimeUnit.DAYS.sleep(100);
        RSSMT rss = new RSSMT();

        int l = 0;
        while(true) {
            rss.parse();
            if(l++%5 == 0)
                rss.closeAllStreams();
            TimeUnit.SECONDS.sleep(60);
        }
//        rss.closeAllStreams();
    }


}
