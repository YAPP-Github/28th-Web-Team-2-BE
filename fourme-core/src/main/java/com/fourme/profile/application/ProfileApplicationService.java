package com.fourme.profile.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileApplicationService {

    private final ProfileCommandService commandService;
    private final ProfileQueryService queryService;

    public CreateProfileResult createProfile(CreateProfileCommand command) {
        return commandService.createProfile(command);
    }

    public boolean verifyOwnerToken(VerifyProfileOwnerTokenQuery query) {
        return queryService.verifyOwnerToken(query.profileId(), query.ownerToken());
    }
}
