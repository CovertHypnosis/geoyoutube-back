package com.geotube.services;

import com.geotube.config.JwtTokenUtil;
import com.geotube.cron.YoutubeUpdateManager;
import com.geotube.dtos.UserDTO;
import com.geotube.model.ClientInformation;
import com.geotube.model.JwtRequest;
import com.geotube.model.User;
import com.geotube.repositories.UserRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.UUID;

@Service
@DependsOn({"jwtRequestFilter"})
public class SsoService implements UserDetailsService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final YoutubeUpdateManager youtubeUpdateManager;

    public SsoService(UserRepository userRepository,
                      AuthenticationManager authenticationManager,
                      JwtTokenUtil jwtTokenUtil, YoutubeUpdateManager youtubeUpdateManager) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.youtubeUpdateManager = youtubeUpdateManager;
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }

    public ClientInformation isTokenActive(final String token) {
        ClientInformation clientInformation = new ClientInformation();
        Boolean isExpired = jwtTokenUtil.isTokenExpired(token);
        if (!isExpired) {
            String username = jwtTokenUtil.getUsername(token);
            User user = userRepository.findByUsername(username);
            clientInformation.setUserName(username);
            clientInformation.setEmail(user.getEmail());
            clientInformation.setClientId(user.getId().toString());
            clientInformation.setToken(token);
        }
        return clientInformation;
    }

    public ClientInformation login(JwtRequest authenticationRequest) throws Exception {
        String username = authenticationRequest.getUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new BadCredentialsException("invalid credentials");
        }
        String password = user.getPassword();
        if (bCryptPasswordEncoder.matches(authenticationRequest.getPassword(), password)) {
            youtubeUpdateManager.addTrendingInfoByCountry(user.getCountry());
            return getResponseEntity(authenticationRequest);
        } else {
            throw new BadCredentialsException("invalid credentials");
        }
    }

    private ClientInformation getResponseEntity(@RequestBody JwtRequest authenticationRequest) throws Exception {
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        final UserDetails userDetails = loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        final String userId = jwtTokenUtil.getUserIdFromToken(token);
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username);
        return new ClientInformation(username, user.getEmail(), userId, token);
    }

    private void authenticate(String userName, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            return new User(user.getUsername(), user.getPassword(), user.getEmail(), user.getCountry(), user.getUpdateTime());
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }

    public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.findByUsername(userDTO.getUsername()) == null) {
            userRepository.save(
                    new User(userDTO.getUsername(),
                            bCryptPasswordEncoder.encode(userDTO.getPassword()), userDTO.getEmail(), userDTO.getCountry(),
                            userDTO.getUpdateTime()));
            return userDTO;
        }
        return null;
    }

    public UserDTO updateUser(UserDTO userDTO) {
        if (userRepository.findByUsername(userDTO.getUsername()) != null) {
            userRepository.save(new User(userDTO.getUsername(),
                    bCryptPasswordEncoder.encode(userDTO.getPassword()), userDTO.getEmail(), userDTO.getCountry(), userDTO.getUpdateTime()));
            return userDTO;
        }
        return null;
    }

    public void userLeft(String userId) {
        Optional<User> user = userRepository.findById(UUID.fromString(userId));
        user.ifPresent(u -> youtubeUpdateManager.removeTrendingInfoByCountry(u.getCountry()));
    }
}
