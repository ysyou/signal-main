package com.clamos.signal.vo;

import com.clamos.signal.constant.MessageCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ResponseVO<T> implements Serializable {
    private String cmd;
    private String tag;
    private Integer code;
    private String msg;

    public ResponseVO(String cmd, String tag, MessageCode messageCode) {
        this(messageCode.getMsg(),messageCode.getCode());
        this.cmd = cmd;
        this.tag = tag;
    }

    public ResponseVO(String msg, Integer code) {
        this.msg = msg;
        this.code = code;
    }

    public ResponseVO(String cmd, String tag, Integer code, String msg) {
        this.cmd = cmd;
        this.tag = tag;
        this.code = code;
        this.msg = msg;
    }
}
