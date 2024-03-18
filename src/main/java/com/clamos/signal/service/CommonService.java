package com.clamos.signal.service;

import com.clamos.signal.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.TemporalField;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CommonService {

    public String getUUID() {
        return RandomStringUtils.randomAlphanumeric(Constants.SHORT_ID_LENGTH);
    }

    public Long getNowSecond() {
        return Instant.now().getEpochSecond();
    }
    public Long getNowMilSecond() {return Instant.now().toEpochMilli();}

}
