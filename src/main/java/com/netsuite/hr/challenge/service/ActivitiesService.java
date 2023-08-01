package com.netsuite.hr.challenge.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netsuite.hr.challenge.dataobject.Activity;
import com.netsuite.hr.challenge.dataobject.Athlete;
import com.netsuite.hr.challenge.repository.ActivityRepository;
import com.netsuite.hr.challenge.repository.AthleteRepository;
import com.netsuite.hr.challenge.utilities.Filtering;
import com.netsuite.hr.challenge.utilities.JsonBodyHandler;
import com.netsuite.hr.challenge.utilities.Sorting;
import com.netsuite.hr.challenge.utilities.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class ActivitiesService
{
    public static final int TOO_MANY_REQUESTS = 429;
    @Autowired
    AthleteRepository athleteRepository;

    @Autowired
    ActivityRepository activityRepository;

    private static final String ACTIVITIES = "https://www.strava.com/api/v3/activities";
    private static final Set<String> SUPPORTED_ACTIVITIES = Set.of("Hike", "Run", "Walk");

    @RequestMapping("/refresh/{month}")
    public String refreshActivities(@PathVariable() Integer month) throws IOException, InterruptedException
    {
        // 1 all athletes
        // for each: check if token is ok, if not, get refresh token
        // get activities
        // diff against DB
        // get and save new activities
        List<Athlete> athletes = athleteRepository.findAll();

        StringBuilder resultBuilder = new StringBuilder();

        for (Athlete athlete : athletes)
        {
            if (!athlete.active)
                continue;

            var client = HttpClient.newHttpClient();

            if (athlete.tokenExpiration() == null || athlete.tokenExpiration().isBefore(LocalDateTime.now()))
            {
                System.out.println(athlete.tokenExpiration());

                var tokenRequest = HttpRequest.newBuilder(
                        URI.create(AuthorizationService.STRAVA_TOKEN))
                        .POST(HttpRequest.BodyPublishers.ofString(
                            UrlUtil.getFormDataAsString(Map.of(
                                        "client_id",   AuthorizationService.CLIENT_ID,
                                        "client_secret", "ba680a1314ceb669fdfacacc246d732e7c49598a",
                                        "refresh_token", Objects.requireNonNull(athlete.refreshToken),
                                        "grant_type", "refresh_token"
                                ))
                        ))
                        .header("Accept", "application/json")
                        .build();

                var response = client.send(tokenRequest, new JsonBodyHandler<>(TokenRefresh.class));
                System.out.println(response.body());

                System.out.println(response.body().access_token);
                System.out.println(response.body().refresh_token);
                System.out.println(response.body().expires_at);

                athlete.accessExpiration = Long.valueOf(response.body().expires_at);
                athlete.accessToken = response.body().access_token;
                athlete.refreshToken = response.body().refresh_token;
                athleteRepository.save(athlete);
            }

            LocalDate monthDate = LocalDate.of(2023, month, 1);
            if (monthDate.isAfter(LocalDate.now()))
                return "There can be no future activities";

            String requestUrl = ACTIVITIES + "?access_token=" + athlete.accessToken
                    + "&per_page=200"
                    + "&page=%d&after="
                    + monthDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
                    + "&before="
                    + (monthDate.plusMonths(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) - 1);

            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            for (int i = 1; i < 6; i++) {
                var activitiesRequest = HttpRequest.newBuilder(
                                URI.create(String.format(requestUrl, i))
                        )
                        .build();

                var response = client.send(activitiesRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == TOO_MANY_REQUESTS)
                    return "Requests limit reached, exiting.<br/>" + resultBuilder;

                if (response.statusCode() != 200)
                    continue;

                ActivitiesResponse[] activitiesResponses = objectMapper.readValue(response.body(), ActivitiesResponse[].class);

                for (ActivitiesResponse activityResponse : activitiesResponses) {
                    Activity activity = activityRepository.findByStravaId(activityResponse.id);
                    if (activity == null) {
                        getAndSaveActivity(athlete, activityResponse.id, activityResponse.sport_type, activityResponse.distance, activityResponse.start_date_local);
                        resultBuilder.append("Found new activity ")
                                .append(activityResponse.id)
                                .append(" for athlete ")
                                .append(athlete.firstName)
                                .append(" ").
                                append(athlete.lastName)
                                .append("<br/>");
                    }
                }

                // if we didn't get 200 activities, don't request anything
                if (activitiesResponses.length < 200)
                {
                    System.out.println("No more activities, exiting loop at " + i);
                    break;
                }
            }
        }

        resultBuilder.append("Finished successfully<br/>");
        return resultBuilder.toString();
    }

    @RequestMapping("/activities")
    public String listActivities()
    {
        return createLeaderboard(null);
    }

    @RequestMapping("/activities/{month}")
    public String listMonthlyActivities(@PathVariable Integer month)
    {
        return createLeaderboard(month);
    }

    private String createLeaderboard(Integer month) {
        StringBuilder result = new StringBuilder();

        result.append("<table>");

        List<Athlete> athletes = athleteRepository.findAll();

        Map<String, BigDecimal> distancePerAthlete = new HashMap<>();
        Map<String, Integer> activitiesPerAthlete = new HashMap<>();

        for (Athlete athlete : athletes)
        {
            List<Activity> athleteActivities = activityRepository.findByAthleteId(athlete.id);

            if (month != null)
                athleteActivities = Filtering.filterByMonth(athleteActivities, month);

            BigDecimal calories = athleteActivities.stream().map(a -> a.distance).reduce(BigDecimal.ZERO, BigDecimal::add);
            distancePerAthlete.put(athlete.stravaId, calories);
            activitiesPerAthlete.put(athlete.stravaId, athleteActivities.size());
        }

        List<Map.Entry<String, BigDecimal>> sorted = Sorting.sortDistance(distancePerAthlete);

        int position = 1;
        for (Map.Entry<String, BigDecimal> entry : sorted)
        {
            Athlete athlete = athleteRepository.findByStravaId(entry.getKey());
            createResultForAthlete(result, position++, athlete, activitiesPerAthlete.get(entry.getKey()), entry.getValue());
        }

        result.append("</table>");

        result.append("<a href='/activities'>Overall leaderboard</a><br>");

        return result.toString();
    }

    private void createResultForAthlete(StringBuilder result, int position, Athlete athlete, Integer activitiesCount, BigDecimal distance) {
        result.append("<tr>");
        result.append("<td>");
        result.append(position);
        result.append("</td><td>");
        result.append(athlete.name());
        result.append("</td><td>");
        result.append(athlete.tribe);
        result.append("</td><td>");
        result.append(activitiesCount);
        result.append("</td><td>");
        result.append(distance);
        result.append("</td></tr>");
    }

    private void getAndSaveActivity(Athlete athlete, String id, String sport_type, String distance, String start_date_local) {
        if (!isSupportedType(sport_type) || distance == null)
            return;

        var activity = new Activity(id, athlete.id, new BigDecimal(distance).divide(BigDecimal.valueOf(1000), RoundingMode.HALF_UP) , stringToDate(start_date_local));
        activityRepository.save(activity);
    }

    private boolean isSupportedType(String sport_type) {
        return SUPPORTED_ACTIVITIES.contains(sport_type);
    }

    private static String stringToDate(String dateAsString) {
        if (dateAsString != null)
        {
            LocalDate date = LocalDate.parse(dateAsString.split("T")[0]);
            return date.format(DateTimeFormatter.ISO_DATE);
        }
        return null;
    }

    private static class ActivitiesResponse
    {
        public String id;
        public String sport_type;
        public String distance;
        public String start_date_local;
    }

    private static class TokenRefresh
    {
        public String access_token;
        public String expires_at;
        public String refresh_token;
    }
}
