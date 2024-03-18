package com.clamos.signal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class WebLoginDTO extends CommandDTO {
    private String groupId;
    private Boolean force;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String sessionId;
}
