package com.fixall.backend.service;

import com.fixall.backend.dto.request.UpdateProfessionalProfileRequest;
import com.fixall.backend.exception.ResourceNotFoundException;
import com.fixall.backend.model.ProfessionalProfile;
import com.fixall.backend.model.User;
import com.fixall.backend.repository.ProfessionalProfileRepository;
import com.fixall.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfessionalProfileService {

    private final ProfessionalProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfessionalProfile saveOrUpdate(String userId, UpdateProfessionalProfileRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<ProfessionalProfile> existing = profileRepository.findByUser_Id(userId);
        ProfessionalProfile profile = existing.orElseGet(() -> ProfessionalProfile.builder()
            .user(user)
            .build());

        profile.setBusinessName(req.getBusinessName());
        profile.setBio(req.getBio());
        profile.setCategories(req.getCategories());
        if (profile.getRatingAverage() == null) profile.setRatingAverage(0.0);
        if (profile.getTotalJobs() == null) profile.setTotalJobs(0);

        return profileRepository.save(profile);
    }

    /** Returns the professional profile for a user, or null if none exists yet. */
    public ProfessionalProfile getByUserId(String userId) {
        return profileRepository.findByUser_Id(userId).orElse(null);
    }
}

