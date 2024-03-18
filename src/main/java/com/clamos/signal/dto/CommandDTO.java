package com.clamos.signal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CommandDTO {
    private String cmd;
    private String tag;

    @JsonProperty(defaultValue = "")
    private String id;
}
