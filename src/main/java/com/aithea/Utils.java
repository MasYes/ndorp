package com.aithea;

import de.jetwick.snacktory.ArticleTextExtractor;
import de.jetwick.snacktory.JResult;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by julian on 04.05.17.
 */
public class Utils {

    private final static Logger logger = Logger.getLogger(Utils.class);
    private static final String path = "/data/news/";
    private static int day;
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(System.currentTimeMillis()));
        day = calendar.get(Calendar.DAY_OF_MONTH);
        new File(path).mkdirs();
    }

    private Utils(){}


    public static String getFileName(Date date){
        if(date == null)
            date = new Date(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        if(day.length() == 1)
            day = "0" + day;
        if(month.length() == 1)
            month = "0" + month;
        return day + month + year;
    }

    public static String getTime(Date date){
        if(date == null)
            date = new Date(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = String.valueOf(calendar.get(Calendar.MINUTE));
        if(hour.length() == 1)
            hour = "0" + hour;
        if(minute.length() == 1)
            minute = "0" + minute;
        return hour + minute;
    }

    public static boolean newDayStarted(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(System.currentTimeMillis()));
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        if(currentDay == day)
            return false;
        day = currentDay;
        return true;
    }

    public static String formatDate(Date date){
        if(date == null)
            date = new Date(System.currentTimeMillis());
        return format.format(date);
    }

    public static OutputStream getStream(String date){
        try {
            OutputStream result = null;
            if(!new File(path + date + ".bz2").exists())
                result = new FileOutputStream(path + date + ".bz2");
            else
                for(int i = 1; i < 100; i++)
                    if(!new File(path + date + "_" + i + ".bz2").exists()) {
                        result = new FileOutputStream(path + date + "_" + i + ".bz2");
                        break;
                    }
            return new BZip2CompressorOutputStream(result);
        } catch (Exception ex){
            logger.error("Failed to create os", ex);
        }
        return System.out;
    }

    public static OutputStream getOneStream(){
        try {
            Date date = new Date(System.currentTimeMillis());
            String s1 = getFileName(date);
            String s2 = getTime(date);
            OutputStream result = new FileOutputStream(path + s1 + "_" + s2 + ".bz2");
            return new BZip2CompressorOutputStream(result);
        } catch (Exception ex){
            logger.error("Failed to create os", ex);
        }
        return System.out;
    }

    public static JResult extractContent(URL url) throws Exception {
        String html = Jsoup.connect(url.toString())
                .userAgent("Mozilla")
                .ignoreContentType(true)
                .timeout(30000).get().html();
        ArticleTextExtractor extractor = new ArticleTextExtractor();
        return extractor.extractContent(html);
    }

}
