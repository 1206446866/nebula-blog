package com.nebula.common.mail.service;

public interface MailService {

    /**
     * 发送文本邮件
     *
     * @param to 收件人
     * @param subject 标题
     * @param content 内容
     */
    void sendText(String to, String subject, String content);


    /**
     * 发送HTML邮件
     *
     * @param to 收件人
     * @param subject 标题
     * @param html HTML内容
     */
    void sendHtml(String to, String subject, String html);
}
