package com.clamos.signal.vo;

import lombok.Builder;
import lombok.Data;

@Data
public class MediaStatusVO {
    private String cmd;
    private String tag;
    private String id;
    private Integer status;

    @Builder
    public MediaStatusVO(String cmd, String tag, String id, Integer status) {
        this.cmd = cmd;
        this.tag = tag;
        this.id = id;
        this.status = status;
    }
}
