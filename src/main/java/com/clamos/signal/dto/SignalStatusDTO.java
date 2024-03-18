package com.clamos.signal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class SignalStatusDTO {
    private String cmd;
    private String tag;
    private String id;
    private Integer status;
    private List groupIds;

    @Builder
    public SignalStatusDTO(String cmd, String tag, String id, Integer status, List groupIds) {
        this.cmd = cmd;
        this.tag = tag;
        this.id = id;
        this.status = status;
        this.groupIds = groupIds;
    }
}
