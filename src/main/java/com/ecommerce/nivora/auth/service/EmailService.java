package com.ecommerce.nivora.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        log.info("=== VERIFICATION CODE FOR {} : {} ===", to, code);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Nivora - Mã xác nhận kích hoạt tài khoản");
            message.setText("Chào bạn,\n\n"
                    + "Mã xác nhận kích hoạt tài khoản Nivora của bạn là: " + code + "\n\n"
                    + "Vui lòng nhập mã này trên trang xác thực để hoàn tất đăng ký tài khoản.\n\n"
                    + "Trân trọng,\n"
                    + "Nivora Team");
            mailSender.send(message);
            log.info("Successfully sent verification email to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}. Proceeding without throwing exception for local testing.", to, e.getMessage());
        }
    }
}
