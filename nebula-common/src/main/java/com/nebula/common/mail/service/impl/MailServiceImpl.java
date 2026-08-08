package com.nebula.common.mail.service.impl;

import com.nebula.common.mail.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Override
    public void sendText(String to, String subject, String content) {
        MimeMessage message = mailSender.createMimeMessage();

        try {

            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            helper.setFrom(
                    mailUsername
            );
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtml(String to, String subject, String html) {
        MimeMessage message = mailSender.createMimeMessage();

        try {

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom(
                    mailUsername
            );
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
