package com.netsuite.hr.challenge.service;

import com.netsuite.hr.challenge.dataobject.Activity;
import com.netsuite.hr.challenge.dataobject.Athlete;
import com.netsuite.hr.challenge.repository.ActivityRepository;
import com.netsuite.hr.challenge.repository.AthleteRepository;
import com.netsuite.hr.challenge.utilities.Filtering;
import com.netsuite.hr.challenge.utilities.Sorting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LeaderBoardService
{
    @Autowired
    AthleteRepository athleteRepository;

    @Autowired
    ActivityRepository activityRepository;

    @RequestMapping("/leaderboard")
    public String getLeaderboard()
    {
        return createLeaderboard(null);
    }

    @RequestMapping("/leaderboard/{month}")
    public String getLeaderboard(@PathVariable String month)
    {
        return createLeaderboard(month);
    }

    private String createLeaderboard(String month)
    {
        List<Athlete> athletes = athleteRepository.findAll();

        Map<Athlete, BigDecimal> distancePerAthlete = getDistancePerAthlete(month, athletes);
        Map<String, BigDecimal> distancePerTribe = getDistancePerTribe(distancePerAthlete);
        List<Map.Entry<String, BigDecimal>> leaderboard = Sorting.sortDistance(distancePerTribe);

        StringBuilder result = new StringBuilder();

        createTable(leaderboard, result);

        result.append("<a href='/leaderboard'>Overall leaderboard</a><br>");

        for (int i = 3; i<=12; i++)
        {
            result.append(String.format("<a href='/leaderboard/%d'>Month %d</a><br>", i, i));
        }

        return result.toString();
    }

    private Map<Athlete, BigDecimal> getDistancePerAthlete(String month, List<Athlete> athletes) {
        Map<Athlete, BigDecimal> caloriesPerAthlete = new HashMap<>();
        athletes.forEach(
                athlete ->
                {
                    BigDecimal totalCalories = countDistance(athlete, month != null ? Integer.parseInt(month) : null);
                    caloriesPerAthlete.put(athlete, totalCalories);
                }

        );
        return caloriesPerAthlete;
    }

    private Map<String, BigDecimal> getDistancePerTribe(Map<Athlete, BigDecimal> distancePerAthlete) {
        Map<String, BigDecimal> distancePerTribe = new HashMap<>();
        distancePerAthlete.forEach((athlete, calories) ->
                {
                    BigDecimal tribeCal = distancePerTribe.getOrDefault(athlete.tribe, BigDecimal.ZERO);
                    tribeCal = tribeCal.add(calories);
                    distancePerTribe.put(athlete.tribe, tribeCal);
                }
        );
        return distancePerTribe;
    }

    private void createTable(List<Map.Entry<String, BigDecimal>> leaderboard, StringBuilder result) {
        result.append("<table>");
        leaderboard.forEach(
                entry ->
                {
                    result.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
                }
        );
        result.append("</table>");
    }


    private BigDecimal countDistance(Athlete athlete, Integer month)
    {
        List<Activity> activities = activityRepository.findByAthleteId(athlete.id);
        List<Activity> filtered = activities;

        if (month != null)
        {
             filtered = Filtering.filterByMonth(activities, month);
        }

        return filtered.stream().map(a -> a.distance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
