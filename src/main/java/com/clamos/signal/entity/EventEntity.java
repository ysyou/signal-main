package com.clamos.signal.entity;

import com.clamos.signal.dto.EventStartMangerItemDTO;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "events")
public class EventEntity extends EventStartMangerItemDTO {
    @Column(name = "evt_cl_cd")
    private String evtClCd;
    @Column(name = "dspt_item_cd")
    private String dsptItemCd;
    @Column(name = "end_time")
    private Integer endTime;
}
