package com.clamos.signal.dto;

import com.clamos.signal.constant.MessageCode;
import com.clamos.signal.vo.MediaListVO;
import com.clamos.signal.vo.ResponseVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class MediaListDTO{
    private String id;
    private Integer s;
    private Integer m;
}
