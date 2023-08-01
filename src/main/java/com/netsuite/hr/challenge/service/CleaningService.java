package com.netsuite.hr.challenge.service;

import com.netsuite.hr.challenge.repository.ActivityRepository;
import com.netsuite.hr.challenge.repository.AthleteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CleaningService {

    @Autowired
    AthleteRepository athleteRepository;

    @Autowired
    ActivityRepository activityRepository;

    @RequestMapping("/clear")
    public String clear()
    {
        activityRepository.deleteAll();
        athleteRepository.deleteAll();

        return "Successfully cleared db";
    }

    @RequestMapping("/clearactivities")
    public String clearActivities()
    {
        activityRepository.deleteAll();

        return "Successfully cleared activities";
    }

}
