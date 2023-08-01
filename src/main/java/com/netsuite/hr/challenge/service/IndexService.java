package com.netsuite.hr.challenge.service;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexService
{

    @RequestMapping("/")
    public String home()
    {
        return """
                <a href="/adduser">Authorize new user</a><br/>
                <a href="/activities">List distance per athlete</a><br/>
                <a href="/athletechart">Overall leaderboard</a><br/>
                """
                ;
    }
}
