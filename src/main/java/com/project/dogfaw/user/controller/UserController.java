package com.project.dogfaw.user.controller;

import com.project.dogfaw.common.CommonService;
import com.project.dogfaw.common.exception.ErrorCode;
import com.project.dogfaw.common.exception.ExceptionResponse;
import com.project.dogfaw.common.exception.StatusResponseDto;
import com.project.dogfaw.sse.security.jwt.TokenDto;
import com.project.dogfaw.sse.security.jwt.TokenRequestDto;
import com.project.dogfaw.user.dto.KakaoUserInfo;
import com.project.dogfaw.user.dto.LoginDto;
import com.project.dogfaw.user.dto.SignupRequestDto;
import com.project.dogfaw.user.dto.UserInfo;
import com.project.dogfaw.user.model.User;
import com.project.dogfaw.user.repository.UserRepository;
import com.project.dogfaw.user.service.KakaoUserService;
import com.project.dogfaw.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@Slf4j
@Api(tags = {"유저관련 API"})
public class UserController {
    private final UserService userService;
    private final KakaoUserService kakaoUserService;
//    private final GoogleUserService googleUserService;
    private final UserRepository userRepository;
    private final CommonService commonService;


    // 회원가입 API


    @Operation(summary = "회원가입", description = "이메일(username), 닉네임(nickname), 비밀번호(password), 스택(stacks)을 이용하여 유저 정보를 신규 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "회원가입완료"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @PostMapping("/user/signup")
    public ResponseEntity<Object> registerUser(@RequestBody SignupRequestDto requestDto) {
        TokenDto tokenDto = userService.register(requestDto);
        return new ResponseEntity<>(new StatusResponseDto("회원가입 완료했습니다.", ""), HttpStatus.OK);
    }

    // 닉네임 중복검사 API
    @Operation(summary = "닉네임 중복검사", description = "DB에 중복된 닉네임이 존재하는지 검사합니다.")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "사용가능한 닉네임"),
            @ApiResponse(responseCode ="400",description = "이미 존재하는 닉네임"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @PostMapping("/user/nickname")
    public ResponseEntity<Object> nicknameCheck(@RequestBody SignupRequestDto requestDto){
        if(userRepository.existsByNickname(requestDto.getNickname())) {
            return new ResponseEntity<>(new ExceptionResponse(ErrorCode.SIGNUP_NICKNAME_DUPLICATE), HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(new StatusResponseDto("사용가능한 닉네임입니다.", ""), HttpStatus.OK);
        }
    }


    // 로그인 API
    @Operation(summary = "로그인", description = "이메일(username), 비밀번호(password)를 입력하여 로그인 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "로그인 성공"),
            @ApiResponse(responseCode ="400",description = "로그인 실패"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @PostMapping("/user/login")
    public ResponseEntity<Object> login(HttpServletRequest httpServletRequest,@RequestBody LoginDto loginDto) {
        Map<String, Object> data = userService.login(loginDto);
        return new ResponseEntity<>(new StatusResponseDto("로그인에 성공하셨습니다", data), HttpStatus.OK);
    }

    // 로그아웃 API
    @Operation(summary = "로그아웃", description = "로그아웃하여 db에 저장된 Refresh토큰을 삭제")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "로그아웃 성공"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @PostMapping("/user/logout")
    public ResponseEntity<Object> logout(@RequestBody TokenRequestDto tokenRequestDto) {
        userService.deleteRefreshToken(tokenRequestDto);
        return new ResponseEntity<>(new StatusResponseDto("로그아웃 성공", ""), HttpStatus.OK);
    }

    // 토큰 재발행 API
    @Operation(summary = "AccessToken 재발행", description = "Refresh 토큰을 이용하여 Access토큰을 재발급")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "토큰 재발급 성공"),
            @ApiResponse(responseCode ="400",description = "토큰 재발급 실패"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @PostMapping("/user/reissue")
    public ResponseEntity<Object> reissue(@RequestBody TokenRequestDto tokenRequestDto) {
        TokenDto tokenDto = userService.reissue(tokenRequestDto);
        return new ResponseEntity<>(new StatusResponseDto("토큰 재발급 성공", tokenDto), HttpStatus.OK);
    }

    // 유저 정보 API
    @Operation(summary = "유저정보 조회", description = "SecurityContextHolder에서 로그인한 유저 정보를 조회하여 이메일, 닉네임, 프로필이미지, 기술스택 을 보내줌")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "조회성공"),
            @ApiResponse(responseCode ="400",description = "조회실패"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @GetMapping("/user/userinfo")
    public UserInfo Session(){
        User user = commonService.getUser();
        return new UserInfo(user.getUsername(), user.getNickname(),user.getProfileImg(), user.getStacks());
    }

    // 카카오 로그인 API
    @Operation(summary = "카카오 로그인", description = "인가코드를 이용하여 카카오에 토큰을 요청하고 해당 토큰을 이용하여 토큰을 재 생성 한 후 AccessToken부여")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "로그인 성공"),
            @ApiResponse(responseCode ="400",description = "로그인 실패"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @GetMapping("/user/kakao/login")
    public void kakaoLogin(HttpServletRequest httpServletRequest,@RequestParam String code, HttpServletResponse response) throws IOException {
        KakaoUserInfo kakaoUserInfo = kakaoUserService.kakaoLogin(code);
        TokenDto tokenDto = userService.SignupUserCheck(kakaoUserInfo.getKakaoId());
        String url = "https://dogpaw.kr/?token=" + tokenDto.getAccessToken() + "&refreshtoken=" + tokenDto.getRefreshToken();
        User kakaoUser = userRepository.findByUsername(kakaoUserInfo.getKakaoMemberId()).orElse(null);
        if (kakaoUser.getNickname().equals("default")){
            url = url + "&nickname=default" + "&userId=" + kakaoUser.getId();
        }
        else{
            url = url + "&nickname=" + kakaoUser.getNickname() + "&userId=" + kakaoUser.getId();
        }
        response.sendRedirect(url);
    }


    // 회원가입 추가 정보 API
    @Operation(summary = "회원정보 추가 기입", description = "카카오 로그인 시 닉네입과 기술 스택에대한 정보 추가기입 요청")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "추가정보 등록 성공"),
            @ApiResponse(responseCode ="400",description = "추가정보 등록 실패"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @PostMapping("/user/signup/addInfo")
    public ResponseEntity<Object> addInfo(HttpServletRequest httpServletRequest,@RequestBody SignupRequestDto requestDto) {
        User user = commonService.getUser();
        userService.addInfo(requestDto,user);

        return new ResponseEntity<>(new StatusResponseDto("추가 정보 등록 성공",""), HttpStatus.CREATED);
    }

    // 회원 정보 삭제 API
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴 시 회원 정보 삭제")
    @ApiResponses({
            @ApiResponse(responseCode ="200",description = "회원탈퇴 성공"),
            @ApiResponse(responseCode ="400",description = "회원탈퇴 실패"),
            @ApiResponse(responseCode = "500",description = "서버에러"),
    })
    @PutMapping("/user/delete")
    public ResponseEntity<Object> deleteUser() {
        userService.deleteUser();
        return new ResponseEntity<>(new StatusResponseDto("회원 정보 삭제 성공",""), HttpStatus.CREATED);
    }

    /*요청에 대한 ip추적*/
    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}