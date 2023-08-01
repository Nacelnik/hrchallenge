package com.netsuite.hr.challenge.dataobject;

import com.mongodb.lang.Nullable;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

public class Activity
{
    @Id
    public String id;

    public String stravaId;

    public String athleteId;

    public BigDecimal distance;

    @Nullable
    public String startDate;

    public Activity(String stravaId, String athleteId, BigDecimal distance, String startDate)
    {
        this.stravaId = stravaId;
        this.athleteId = athleteId;
        this.distance = distance;
        this.startDate = startDate;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id='" + id + '\'' +
                ", stravaId='" + stravaId + '\'' +
                ", athleteId='" + athleteId + '\'' +
                ", distance=" + distance +
                '}';
    }
}
