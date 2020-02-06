package de.tub.trafficlight.controller.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntrusionDetectionHandler {

    private static final Logger logger = LogManager.getLogger(IntrusionDetectionHandler.class);

    private static Map<String, List<LocalDateTime>> evRequests = new HashMap<>();

    private static List<String> blacklist = new ArrayList<>();

    public static boolean isHonest(String username){
        if (username.isEmpty() || blacklist.contains(username)){
            logger.info("IDS denies access for user "+username);
            return false;
        }
        if(!evRequests.containsKey(username)){
            evRequests.put(username, new ArrayList<>());
            evRequests.get(username).add(LocalDateTime.now());
            logger.info("No past records for malicious behaviour for user "+username);
            return true;
        } else if (evRequests.get(username).size()<3){
            List<LocalDateTime> times = evRequests.get(username);
            times.add(LocalDateTime.now());
            evRequests.replace(username, times);
            logger.info("Warning: user "+username+" has now "+evRequests.get(username).size()+" records, user is on watchlist.");
            return true;
        } else {
            List<LocalDateTime> times = evRequests.get(username);
            for (LocalDateTime old : times){
                Duration timeElapsed = Duration.between(old, LocalDateTime.now());
                int diff = timeElapsed.compareTo(Duration.ofMinutes(Integer.toUnsignedLong(5)));
                if ( diff < 0){
                    logger.info("WARNING: Malicious Behaviour: User "+ username + " has been blacklisted.");
                    blacklist.add(username);
                    return false;
                } else {
                    times.remove(old);
                    evRequests.replace(username, times);
                }
            }
            return false;
        }
    }

    public static void resetBlacklist(){
        evRequests.clear();
        blacklist.clear();
    }
}
