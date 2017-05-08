package com.aithea;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.impl.basic.Item;
import de.nava.informa.parsers.FeedParser;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.Charsets;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.aithea.Utils.getFileName;
import static com.aithea.Utils.getTime;

/**
 * Created by julian on 04.05.17.
 */
public class Main {

    public static void main(String[] args) throws Exception{
        String s = "ОК";
        RSSMT rss = new RSSMT();
        if(!new File("/data").exists()){
            System.out.println("Data doesn't exist");
            TimeUnit.DAYS.sleep(100);
        }

//        RSSMT.validateRssList();

        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        out.println(s);


        long l = 0;
        while(true) {
            rss.parse();
            if(l++%2 == 0)
                rss.closeAllStreams();
            TimeUnit.SECONDS.sleep(120);
        }
    }


    public static void test() throws Exception {
        String path = "/home/julian/JavaProjects/news/data/data/news/";
        String line;
        HashSet<String> set = new HashSet<>();
        BufferedReader br = new BufferedReader(new FileReader(path + "06052017_1636"));
        while((line = br.readLine()) != null){
//            if(line.length() < 5)
//                continue;
            JSONObject json = new JSONObject(line);
//            if(!json.has("publisher"))
//                System.out.println(json + " " + line.length());
            if(set.add(json.getString("publisher"))){
                System.out.println(line);
            }
        }

    }



}
