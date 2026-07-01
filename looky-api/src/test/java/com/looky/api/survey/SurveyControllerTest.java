package com.looky.api.survey;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.result.application.ResultOverviewRecord;
import com.looky.result.application.ResultQueryService;
import com.looky.result.domain.ResultGenerationPhase;
import com.looky.survey.application.SurveyService;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.CreateSurveyCommand;
import com.looky.survey.application.dto.SubmissionCompletedResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import com.looky.survey.application.dto.SurveyCreatedResult;
import com.looky.survey.application.dto.SurveyResultQuadrant;
import com.looky.survey.application.dto.SurveyResultResult;
import com.looky.survey.application.dto.SurveyStatusResult;
import com.looky.submission.domain.SubmissionStatus;
import com.looky.submission.domain.SubmitterType;
import com.looky.survey.domain.ResultStatus;
import com.looky.survey.domain.SurveyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SurveyController.class)
class SurveyControllerTest {

    private static final String SURVEY_CODE = "b91k2p";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-23T03:00:00+09:00");
    private static final OffsetDateTime RESULT_AVAILABLE_AT = OffsetDateTime.parse("2026-06-24T03:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SurveyService surveyService;

    @MockitoBean
    private ResultQueryService resultQueryService;

    @Test
    void surveyControllerImplementsSurveyApi() {
        assertTrue(SurveyApi.class.isAssignableFrom(SurveyController.class));
    }

    @Test
    void createSurveyReturnsApprovedContract() throws Exception {
        given(surveyService.createSurvey(new CreateSurveyCommand("만두")))
                .willReturn(createdSurvey("만두"));

        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userNickname\":\"만두\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/surveys/" + SURVEY_CODE + "/status"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("설문이 생성되었습니다."))
                .andExpect(jsonPath("$.payload.surveyCode").value(SURVEY_CODE))
                .andExpect(jsonPath("$.payload.shareUrl").value("https://looky.my/" + SURVEY_CODE))
                .andExpect(jsonPath("$.payload.userNickname").value("만두"))
                .andExpect(jsonPath("$.payload.surveyStatus").value("DRAFT"))
                .andExpect(jsonPath("$.payload.resultAvailableAt").value("2026-06-24T03:00:00+09:00"))
                .andExpect(jsonPath("$.payload.createdAt").value("2026-06-23T03:00:00+09:00"))
                .andExpect(jsonPath("$.payload.surveyId").doesNotExist())
                .andExpect(jsonPath("$.payload.ownerCode").doesNotExist())
                .andExpect(jsonPath("$.payload.shareCode").doesNotExist())
                .andExpect(jsonPath("$.payload.ownerUrl").doesNotExist());
    }

