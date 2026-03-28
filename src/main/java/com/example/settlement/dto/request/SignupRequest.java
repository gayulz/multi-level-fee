package com.example.settlement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * [NEW] 회원가입 요청 DTO
 * 
 * JSR-303 검증 어노테이션을 사용하여 폼 입력을 검증합니다.
 *
 * @author gayul.kim
 * @since 2026-03-09
 */
@Getter
@Setter
public class SignupRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수 입력입니다")
    private String email;

    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$", message = "특수문자 1개 이상, 영어 대문자 1개 이상, 최소 8자리")
    private String password;

    @NotBlank(message = "이름은 필수 입력입니다")
    private String name;

    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phone;

    @NotNull(message = "소속은 필수 선택입니다")
    private Long organizationId;
}
