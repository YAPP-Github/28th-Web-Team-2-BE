package com.looky.api.health;

import com.looky.api.health.dto.HealthResponse;
import com.looky.api.support.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController implements HealthApi {

    @Override
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthResponse>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                "헬스 체크에 성공했습니다.",
                new HealthResponse("UP")
        ));
    }
}
