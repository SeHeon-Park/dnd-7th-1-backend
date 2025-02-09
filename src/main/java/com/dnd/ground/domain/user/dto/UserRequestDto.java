package com.dnd.ground.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 회원 관련 Request Dto
 * @author  박세헌, 박찬호
 * @since   2022-08-18
 * @updated 1.회원 정보 수정 구현 완료
 *          - 2022-10-22 박찬호
 */

@Data
public class UserRequestDto {

    /*회원가입시 사용하는 DTO*/
    @Data
    static public class SignUp {
        @ApiModelProperty(name = "유저의 닉네임", example = "NickA", required = true)
        private String nickname;

        @ApiModelProperty(name = "친구 리스트(닉네임)", example = "[NickA]", required = true)
        private List<String> friends;

        @ApiModelProperty(name = "카카오 리프레시 토큰", example = "2LqQd2jnW50rHbOyGyyKu_xNRv4p2Jri7wWsso7RCj11mgAAAYLKLqJq", required = true)
        private String kakaoRefreshToken;

        @ApiModelProperty(name = "내 위치 공개", example = "true", required = true)
        private Boolean isPublicRecord;
        //..필터 추가 예정
    }

    @Data
    static public class LookUp{

        @ApiModelProperty(name = "유저의 닉네임", example = "NickA", required = true)
        private String nickname;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        @ApiModelProperty(name = "조회 하고 싶은 데이터의 시작 날짜", example = "2022-08-15T00:00:00",required = true)
        private LocalDateTime start;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
        @ApiModelProperty(name = "조회 하고 싶은 데이터의 끝 날짜", example = "2022-08-18T23:59:59", required = true)
        private LocalDateTime end;
    }

    @Data
    @AllArgsConstructor
    static public class Profile{

        @NotNull
        @ApiModelProperty(name = "유저의 원래 닉네임", example = "NickA", required = true)
        private String originNickname;

        @NotNull
        @ApiModelProperty(name = "수정한 닉네임", example = "NickB", required = true)
        private String editNickname;

        @ApiModelProperty(name = "유저의 소개글", example = "소개글 예시")
        private String intro;

        @ApiModelProperty(name = "네모두 기본 사진 변경 여부", example = "false")
        private Boolean isBasic;
    }

    /* 운동 기록 날짜 조회시 사용하는 dto */
    @Data
    static public class dayEventList{
        @ApiModelProperty(name = "닉네임", example = "NickA", required = true)
        private String nickname;

        @ApiModelProperty(name = "년-월-날짜", example = "2022-09-01", required = true)
        private LocalDate yearMonth;
    }
}
