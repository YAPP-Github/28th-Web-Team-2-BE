package com.looky.result.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResultAnswerAdjectiveRecordTest {

    @Test
    void exposesStoredAnswerAdjectiveRecordType() throws Exception {
        assertNotNull(Class.forName("com.looky.result.application.ResultAnswerAdjectiveRecord"));
    }
}
