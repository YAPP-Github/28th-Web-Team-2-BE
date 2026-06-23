package com.looky.result.storage;

import com.looky.result.application.ResultUrlSigner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestResultUrlSigner implements ResultUrlSigner {
    @Override
    public String sign(String objectKey) {
        return "https://signed.test/" + objectKey;
    }
}
