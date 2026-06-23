package com.looky.result.scheduler;

import com.looky.result.application.ResultGenerationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResultGenerationScheduler {

    private final ResultGenerationService resultGenerationService;

    public ResultGenerationScheduler(ResultGenerationService resultGenerationService) {
        this.resultGenerationService = resultGenerationService;
    }

    @Scheduled(fixedDelayString = "${looky.result-generation.fixed-delay:60000}")
    public void generateReadyResults() {
        resultGenerationService.generateReadyResults();
    }
}
