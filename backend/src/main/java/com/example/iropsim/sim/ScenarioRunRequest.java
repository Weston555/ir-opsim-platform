package com.example.iropsim.sim;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * 仿真运行请求DTO
 */
@Data
public class ScenarioRunRequest {

    @NotNull(message = "场景ID不能为空")
    private UUID scenarioId;

    @NotNull(message = "运行模式不能为空")
    private String mode; // REALTIME 或 REPLAY

    @Min(value = 1, message = "采样频率至少为1Hz")
    private Integer rateHz = 1;

    private Long seed; // 可选，用于保证可复现
}
