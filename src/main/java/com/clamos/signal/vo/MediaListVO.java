package com.clamos.signal.vo;

import com.clamos.signal.constant.MessageCode;
import com.clamos.signal.dto.MediaListDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MediaListVO extends ResponseVO {
    private List<MediaListDTO> list;

    public MediaListVO(String cmd, String tag, MessageCode messageCode, List<MediaListDTO> list) {
        super(cmd, tag, messageCode);
        this.list = list;
    }
}
