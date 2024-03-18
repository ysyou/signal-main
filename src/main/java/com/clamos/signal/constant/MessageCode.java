package com.clamos.signal.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageCode {
    SUCCESS("",0),
    REDIS_MEDIA_DEVICE_URL_IS_NULL("휴대폰이 연결된 미디어 서버 주소가 존재하지 않습니다.",4033),
    REDIS_MEDIA_URL_IS_NULL("레디스에 미디어 서버 주소가 존재하지 않습니다.",4034),
    MEDIA_CANDIDATE_SESSION_IS_NULL("미디어 서버에 연결된 세션이 존재하지 않습니다.",4035),
    DUPLICATION_LOGIN("중복 로그인입니다.",4036),
    ALREADY_LOGIN("이미 로그인 되었습니다.",4037),
    DO_NOT_LOGIN("로그인이 필요합니다",401),
    STRUCTURE("규격에 문제가 있습니다",4038),
    ;
    private final String msg;
    private final Integer code;
}

