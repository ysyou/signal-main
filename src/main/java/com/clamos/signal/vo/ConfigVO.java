package com.clamos.signal.vo;

import com.clamos.signal.constant.MessageCode;
import com.clamos.signal.vo.ResponseVO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigVO extends ResponseVO {
    private String id;
    private String rtsp;
    private String type;
    private Integer resolution;
    private Integer fps;
    private Boolean alchera;
    private Double lat;
    private Double lon;

    public ConfigVO(String cmd, String tag, MessageCode messageCode, String id, String rtsp, String type, Integer resolution, Integer fps, Boolean alchera, Double lat, Double lon) {
        super(cmd, tag, messageCode);
        this.id = id;
        this.rtsp = rtsp;
        this.type = type;
        this.resolution = resolution;
        this.fps = fps;
        this.alchera = alchera;
        this.lat = lat;
        this.lon = lon;
    }


}
