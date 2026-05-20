//package com.nebula.start.controller;
//
//import com.nebula.start.dto.LoginDTO;
//import com.nebula.start.utils.JwtUtil;
//import org.springframework.security.authentication.*;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/auth")
//public class LoginController {
//
//    private final AuthenticationManager authenticationManager;
//    private final UserDetailsService userDetailsService;
//    private final JwtUtil jwtUtil;
//
//    public LoginController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtUtil jwtUtil) {
//        this.authenticationManager = authenticationManager;
//        this.userDetailsService = userDetailsService;
//        this.jwtUtil = jwtUtil;
//    }
//
//    @PostMapping("/login")
//    public String login(@RequestBody LoginDTO dto) {
//        try {
//            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
//        } catch (BadCredentialsException e) {
//            return "用户名或密码错误";
//        }
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getUsername());
//        String token = jwtUtil.generateToken(userDetails.getUsername());
//        return token;
//    }
//}