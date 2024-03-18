package com.clamos.signal.dto;

import lombok.Data;

import javax.persistence.*;

@Data
@MappedSuperclass
public class EventStartMangerItemDTO {
    @Id()
    @Column(name = "id")
    private Long eventId;
    @Column(name = "case_num")
    private String caseNum;
    @Column(name = "evt_no")
    private String evtNo;
    @Column(name = "code")
    private String lv;
    @Column(name = "evt_cl_cd")
    private String evtClCd;
    @Column(name = "evt_cl_cd_nm")
    private String evtClCdNm;
    @Column(name = "start_time")
    private Long ts;

}
