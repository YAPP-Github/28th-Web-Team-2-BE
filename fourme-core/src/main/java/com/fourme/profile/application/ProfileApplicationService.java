package com.fourme.profile.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileApplicationService {

    private final ProfileCommandService commandService;

    public CreateProfileResult createProfile(CreateProfileCommand command) {
        return commandService.createProfile(command);
    }
}
