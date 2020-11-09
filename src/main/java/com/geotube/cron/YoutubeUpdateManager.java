package com.geotube.cron;

import com.geotube.model.TrendingInfo;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.I18nRegion;
import com.google.api.services.youtube.model.I18nRegionListResponse;
import com.google.api.services.youtube.model.VideoListResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@Log4j2
public class YoutubeUpdateManager {
    private Map<String, TrendingInfo> trendingInfoByCountry;
    private Map<String, Disposable> jobsByCountry;
    private Map<String, AtomicLong> countryWatchedByUser;
    private YouTube youtube;
    @Value("${youtube.secret}")
    private String key;


    public YoutubeUpdateManager() {
        // initialize hashmap on creation
        trendingInfoByCountry = new ConcurrentHashMap<>();
        jobsByCountry = new ConcurrentHashMap<>();
        countryWatchedByUser = new ConcurrentHashMap<>();
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), request -> {
            log.info("Request is " + request);
        }).setApplicationName("geoyoutube")
                .setYouTubeRequestInitializer(new YouTubeRequestInitializer(key)).build();
    }

    public TrendingInfo getTrendingInfoByCountry(String country) {
        if (trendingInfoByCountry.containsKey(country)) {
            return trendingInfoByCountry.get(country);
        }
        return null;
    }

    public List<String> getAllCountries() {
        I18nRegionListResponse regions = null;
        try {
            regions = youtube.i18nRegions().list("id").execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
       return regions.getItems().stream()
                .map(I18nRegion::getId).collect(Collectors.toList());
    }

    public void addTrendingInfoByCountry(String country) {
        countryWatchedByUser.putIfAbsent(country, new AtomicLong(1L));
        countryWatchedByUser.get(country).incrementAndGet();
        if (!trendingInfoByCountry.containsKey(country)) {
            // youtube trending videos get updated roughly every 15 minute
            Disposable subscribe = Flux.interval(Duration.ZERO, Duration.ofMinutes(15))
                    .flatMap(interval -> {
                        // make request to youtube api with certain country
                        Tuple2<String, String> mostLikedVideoAndComment = getMostLikedVideoAndComment(country);
                        String comment = mostLikedVideoAndComment.getT2();
                        String videoLink = mostLikedVideoAndComment.getT1();
                        TrendingInfo trendingInfo = new TrendingInfo("GE", 20L, videoLink, comment);
                        trendingInfoByCountry.put(country, trendingInfo);
                        return Mono.just(interval);
                    }).subscribe();
            jobsByCountry.put(country, subscribe);
        }
    }

    public void removeTrendingInfoByCountry(String country) {
        if (countryWatchedByUser.containsKey(country)) {
            long decrementCount = countryWatchedByUser.get(country).decrementAndGet();
            if (decrementCount <= 0) {
                countryWatchedByUser.remove(country);
                trendingInfoByCountry.remove(country);
                jobsByCountry.get(country).dispose();
                jobsByCountry.remove(country);
            }
        }
    }

    private Tuple2<String, String> getMostLikedVideoAndComment(String country) {
        VideoListResponse execute = null;
        try {
            execute = youtube.videos().list("id, snippet")
                    .setChart("mostPopular")
                    .setMaxResults(1L)
                    .setRegionCode(country)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String id = (String)execute.getItems().get(0).get("id");
        String video = "https://www.youtube.com/watch?v=" + id;
        AtomicReference<String> mostLikedComment =  new AtomicReference<>();
        VideoListResponse finalExecute = execute;
        execute.getItems().forEach(item -> {
            CommentThreadListResponse executeComment = null;
            try {
                YouTube.CommentThreads.List list = youtube.commentThreads().list("id, snippet")
                        .setVideoId(item.getId())
                        .setMaxResults(50L);

                executeComment = list.execute();
                long count = 0;
                while (executeComment.getNextPageToken() != null && !finalExecute.getNextPageToken().isEmpty()) {
                    Tuple2<String, Long> commentXLikeCount = filterComments(executeComment.getItems());
                    if (Math.max(count, commentXLikeCount.getT2()) > count) {
                        mostLikedComment.set(commentXLikeCount.getT1());
                        count = commentXLikeCount.getT2();
                    }
                    executeComment = list.setPageToken(executeComment.getNextPageToken()).execute();
                }
                Tuple2<String, Long> lastRequest = filterComments(executeComment.getItems());
                if (lastRequest.getT2() > count) {
                    mostLikedComment.set(lastRequest.getT1());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return Tuples.of(video, mostLikedComment.get());
    }

    private Tuple2<String, Long> filterComments(List<CommentThread> items) {
        AtomicLong likeCount = new AtomicLong(items.get(0).getSnippet().getTopLevelComment().getSnippet().getLikeCount());
        AtomicReference<String> text = new AtomicReference<>(items.get(0).getSnippet().getTopLevelComment().getSnippet().getTextDisplay());
        items.forEach(e -> {
            if (likeCount.get() < e.getSnippet().getTopLevelComment().getSnippet().getLikeCount()) {
                likeCount.set(e.getSnippet().getTopLevelComment().getSnippet().getLikeCount());
                text.set(e.getSnippet().getTopLevelComment().getSnippet().getTextDisplay());
            }
        });
        return Tuples.of(text.get(), likeCount.get());
    }
}
