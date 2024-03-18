package com.clamos.signal.dto;

import lombok.Data;

@Data
public class AlcheraDTO {

    Long matchId;
    String matchImgUrl;
    String matchBox;
    Float distance;

    Long targetId;
    String targetName;
    String targetType;
    String targetImgUrl;
    String targetBox;
    String sex;
    String ageNow;
    String age;
    String occurDate;
    String occurAddress;
    String alldressingDscd;
    String height;
    String manualRegistration;

    String deviceId;
    String deviceName;
    Double lat;
    Double lng;
    Long timestamp;
}
