package com.aithea;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by julian on 04.05.17.
 */
public class Main {

    public static void main(String[] args) throws Exception{
        if(!new File("/data").exists()){
            System.out.println("Data doesn't exist");
            TimeUnit.DAYS.sleep(100);
        }
        System.out.println("OK");
        RSS rss = new RSS();
        while(true) {
            rss.parse();
            TimeUnit.MINUTES.sleep(5);
        }
//        rss.closeAllStreams();
    }

}
