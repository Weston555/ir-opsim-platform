package com.example.iropsim.sim;

import com.example.iropsim.entity.FaultInjection;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * 故障注入请求DTO
 */
@Data
public class FaultInjectionRequest {

    @NotNull(message = "故障类型不能为空")
    private FaultInjection.FaultType faultType;

    @NotNull(message = "开始时间不能为空")
    private Instant startTs;

    @NotNull(message = "结束时间不能为空")
    private Instant endTs;

    private JsonNode params; // 故障参数，如幅值、漂移率等
}
