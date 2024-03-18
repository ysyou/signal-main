package com.clamos.signal.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SessionVO {
    private String id;
    private String sessionId;
    private String groupId;
    private List groupIds;
}
