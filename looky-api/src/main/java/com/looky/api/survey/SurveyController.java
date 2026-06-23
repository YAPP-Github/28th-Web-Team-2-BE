package com.looky.api.survey;

import com.looky.api.support.response.ApiResponse;
import com.looky.api.survey.dto.CreateSurveyRequest;
import com.looky.api.survey.dto.CreateSurveyResponse;
import com.looky.api.survey.dto.SubmissionCompletedResponse;
import com.looky.api.survey.dto.SubmissionStartedResponse;
import com.looky.api.survey.dto.SubmitAnswersRequest;
import com.looky.api.survey.dto.SurveyResultResponse;
import com.looky.api.survey.dto.SurveyStatusResponse;
import com.looky.survey.application.SurveyService;
import com.looky.survey.application.dto.CreateSurveyCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
public class SurveyController implements SurveyApi {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @Override
    @PostMapping("/surveys")
    public ResponseEntity<ApiResponse<CreateSurveyResponse>> createSurvey(@Valid @RequestBody CreateSurveyRequest request) {
        CreateSurveyResponse response = CreateSurveyResponse.from(surveyService.createSurvey(new CreateSurveyCommand(request.userNickname())));
        return ResponseEntity
                .created(URI.create("/api/v1/surveys/" + response.surveyCode() + "/status"))
                .body(ApiResponse.success("설문이 생성되었습니다.", response));
    }

    @Override
    @PostMapping("/surveys/{surveyCode}/submissions")
    public ResponseEntity<ApiResponse<SubmissionStartedResponse>> startSubmission(@PathVariable String surveyCode) {
        SubmissionStartedResponse response = SubmissionStartedResponse.from(surveyService.startSubmission(surveyCode));
        return ResponseEntity
                .created(URI.create("/api/v1/submissions/" + response.submissionId()))
                .body(ApiResponse.success("응답이 시작되었습니다.", response));
    }

    @Override
    @PostMapping("/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<SubmissionCompletedResponse>> submitAnswers(
            @PathVariable Long submissionId,
            @Valid @RequestBody SubmitAnswersRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "응답이 제출되었습니다.",
                SubmissionCompletedResponse.from(surveyService.submitAnswers(submissionId, request.toCommand()))
        ));
    }

    @Override
    @GetMapping("/surveys/{surveyCode}/status")
    public ResponseEntity<ApiResponse<SurveyStatusResponse>> getSurveyStatus(@PathVariable String surveyCode) {
        return ResponseEntity.ok(ApiResponse.success(
                "설문 상태를 조회했습니다.",
                SurveyStatusResponse.from(surveyService.getSurveyStatus(surveyCode))
        ));
    }

    @Override
    @GetMapping("/surveys/{surveyCode}/result")
    public ResponseEntity<ApiResponse<SurveyResultResponse>> getSurveyResult(@PathVariable String surveyCode) {
        return ResponseEntity.ok(ApiResponse.success(
                "설문 결과를 조회했습니다.",
                SurveyResultResponse.from(surveyService.getSurveyResult(surveyCode))
        ));
    }
}
