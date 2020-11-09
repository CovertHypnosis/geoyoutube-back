package com.geotube.services;

import com.geotube.config.JwtTokenUtil;
import com.geotube.cron.YoutubeUpdateManager;
import com.geotube.dtos.TrendingInfoDTO;
import com.geotube.model.TrendingInfo;
import com.geotube.model.User;
import com.geotube.model.UserInformation;
import com.geotube.repositories.UserInformationRepository;
import com.geotube.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Log4j2
public class YoutubeService {
    private final JwtTokenUtil jsonTokenUtil;
    private final UserRepository userRepository;
    private final UserInformationRepository userInformationRepository;
    private final YoutubeUpdateManager youtubeUpdateManager;

    public YoutubeService(JwtTokenUtil jsonTokenUtil, UserRepository userRepository, UserInformationRepository userInformationRepository, YoutubeUpdateManager youtubeUpdateManager) {
        this.jsonTokenUtil = jsonTokenUtil;
        this.userRepository = userRepository;
        this.userInformationRepository = userInformationRepository;
        this.youtubeUpdateManager = youtubeUpdateManager;
    }

    public TrendingInfoDTO getYoutubeInformationByUser(HttpHeaders httpHeaders) {
        String authorization = Objects.requireNonNull(httpHeaders.get("Authorization")).get(0);
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        String userId = jsonTokenUtil.getUserIdFromToken(token);
        Optional<User> optionalUser = userRepository.findById(UUID.fromString(userId));
        Optional<UserInformation> optionalUserInformation = userInformationRepository.findFirstByUserId(UUID.fromString(userId));
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User cannot be found");
        }
        User user = optionalUser.get();
        UserInformation userInformation = optionalUserInformation
                .orElse(new UserInformation(user, 0L, user.getCountry(), null, null));
        Long currentRequestTime = Instant.now().getEpochSecond();
        Long lastRequestTime = userInformation.getLastRequestTime();
        // if request was made long time ago get a new one
        if (lastRequestTime == 0 || lastRequestTime - currentRequestTime > user.getUpdateTime() * 60) {
            TrendingInfo trendingInfoByCountry = youtubeUpdateManager.getTrendingInfoByCountry(user.getCountry());
            // in case country change was made after login
            if (trendingInfoByCountry == null) {
                youtubeUpdateManager.addTrendingInfoByCountry(user.getCountry());
                // might be necessary to put this in reactive flow
            }
            return getUserInformation(user, userInformation, currentRequestTime);
        } else {
            return new TrendingInfoDTO(user.getCountry(), user.getUpdateTime(),
                    userInformation.getYoutubeLink(), userInformation.getYoutubeComment());
        }
    }

    public TrendingInfoDTO getUserInformation(User user, UserInformation userInformation, Long currentRequestTime) {
        TrendingInfo trendingInfoByCountry = youtubeUpdateManager.getTrendingInfoByCountry(user.getCountry());
        userInformation.setCountry(user.getCountry());
        userInformation.setLastRequestTime(currentRequestTime);
        userInformation.setYoutubeLink(trendingInfoByCountry.getVideoLink());
        userInformation.setYoutubeComment(trendingInfoByCountry.getComment());
        userInformationRepository.save(userInformation);
        return new TrendingInfoDTO(user.getCountry(), user.getUpdateTime(),
                userInformation.getYoutubeLink(), userInformation.getYoutubeComment());
    }
}
