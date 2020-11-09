package com.geotube.controllers;

import com.geotube.dtos.UserDTO;
import com.geotube.model.ClientInformation;
import com.geotube.model.JwtRequest;
import com.geotube.services.SsoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class SsoController {
    private final SsoService userDetailsService;

    public SsoController(SsoService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userDetailsService.createUser(userDTO);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping("/left/{userId}")
    public ResponseEntity userLeft(@PathVariable String userId) {
        userDetailsService.userLeft(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<ClientInformation> createLoginToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
        return new ResponseEntity<>(userDetailsService.login(authenticationRequest), HttpStatus.OK);
    }

    @PutMapping("/update")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userDetailsService.updateUser(userDTO);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/")
    public ResponseEntity<ClientInformation> isTokenActive(final String token) {
        return new ResponseEntity<>(userDetailsService.isTokenActive(token), HttpStatus.OK);
    }
}
