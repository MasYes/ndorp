package com.aithea;

import de.jetwick.snacktory.JResult;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.Category;
import de.nava.informa.impl.basic.Channel;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.aithea.Utils.formatDate;
import static com.aithea.Utils.getFileName;
import static com.aithea.Utils.getOneStream;

/**
 * Created by julian on 05.05.17.
 */
public class RSSMT {

    private final static Logger logger = Logger.getLogger(RSS.class);
    private final static HashSet<String> feeds = new HashSet<>();
    private final static HashSet<String> saved = new HashSet<>();
    private final static HashMap<String, OutputStream> streams = new HashMap<>();
    private static OutputStream stream;
    private final static long execLimit = 30*60*1000;


    public RSSMT(){
        String line;
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(this.getClass().getClassLoader()
                            .getResourceAsStream("rss.txt")));
            while((line = br.readLine()) != null)
                if(!line.startsWith("#")) {
                    feeds.add(line);
                }
        } catch (Exception ex){
            logger.error("File with RSS not found", ex);
        }
        for(String feed : feeds)
            try {
//                new Channel(feed);
            }
            catch (Exception ex){
                logger.error("problem with a feed occurred : " + feed, ex);
            }
        logger.info("RSS initialized, the number of feeds is " + feeds.size());
    }

    public void parse(){
        logger.info("Parsing started");
        ArrayList<Downloader> downloaders = new ArrayList<>();
        for(String feed : feeds)
            downloaders.add(new Downloader(feed));
        int completed = 0;
        int running = 0;
        while(completed < downloaders.size()){
            for(Downloader downloader : downloaders){
                try {
                    if (!downloader.started) {
                        if (running < 100) {
                            downloader.setDaemon(true);
                            downloader.start();
                            downloader.started = true;
                            running++;
                        }
                    } else if (!downloader.isAlive() && !downloader.processed) {
                        for (JSONObject json : downloader.result) {
                            try {
                                if (json.has("fileName")) {
                                    getOutputStreamWriter(json.getString("fileName"))
                                            .println(new String(json.toString().getBytes("UTF-8"), "UTF-8"));
                                    saved.add(json.getString("link"));
                                }
                            } catch (Exception ex) {
                                logger.error("Error while saving result", ex);
                            }
                        }
                        downloader.result = null;
                        downloader.processed = true;
                        completed++;
                        running--;
                    } else if (downloader.isAlive()){
                        if(System.currentTimeMillis() - downloader.created > execLimit){
                            downloader.interrupt();
                            TimeUnit.SECONDS.sleep(60);
                            if(downloader.isAlive()) {
                                downloader.stop();
                                logger.error("Feed hard stop: " + downloader.feed);
                            }
                            logger.info("Feed hard interrupted: " + downloader.feed);
                            TimeUnit.SECONDS.sleep(10);
                            if(downloader.isAlive())
                                throw new IllegalStateException("THIS SHIT DOESN'T WANT TO STOP!!!");
                        }
                    }
                } catch (Exception ex){
                    logger.error("Problem with the feed: " + downloader.feed, ex);
                    downloader.result = null;
                    downloader.processed = true;
                    completed++;
                    running--;
                }
            }
        }
        logger.info("Parsing finished");
        if(Utils.newDayStarted())
            newDay();
    }

    void closeAllStreams(){
//        for (String s : new HashSet<>(streams.keySet()))
//            try {
//                streams.get(s).close();
//                streams.remove(s);
//            } catch (Exception ex){
//                logger.error("Failed to close a stream for" + s, ex);
//            }

            try {
                stream.close();
                stream = null;
            } catch (Exception ex){
                logger.error("Failed to close the stream", ex);
            }

        logger.info("all streams closed");
    }

    private void newDay(){
        logger.info("new day initialized");
        closeAllStreams();
        HashSet<String> saved = new HashSet<>();
        try {
            for (String feed : feeds) {
                ChannelIF channel = FeedParser.parse(new ChannelBuilder(), feed);
                for (ItemIF item : channel.getItems()) {
                    if (RSSMT.saved.contains(String.valueOf(item.getLink()))) {
                        saved.add(String.valueOf(item.getLink()));
                    }
                }
            }
        } catch (Exception ex){
            logger.error("Error with manager", ex);
        }
        RSSMT.saved.clear();
        RSSMT.saved.addAll(saved);
    }

    private static JSONObject getItemInfo(ItemIF item){
        try {
//            logger.info(item.getLink() + " : start parsing");
            JSONObject result = new JSONObject();
            result.put("fileName", getFileName(item.getDate()));
            result.put("pubDate", formatDate(item.getDate()));
            result.put("found", formatDate(item.getFound()));
            result.put("publisher", item.getChannel().getTitle());
            result.put("author", item.getCreator());
            result.put("publisherLink", item.getChannel().getSite());
            result.put("title", item.getTitle());
            result.put("link", String.valueOf(item.getLink()));
            result.put("source", item.getSource());
            if(item.getEnclosure() != null)
                try {
                    result.put("image", item.getEnclosure().getLocation());
                }catch (Exception ex){

                }
            StringBuilder categories = new StringBuilder();
            for(Object o : item.getCategories()) {
                Category category = (Category) o;
                if(categories.length() > 0)
                    categories.append(", ");
                categories.append(category.getTitle());
            }
            result.put("category", categories.toString());
            result.put("description", item.getDescription());
            if(item.getLink() != null && item.getLink().toString().length() > 4) {
                JResult content = Utils.extractContent(item.getLink());
                result.put("extractedTitle", content.getTitle());
                result.put("body", content.getText());
                result.put("extractedKeywords", content.getKeywords());
                result.put("extractedDescription", content.getDescription());
                result.put("extractedDate", content.getDate());
            }
//            result.put("guid", item.getGuid());
//            logger.info(item.getLink() + " parsed successfully");
            return result;
        }catch (Exception ex){
            logger.info(item.getLink() + " parsed with errors");
            logger.error("Error in time of parsing the item: " + ex.getMessage());
        }
        return new JSONObject();
    }

    private static PrintStream getOutputStreamWriter(Date date) throws Exception {
        return getOutputStreamWriter(getFileName(date));
    }

    private static PrintStream getOutputStreamWriter(String file) throws Exception {
//        if(!streams.containsKey(file))
//            streams.put(file, getStream(file));
//        return new PrintStream(streams.get(file));
        if(stream == null)
            stream = getOneStream();
        return new PrintStream(stream, true, "UTF-8");
    }

    public static void validateRssList(){
        String line;
        HashSet<String> feeds = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(RSSMT.class.getClassLoader()
                            .getResourceAsStream("rss.txt")));
            while((line = br.readLine()) != null)
                if(!line.startsWith("#")) {
                    if(feeds.add(line))
                        try{
                            FeedParser.parse(new ChannelBuilder(), line);
                        } catch (Exception ex){
                            try{
                                line = "https" + line.substring(4);
                                FeedParser.parse(new ChannelBuilder(), line);
                                logger.error("Use https for the feed: " + line);
                            }catch (Exception ex2) {
                                logger.error("A problem with the feed: " + line);
                                continue;
                            }
                        }
                    else {
                        logger.error("The feed repeats: " + line);
                        continue;
                    }
//                    logger.info("The feed is OK: " + line);
                }
        } catch (Exception ex){
            logger.error("File with RSS not found", ex);
        }
    }

    public static class Downloader extends Thread {

        String feed;
        ArrayList<JSONObject> result;
        boolean started;
        boolean processed;
        long created;

        Downloader(String feed){
            result = new ArrayList<>();
            this.feed = feed;
            started = false;
            processed = false;
            created = System.currentTimeMillis();
        }

        public void run(){
            started = true;
            try {
                logger.info(feed + " : start parsing the feed");
                ChannelIF channel = FeedParser.parse(new ChannelBuilder(), feed);
                for (ItemIF item : channel.getItems()) {
                    if (!saved.contains(String.valueOf(item.getLink()))) {
                        JSONObject json = getItemInfo(item);
                        json.put("feed", feed);
                        result.add(json);
                    }
                    if(1 > 0)
                        break;
                    TimeUnit.SECONDS.sleep((long)(Math.random()*5 + 2));
                }
            }catch (Exception ex){
                logger.error("Error with parsing " + feed + "  " + ex.getMessage());
            }
            finally {
                logger.info(feed + " : parsed");
            }
        }

    }

}
