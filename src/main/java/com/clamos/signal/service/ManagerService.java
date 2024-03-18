package com.clamos.signal.service;

import com.clamos.signal.dto.ResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "pc")
public interface ManagerService {

    @GetMapping("/events/{id}")
    ResultDTO<Map<String, Object>> getEventById(@PathVariable("id") Long id);

    @GetMapping("/devices/{id}/config")
    ResultDTO<Map<String, Object>> getDeviceConfigById(@PathVariable("id") String id);

    @GetMapping("/devices/{id}/groupIds")
    ResultDTO<List<String>> getDeviceGroupIds(@PathVariable("id") String id);
}
