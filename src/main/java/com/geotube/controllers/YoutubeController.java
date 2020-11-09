package com.geotube.controllers;

import com.geotube.dtos.TrendingInfoDTO;
import com.geotube.services.YoutubeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class YoutubeController {
    private final YoutubeService youtubeService;

    public YoutubeController(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    @GetMapping("/youtube")
    public ResponseEntity<TrendingInfoDTO> getYoutube(@RequestHeader HttpHeaders httpHeaders) {
        TrendingInfoDTO youtubeInformationByUser = youtubeService.getYoutubeInformationByUser(httpHeaders);
        return new ResponseEntity<>(youtubeInformationByUser, HttpStatus.OK);
    }
}
