package com.looky.result.scheduler;

import com.looky.result.application.ResultGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResultGenerationScheduler {

    private final ResultGenerationService resultGenerationService;

    @Scheduled(fixedDelayString = "${looky.result-generation.fixed-delay:60000}")
    public void generateReadyResults() {
        resultGenerationService.generateReadyResults();
    }
}
