package com.clamos.signal.vo;

import com.clamos.signal.constant.MessageCode;
import com.clamos.signal.vo.ResponseVO;
import com.clamos.signal.vo.SessionVO;
import lombok.*;

import java.util.List;

@Getter
@Setter
public class SessionListVO extends ResponseVO {
    private List<SessionVO> list;

    public SessionListVO(String cmd, String tag, MessageCode messageCode, List<SessionVO> list) {
        super(cmd, tag, messageCode);
        this.list = list;
    }
}

