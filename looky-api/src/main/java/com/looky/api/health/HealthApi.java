package com.looky.api.health;

import com.looky.api.health.dto.HealthResponse;
import com.looky.api.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Health", description = "서버 상태 확인 API")
public interface HealthApi {

    @Operation(
            summary = "헬스 체크",
            description = "서버가 요청을 처리할 수 있는지 확인합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "헬스 체크 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": "success",
                                              "message": "헬스 체크에 성공했습니다.",
                                              "payload": {
                                                "healthStatus": "UP"
                                              }
                                            }
                                            """)
                            )
                    )
            }
    )
    ResponseEntity<ApiResponse<HealthResponse>> health();
}
