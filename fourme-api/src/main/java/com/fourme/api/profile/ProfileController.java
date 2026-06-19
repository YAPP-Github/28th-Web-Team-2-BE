package com.fourme.api.profile;

import com.fourme.api.profile.dto.CreateProfileRequest;
import com.fourme.api.profile.dto.CreateProfileResponse;
import com.fourme.profile.application.CreateProfileCommand;
import com.fourme.profile.application.CreateProfileResult;
import com.fourme.profile.application.ProfileApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileApplicationService profileApplicationService;

    public ProfileController(ProfileApplicationService profileApplicationService) {
        this.profileApplicationService = profileApplicationService;
    }

    @PostMapping
    public ResponseEntity<CreateProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest request
    ) {
        CreateProfileResult result = profileApplicationService.createProfile(
                new CreateProfileCommand(request.displayName())
        );
        CreateProfileResponse response = new CreateProfileResponse(
                result.profileId().toString(),
                result.ownerToken(),
                result.createdAt()
        );

        return ResponseEntity
                .created(URI.create("/api/v1/profiles/" + response.profileId()))
                .body(response);
    }
}
