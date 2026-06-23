package com.looky.api.survey;

import com.looky.common.exception.ErrorCode;
import com.looky.common.exception.LookyException;
import com.looky.result.application.ResultQueryService;
import com.looky.survey.application.SurveyService;
import com.looky.survey.application.dto.AnswerCommand;
import com.looky.survey.application.dto.CreateSurveyCommand;
import com.looky.survey.application.dto.SubmissionCompletedResult;
import com.looky.survey.application.dto.SubmissionStartedResult;
import com.looky.survey.application.dto.SubmitAnswersCommand;
import com.looky.survey.application.dto.SurveyCreatedResult;
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
                .andExpect(header().string("Location", "/api/v1/surveys/b91k2p8xq4z2/status"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("설문이 생성되었습니다."))
                .andExpect(jsonPath("$.payload.surveyCode").value("b91k2p8xq4z2"))
                .andExpect(jsonPath("$.payload.shareUrl").value("https://looky.my/surveys/b91k2p8xq4z2"))
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
        given(surveyService.startSubmission("b91k2p8xq4z2"))
                .willReturn(new SubmissionStartedResult(
                        10L,
                        SubmitterType.SELF,
                        SubmissionStatus.IN_PROGRESS,
                        "만두",
                        List.of()
                ));

        mockMvc.perform(post("/api/v1/surveys/b91k2p8xq4z2/submissions"))
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
        given(surveyService.getSurveyStatus("b91k2p8xq4z2"))
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
                        "b91k2p8xq4z2"
                ));

        mockMvc.perform(get("/api/v1/surveys/b91k2p8xq4z2/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("설문 상태를 조회했습니다."))
                .andExpect(jsonPath("$.payload.surveyCode").value("b91k2p8xq4z2"))
                .andExpect(jsonPath("$.payload.shareUrl").value("https://looky.my/surveys/b91k2p8xq4z2"))
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
        given(resultQueryService.getSurveyResult("b91k2p8xq4z2"))
                .willReturn(new SurveyResultResult(
                        "b91k2p8xq4z2",
                        ResultStatus.READY,
                        Map.of(
                                "OPEN", "https://cdn.looky.my/results/b91/open.png",
                                "BLIND", "https://cdn.looky.my/results/b91/blind.png",
                                "HIDDEN", "https://cdn.looky.my/results/b91/hidden.png",
                                "UNKNOWN", "https://cdn.looky.my/results/b91/unknown.png"
                        )
                ));

        mockMvc.perform(get("/api/v1/surveys/b91k2p8xq4z2/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("설문 결과를 조회했습니다."))
                .andExpect(jsonPath("$.payload.surveyCode").value("b91k2p8xq4z2"))
                .andExpect(jsonPath("$.payload.resultStatus").value("READY"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.OPEN").value("https://cdn.looky.my/results/b91/open.png"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.BLIND").value("https://cdn.looky.my/results/b91/blind.png"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.HIDDEN").value("https://cdn.looky.my/results/b91/hidden.png"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls.UNKNOWN").value("https://cdn.looky.my/results/b91/unknown.png"))
                .andExpect(jsonPath("$.payload.mainImageUrl").doesNotExist());
    }

    @Test
    void getSurveyResultReturnsWaitingStatusWhenResultIsNotReady() throws Exception {
        given(resultQueryService.getSurveyResult("b91k2p8xq4z2"))
                .willReturn(new SurveyResultResult(
                        "b91k2p8xq4z2",
                        ResultStatus.COLLECTING_PEER_RESPONSES,
                        null
                ));

        mockMvc.perform(get("/api/v1/surveys/b91k2p8xq4z2/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.payload.resultStatus").value("COLLECTING_PEER_RESPONSES"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls").value(nullValue()));
    }

    @Test
    void getSurveyResultReturnsGeneratingStatusWhenResultIsGenerating() throws Exception {
        given(resultQueryService.getSurveyResult("b91k2p8xq4z2"))
                .willReturn(new SurveyResultResult(
                        "b91k2p8xq4z2",
                        ResultStatus.GENERATING,
                        null
                ));

        mockMvc.perform(get("/api/v1/surveys/b91k2p8xq4z2/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.payload.resultStatus").value("GENERATING"))
                .andExpect(jsonPath("$.payload.quadrantImageUrls").value(nullValue()));
    }

    @Test
    void getSurveyResultReturnsFailedStatusWhenGenerationFailed() throws Exception {
        given(resultQueryService.getSurveyResult("b91k2p8xq4z2"))
                .willReturn(new SurveyResultResult(
                        "b91k2p8xq4z2",
                        ResultStatus.FAILED,
                        null
                ));

        mockMvc.perform(get("/api/v1/surveys/b91k2p8xq4z2/result"))
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
        mockMvc.perform(post("/api/v1/surveys/b91k2p8xq4z2/submissions/start"))
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
                "b91k2p8xq4z2",
                SurveyStatus.DRAFT,
                ResultStatus.WAITING_SELF_RESPONSE,
                3,
                RESULT_AVAILABLE_AT,
                CREATED_AT
        );
    }
}
