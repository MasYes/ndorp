package com.aithea;

import de.jetwick.snacktory.JResult;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.utils.FeedManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.aithea.Utils.*;

/**
 * Created by julian on 04.05.17.
 */
public class RSS {

    private final static Logger logger = Logger.getLogger(RSS.class);
    private final static FeedManager manager = new FeedManager();
    private final static HashSet<String> feeds = new HashSet<>();
    private final static HashSet<URL> saved = new HashSet<>();
    private final static HashMap<String, OutputStream> streams = new HashMap<>();

    public RSS(){
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/rss.txt"));
            while((line = br.readLine()) != null)
                if(!line.startsWith("#"))
                    feeds.add(line);
        } catch (Exception ex){
            logger.error("File with RSS not found", ex);
        }
        for(String feed : feeds)
            try {
                manager.addFeed(feed);
            }
            catch (Exception ex){
                logger.error("problem with a feed occurred : " + feed, ex);
            }
        logger.info("RSS initialized, the number of feeds is " + feeds.size());
    }

    public void parse(){
        logger.info("Parsing started");
        for(String feed : feeds)
            try {
                logger.info(feed + " : start parsing");
                ChannelIF channel = manager.getFeed(feed).getChannel();
                for (ItemIF item : channel.getItems()) {
                    if (!saved.contains(item.getLink())) {
                        JSONObject json = getItemInfo(item);
                        getOutputStreamWriter(item.getDate()).println(json);
                        channel.removeItem(item);
                        saved.add(item.getLink());
                    }
                    TimeUnit.SECONDS.sleep((long)(Math.random()*5 + 2));
                }
            }catch (Exception ex){
                logger.error("Error with parsing " + feed, ex);
            }
            finally {
                logger.info(feed + " parsed");
            }
        logger.info("Parsing finished");
        if(Utils.newDayStarted())
            newDay();
    }

    void closeAllStreams(){
        for (String s : new HashSet<>(streams.keySet()))
            try {
                streams.get(s).close();
                streams.remove(s);
            } catch (Exception ex){
                logger.error("Failed to close a stream for" + s, ex);
            }
    }

    private void newDay(){
        logger.info("new day initialized");
        closeAllStreams();
        HashSet<URL> saved = new HashSet<>();
        try {
            for (String feed : feeds) {
                ChannelIF channel = manager.getFeed(feed).getChannel();
                for (ItemIF item : channel.getItems()) {
                    if (RSS.saved.contains(item.getLink())) {
                        saved.add(item.getLink());
                    }
                }
            }
        } catch (Exception ex){
            logger.error("Error with manager", ex);
        }
        RSS.saved.clear();
        RSS.saved.addAll(saved);
    }

    private static JSONObject getItemInfo(ItemIF item){
        try {
            JSONObject result = new JSONObject();
            JResult content = Utils.extractContent(item.getLink());
            result.put("pubDate", formatDate(item.getDate()));
            result.put("found", formatDate(item.getFound()));
            result.put("publisher", item.getChannel().getTitle());
            result.put("publisherLink", item.getChannel().getSite());
            result.put("title", item.getTitle());
            result.put("link", item.getLink());
            result.put("source", item.getSource());
            result.put("category", item.getCategories());
            result.put("description", item.getDescription());
            result.put("extractedTitle", content.getTitle());
            result.put("body", content.getText());
            result.put("extractedKeywords", content.getKeywords());
            result.put("extractedDescription", content.getDescription());
            result.put("extractedDate", content.getDate());
            result.put("guid", item.getGuid());
            logger.info(item.getLink() + " parsed successfully");
            return result;
        }catch (Exception ex){
            logger.info(item.getLink() + " parsed with errors");
            logger.error("Error in time of parsing the item", ex);
        }
        return new JSONObject();
    }

    private static PrintStream getOutputStreamWriter(Date date){
        String file = getFileName(date);
        if(!streams.containsKey(file))
            streams.put(file, getStream(file));
        return new PrintStream(streams.get(file));
    }

}
