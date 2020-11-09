package com.geotube.controllers;

import com.geotube.cron.YoutubeUpdateManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class CountryController {
    private final YoutubeUpdateManager youtubeUpdateManager;

    public CountryController(YoutubeUpdateManager youtubeUpdateManager) {
        this.youtubeUpdateManager = youtubeUpdateManager;
    }

    @GetMapping("/country")
    public ResponseEntity<List<String>> getCountries() {
        List<String> allCountries = youtubeUpdateManager.getAllCountries();
        return new ResponseEntity<>(allCountries, HttpStatus.OK);
    }
}
