package com.netsuite.hr.challenge.utilities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Sorting
{
    public static List<Map.Entry<String, BigDecimal>> sortDistance(Map<String, BigDecimal> distance) {
        List<Map.Entry<String, BigDecimal>> leaderboard = new ArrayList<>(distance.entrySet());
        leaderboard.sort(Map.Entry.comparingByValue());
        Collections.reverse(leaderboard);
        return leaderboard;
    }
}
