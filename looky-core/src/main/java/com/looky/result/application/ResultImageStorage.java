package com.looky.result.application;

import com.looky.result.domain.ResultQuadrantType;

public interface ResultImageStorage {
    String upload(String surveyCode, ResultQuadrantType quadrantType, byte[] imageBytes);
}
