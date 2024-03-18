package com.clamos.signal.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmpowermentDTO {

    String id;
    String groupId;
    List<String> groupIds;
}
