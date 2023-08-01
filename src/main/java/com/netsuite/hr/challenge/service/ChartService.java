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
import java.util.stream.Collectors;

@RestController
public class ChartService
{
    @Autowired
    AthleteRepository athleteRepository;

    @Autowired
    ActivityRepository activityRepository;

    @RequestMapping("/athletechart")
    public String overallChart()
    {
        return createChart(null);
    }

    @RequestMapping("/athletechart/{month}")
    public String monthlyChart(@PathVariable() Integer month)
    {
        return createChart(month);
    }

    private String createChart(Integer month) {
        List<Athlete> athletes = athleteRepository.findAll();

        Map<String, BigDecimal> distancePerAthlete = new HashMap<>();

        for (Athlete athlete : athletes)
        {
            List<Activity> athleteActivities = activityRepository.findByAthleteId(athlete.id);

            if (month != null)
                athleteActivities = Filtering.filterByMonth(athleteActivities, month);

            BigDecimal calories = athleteActivities.stream().map(a -> a.distance).reduce(BigDecimal.ZERO, BigDecimal::add);
            distancePerAthlete.put(athlete.stravaId, calories);

        }

        List<Map.Entry<String, BigDecimal>> sorted = Sorting.sortDistance(distancePerAthlete);

        StringBuilder resultBuilder = createInitialCanvas();
        resultBuilder.append("""
                <script>
                  const canvas = document.getElementById('calories');
                """);

        resultBuilder.append("""
                  new Chart(canvas, {
                    type: 'bar',
                    data: {""");
        resultBuilder.append(getLabelsForChart(sorted));
        resultBuilder.append("""
                        datasets: [{
                        label: 'Distance',
                        """);
        resultBuilder.append(getDataForChart(sorted));
        resultBuilder.append("""
                      borderWidth: 1
                      }]
                    },
                    options: {
                      scales: {
                        y: {
                          beginAtZero: true
                        }
                      }
                    }
                  });
                """);

        resultBuilder.append("</script>");
        resultBuilder.append("<a href='/athletechart'>Overall leaderboard</a><br>");

        return resultBuilder.toString();
    }

    private String getDataForChart(List<Map.Entry<String, BigDecimal>> sorted) {
        return "data: [" +
        sorted.stream().map(Map.Entry::getValue)
                .map(BigDecimal::toString)
                .collect(Collectors.joining(","))
        + "],";
    }

    private String getLabelsForChart(List<Map.Entry<String, BigDecimal>> sorted) {
        return "labels: ["
                + sorted.stream().map(Map.Entry::getKey)
                .map(k -> athleteRepository.findByStravaId(k)).map(Athlete::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(","))
                +"],";
    }

    private StringBuilder createInitialCanvas()
    {
        return new StringBuilder("""
                <div>
                  <canvas id="calories"></canvas>
                </div>
                                
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                """);

    }
}