    @Test
    void createSurveyTrimsNickname() throws Exception {
        given(surveyService.createSurvey(new CreateSurveyCommand("만두")))
                .willReturn(createdSurvey(" 만두 "));

        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userNickname\":\" 만두 \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload.userNickname").value("만두"));
    }

    @Test
    void createSurveyAllowsSpecialCharactersAndEmoji() throws Exception {
        given(surveyService.createSurvey(new CreateSurveyCommand("😀!@#")))
                .willReturn(createdSurvey("😀!@#"));

        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userNickname\":\"😀!@#\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload.userNickname").value("😀!@#"));
    }

    @Test
    void createSurveyRejectsBlankNicknameAfterTrim() throws Exception {
        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userNickname\":\"     \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("요청값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.payload.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.payload.errors[0].field").value("userNickname"));

        verifyNoInteractions(surveyService);
    }

    @Test
    void createSurveyRejectsNicknameLongerThanTenCharacters() throws Exception {
        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userNickname\":\"12345678901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.payload.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.payload.errors[0].field").value("userNickname"));

        verifyNoInteractions(surveyService);
    }

    @Test
    void startSubmissionUsesSurveyCodePathAndCommonWrapper() throws Exception {
        given(surveyService.startSubmission(SURVEY_CODE))
                .willReturn(new SubmissionStartedResult(
                        10L,
                        SubmitterType.SELF,
                        SubmissionStatus.IN_PROGRESS,
                        "만두",
                        List.of()
                ));

        mockMvc.perform(post("/api/v1/surveys/" + SURVEY_CODE + "/submissions"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/submissions/10"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("응답이 시작되었습니다."))
                .andExpect(jsonPath("$.payload.submissionId").value(10))
                .andExpect(jsonPath("$.payload.submitterType").value("SELF"))
                .andExpect(jsonPath("$.payload.submissionStatus").value("IN_PROGRESS"));
    }

    @Test
    void getSurveyStatusUsesSurveyCodePathAndCommonWrapper() throws Exception {
        given(surveyService.getSurveyStatus(SURVEY_CODE))
                .willReturn(new SurveyStatusResult(
                        1L,
                        "만두",
                        SurveyStatus.COLLECTING,
                        ResultStatus.COLLECTING_PEER_RESPONSES,
                        true,
                        2,
                        3,
                        RESULT_AVAILABLE_AT,
                        3600,
                        SURVEY_CODE
                ));

        mockMvc.perform(get("/api/v1/surveys/" + SURVEY_CODE + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("설문 상태를 조회했습니다."))
                .andExpect(jsonPath("$.payload.surveyCode").value(SURVEY_CODE))
                .andExpect(jsonPath("$.payload.shareUrl").value("https://looky.my/" + SURVEY_CODE))
                .andExpect(jsonPath("$.payload.userNickname").value("만두"))
                .andExpect(jsonPath("$.payload.surveyStatus").value("COLLECTING"))
                .andExpect(jsonPath("$.payload.resultStatus").value("COLLECTING_PEER_RESPONSES"))
                .andExpect(jsonPath("$.payload.selfSubmitted").value(true))
                .andExpect(jsonPath("$.payload.peerSubmissionCount").value(2))
                .andExpect(jsonPath("$.payload.requiredPeerSubmissionCount").value(3))
                .andExpect(jsonPath("$.payload.surveyId").doesNotExist())
                .andExpect(jsonPath("$.payload.ownerCode").doesNotExist())
                .andExpect(jsonPath("$.payload.shareCode").doesNotExist())
                .andExpect(jsonPath("$.payload.ownerUrl").doesNotExist());
    }

    @Test
    void submitAnswersUsesCommonWrapper() throws Exception {
        given(surveyService.submitAnswers(
                10L,
                new SubmitAnswersCommand(List.of(new AnswerCommand(1L, 101L)))
        )).willReturn(new SubmissionCompletedResult(
                10L,
                SubmitterType.SELF,
                SubmissionStatus.COMPLETED,
                CREATED_AT
        ));

        mockMvc.perform(post("/api/v1/submissions/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "questionId": 1,
                                      "answerOptionId": 101
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("응답이 제출되었습니다."))
                .andExpect(jsonPath("$.payload.submissionId").value(10))
                .andExpect(jsonPath("$.payload.submitterType").value("SELF"))
                .andExpect(jsonPath("$.payload.submissionStatus").value("COMPLETED"));
    }

    @Test
    void getSurveyResultReturnsReadyResultWithQuadrantImages() throws Exception {
        given(resultQueryService.getSurveyResult(SURVEY_CODE))
                .willReturn(new SurveyResultResult(
                        SURVEY_CODE,
                        ResultStatus.READY,
                        null,
                        Map.of(
                                "OPEN", "https://cdn.looky.my/results/b91/open.png",
                                "BLIND", "https://cdn.looky.my/results/b91/blind.png",
                                "HIDDEN", "https://cdn.looky.my/results/b91/hidden.png",
                                "UNKNOWN", "https://cdn.looky.my/results/b91/unknown.png"
                        ),
                        Map.of(
                                "OPEN", "서로 알고 있는 강점",
                                "BLIND", "타인이 먼저 발견하는 특성",
                                "HIDDEN", "혼자 알고 있는 내면",
                                "UNKNOWN", "아직 발견되지 않은 가능성"
                        ),
                        new ResultOverviewRecord(
                                "마음을 잘 여는 사람",
                                "대화를 여는 다정한 기운",
                                "낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 주변도 금세 편하게 반응하고 흐름이 부드러워집니다. 끝나고 나면 따뜻한 여운이 오래 남습니다.",
                                "앞장서다 혼자 짐을 다 안을 때가 있어요.\n한 번만 속도 맞춰볼까요? 하고 먼저 물어보세요.\n주변도 더 편하게 움직이고 관계가 오래 따뜻하게 돌아올 거예요."
                        ),
                        Map.of(
                                "OPEN", new SurveyResultQuadrant("탐험가", List.of("탐험 실험 다 좋아 인간", "새로운 거? 무조건 해봐야지"), "서로 알고 있는 강점", "https://cdn.looky.my/results/b91/open.png"),
                                "BLIND", new SurveyResultQuadrant("관찰자", List.of("사람 잘 챙기기 1순위", "분위기 메이커"), "타인이 먼저 발견하는 특성", "https://cdn.looky.my/results/b91/blind.png"),
                                "HIDDEN", new SurveyResultQuadrant("사색가", List.of("혼자가 편해", "디테일 집착"), "혼자 알고 있는 내면", "https://cdn.looky.my/results/b91/hidden.png"),
                                "UNKNOWN", new SurveyResultQuadrant("개척자", List.of("한 번 꽂히면 끝장", "새로운 곳 좋아"), "아직 발견되지 않은 가능성", "https://cdn.looky.my/results/b91/unknown.png")
                        )
                ));

        mockMvc.perform(get("/api/v1/surveys/" + SURVEY_CODE + "/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("설문 결과를 조회했습니다."))
                .andExpect(jsonPath("$.payload.surveyCode").value(SURVEY_CODE))
                .andExpect(jsonPath("$.payload.resultStatus").value("READY"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.OPEN").value("https://cdn.looky.my/results/b91/open.png"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.BLIND").value("https://cdn.looky.my/results/b91/blind.png"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.HIDDEN").value("https://cdn.looky.my/results/b91/hidden.png"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.UNKNOWN").value("https://cdn.looky.my/results/b91/unknown.png"))
                .andExpect(jsonPath("$.payload.quadrantInterpretations.OPEN").value("서로 알고 있는 강점"))
                .andExpect(jsonPath("$.payload.quadrantInterpretations.BLIND").value("타인이 먼저 발견하는 특성"))
                .andExpect(jsonPath("$.payload.overall.keyword").value("마음을 잘 여는 사람"))
                .andExpect(jsonPath("$.payload.overall.analysisTitle").value("대화를 여는 다정한 기운"))
                .andExpect(jsonPath("$.payload.overall.analysisBody").value("낯선 자리에서도 \"먼저 같이 해볼까요?\" 하고 말을 건넵니다. 주변도 금세 편하게 반응하고 흐름이 부드러워집니다. 끝나고 나면 따뜻한 여운이 오래 남습니다."))
                .andExpect(jsonPath("$.payload.overall.tip").value("앞장서다 혼자 짐을 다 안을 때가 있어요.\n한 번만 속도 맞춰볼까요? 하고 먼저 물어보세요.\n주변도 더 편하게 움직이고 관계가 오래 따뜻하게 돌아올 거예요."))
                .andExpect(jsonPath("$.payload.quadrants.OPEN.definitionKeyword").value("탐험가"))
                .andExpect(jsonPath("$.payload.quadrants.OPEN.adjectiveKeywords[0]").value("탐험 실험 다 좋아 인간"))
                .andExpect(jsonPath("$.payload.mainImageUrl").doesNotExist());
    }

    @Test
    void getSurveyResultReturnsWaitingStatusWhenResultIsNotReady() throws Exception {
        given(resultQueryService.getSurveyResult(SURVEY_CODE))
                .willReturn(new SurveyResultResult(
                        SURVEY_CODE,
                        ResultStatus.COLLECTING_PEER_RESPONSES,
                        null
                ));

        mockMvc.perform(get("/api/v1/surveys/" + SURVEY_CODE + "/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.payload.resultStatus").value("COLLECTING_PEER_RESPONSES"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls").value(nullValue()));
    }

    @Test
    void getSurveyResultReturnsGeneratingStatusWhenResultIsGenerating() throws Exception {
        given(resultQueryService.getSurveyResult(SURVEY_CODE))
                .willReturn(new SurveyResultResult(
                        SURVEY_CODE,
                        ResultStatus.GENERATING,
                        null,
                        ResultGenerationPhase.IMAGE_GENERATING
                ));

        mockMvc.perform(get("/api/v1/surveys/" + SURVEY_CODE + "/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.payload.resultStatus").value("GENERATING"))
                .andExpect(jsonPath("$.payload.generationPhase").value("IMAGE_GENERATING"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls").value(nullValue()));
    }

    @Test
    void getSurveyResultReturnsFailedStatusWhenGenerationFailed() throws Exception {
        given(resultQueryService.getSurveyResult(SURVEY_CODE))
                .willReturn(new SurveyResultResult(
                        SURVEY_CODE,
                        ResultStatus.FAILED,
                        null
                ));

        mockMvc.perform(get("/api/v1/surveys/" + SURVEY_CODE + "/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.payload.resultStatus").value("FAILED"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls").value(nullValue()));
    }

    @Test
    void legacyOwnerAndShareEndpointsAreNotMapped() throws Exception {
        mockMvc.perform(post("/api/v1/owner/surveys/own_legacy/submissions/start"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/share/shr_legacy/submissions/start"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/surveys/" + SURVEY_CODE + "/submissions/start"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/submissions/10/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(surveyService);
    }

    private SurveyCreatedResult createdSurvey(String userNickname) {
        return new SurveyCreatedResult(
                1L,
                userNickname,
                SURVEY_CODE,
                SurveyStatus.DRAFT,
                ResultStatus.WAITING_SELF_RESPONSE,
                3,
                RESULT_AVAILABLE_AT,
                CREATED_AT
        );
    }
}
