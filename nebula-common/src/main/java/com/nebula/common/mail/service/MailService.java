package com.nebula.common.mail.service;

/** 文本与HTML邮件发送服务。 */
public interface MailService {

    /**
     * 发送纯文本邮件。
     *
     * @param to 收件邮箱
     * @param subject 邮件主题
     * @param content 纯文本正文
     */
    void sendText(String to, String subject, String content);


    /**
     * 发送HTML邮件，调用方负责提供可信的邮件内容。
     *
     * @param to 收件邮箱
     * @param subject 邮件主题
     * @param html HTML格式正文
     */
    void sendHtml(String to, String subject, String html);
}
