package com.looky.api.survey;

import com.looky.api.support.response.ApiResponse;
import com.looky.api.survey.dto.CreateSurveyRequest;
import com.looky.api.survey.dto.CreateSurveyResponse;
import com.looky.api.survey.dto.SubmissionCompletedResponse;
import com.looky.api.survey.dto.SubmissionStartedResponse;
import com.looky.api.survey.dto.SubmitAnswersRequest;
import com.looky.api.survey.dto.SurveyResultResponse;
import com.looky.api.survey.dto.SurveyStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Survey", description = "설문 생성, 응답 시작, 응답 제출, 상태 조회 API")
public interface SurveyApi {

    @Operation(
            summary = "설문 생성",
            description = "닉네임으로 설문을 생성하고 공유용 surveyCode와 shareUrl을 반환합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "userNickname": "만두"
                                    }
                                    """)
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "설문 생성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": "success",
                                              "message": "설문이 생성되었습니다.",
                                              "payload": {
                                                "surveyCode": "b91k2p",
                                                "shareUrl": "https://looky.my/b91k2p",
                                                "userNickname": "만두",
                                                "surveyStatus": "DRAFT",
                                                "resultAvailableAt": "2026-06-24T03:00:00+09:00",
                                                "createdAt": "2026-06-23T03:00:00+09:00"
                                              }
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "요청값 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": "fail",
                                              "message": "요청값이 올바르지 않습니다.",
                                              "payload": {
                                                "errorCode": "VALIDATION_ERROR",
                                                "errors": [
                                                  {
                                                    "field": "userNickname",
                                                    "reason": "닉네임은 필수입니다."
                                                  }
                                                ]
                                              }
                                            }
                                            """)
                            )
                    )
            }
    )
    ResponseEntity<ApiResponse<CreateSurveyResponse>> createSurvey(CreateSurveyRequest request);

    @Operation(
            summary = "응답 시작",
            description = "POST /api/v1/surveys/{surveyCode}/submissions 경로로 응답 세션을 시작합니다. SELF 응답이 없으면 SELF, SELF 응답 이후에는 PEER 응답을 시작합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "응답 시작 성공"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "유효하지 않은 surveyCode"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "아직 PEER 응답을 받을 수 없는 설문"
                    )
            }
    )
    ResponseEntity<ApiResponse<SubmissionStartedResponse>> startSubmission(
            @Parameter(description = "설문 코드", example = "b91k2p") String surveyCode
    );

    @Operation(
            summary = "응답 제출",
            description = "POST /api/v1/submissions/{submissionId} 경로로 진행 중인 submissionId에 답변을 제출합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "응답 제출 성공"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "답변 검증 실패"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "제출 정보 없음")
            }
    )
    ResponseEntity<ApiResponse<SubmissionCompletedResponse>> submitAnswers(
            @Parameter(description = "제출 ID", example = "10") Long submissionId,
            SubmitAnswersRequest request
    );

    @Operation(
            summary = "설문 상태 조회",
            description = "surveyCode로 SELF 제출 여부, PEER 완료 수, 결과 상태를 조회합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 조회 성공"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 surveyCode")
            }
    )
    ResponseEntity<ApiResponse<SurveyStatusResponse>> getSurveyStatus(
            @Parameter(description = "설문 코드", example = "b91k2p") String surveyCode
    );

    @Operation(
            summary = "설문 결과 조회",
            description = "surveyCode로 결과 조회 상태를 반환합니다. 유효한 surveyCode라면 200으로 응답하며, payload.resultStatus를 확인합니다. quadrantImageUrls는 READY 상태일 때만 내려갑니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "결과 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "READY",
                                                    value = """
                                                            {
                                                              "status": "success",
                                                              "message": "설문 결과를 조회했습니다.",
                                                              "payload": {
                                                                "surveyCode": "b91k2p",
                                                                "resultStatus": "READY",
                                                                "quadrantImageUrls": {
                                                                  "OPEN": "https://cdn.looky.my/results/b91k2p/open.png",
                                                                  "BLIND": "https://cdn.looky.my/results/b91k2p/blind.png",
                                                                  "HIDDEN": "https://cdn.looky.my/results/b91k2p/hidden.png",
                                                                  "UNKNOWN": "https://cdn.looky.my/results/b91k2p/unknown.png"
                                                                },
                                                                "quadrantInterpretations": {
                                                                  "OPEN": "서로 알고 있는 강점",
                                                                  "BLIND": "타인이 먼저 발견하는 특성",
                                                                  "HIDDEN": "혼자 알고 있는 내면",
                                                                  "UNKNOWN": "아직 발견되지 않은 가능성"
                                                                },
                                                                "overall": {
                                                                  "keyword": "마음을 잘 여는 사람",
                                                                  "analysisTitle": "대화를 여는 다정한 기운",
                                                                  "analysisBody": "낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 주변도 금세 편하게 반응하고 흐름이 부드러워집니다. 끝나고 나면 따뜻한 여운이 오래 남습니다.",
                                                                  "tip": "앞장서다 혼자 짐을 다 안을 때가 있어요.\\n한 번만 속도 맞춰볼까요? 하고 먼저 물어보세요.\\n주변도 더 편하게 움직이고 관계가 오래 따뜻하게 돌아올 거예요."
                                                                },
                                                                "overallKeyword": "마음을 잘 여는 사람",
                                                                "overallAnalysis": "낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 주변도 금세 편하게 반응하고 흐름이 부드러워집니다. 끝나고 나면 따뜻한 여운이 오래 남습니다.",
                                                                "actionTip": "앞장서다 혼자 짐을 다 안을 때가 있어요.\\n한 번만 속도 맞춰볼까요? 하고 먼저 물어보세요.\\n주변도 더 편하게 움직이고 관계가 오래 따뜻하게 돌아올 거예요.",
                                                                "quadrants": {
                                                                  "OPEN": {
                                                                    "definitionKeyword": "탐험가",
                                                                    "adjectiveKeywords": ["탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지"],
                                                                    "interpretation": "서로가 알고 있는 호기심과 개방성입니다.",
                                                                    "imageUrl": "https://cdn.looky.my/results/b91k2p/open.png"
                                                                  }
                                                                }
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "WAITING",
                                                    value = """
                                                            {
                                                              "status": "success",
                                                              "message": "설문 결과를 조회했습니다.",
                                                              "payload": {
                                                                "surveyCode": "b91k2p",
                                                                "resultStatus": "COLLECTING_PEER_RESPONSES",
                                                                "quadrantImageUrls": null
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "GENERATING",
                                                    value = """
                                                            {
                                                              "status": "success",
                                                              "message": "설문 결과를 조회했습니다.",
                                                              "payload": {
                                                                "surveyCode": "b91k2p",
                                                                "resultStatus": "GENERATING",
                                                                "quadrantImageUrls": null
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "FAILED",
                                                    value = """
                                                            {
                                                              "status": "success",
                                                              "message": "설문 결과를 조회했습니다.",
                                                              "payload": {
                                                                "surveyCode": "b91k2p",
                                                                "resultStatus": "FAILED",
                                                                "quadrantImageUrls": null
                                                              }
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 surveyCode"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "결과 데이터 불일치 등 서버 내부 오류")
            }
    )
    ResponseEntity<ApiResponse<SurveyResultResponse>> getSurveyResult(
            @Parameter(description = "설문 코드", example = "b91k2p") String surveyCode
    );
}
