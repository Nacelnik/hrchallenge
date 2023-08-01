package com.netsuite.hr.challenge.repository;

import com.netsuite.hr.challenge.dataobject.Athlete;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AthleteRepository extends MongoRepository<Athlete, String>
{
    Athlete findByStravaId(String stravaId);
}
