package com.clamos.signal.service;

import com.clamos.signal.dto.SessionDTO;
import com.clamos.signal.vo.SessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CommonHandlerService {
    public static List<SessionVO> sessionList(List<SessionDTO> sessionList) {
        List<SessionVO> sessionDtoList = new ArrayList<>();
        for (SessionDTO sessionDto : sessionList) {
            SessionVO sessionVO = new SessionVO(sessionDto.getId(),sessionDto.getSession().getId(),sessionDto.getGroupId(),sessionDto.getGroupIds());
            sessionDtoList.add(sessionVO);
        }
        return sessionDtoList;
    }
}
