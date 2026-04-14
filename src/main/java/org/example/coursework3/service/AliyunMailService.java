package org.example.coursework3.service;

import com.aliyun.dm20151123.models.SingleSendMailRequest;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AliyunMailService {

    private final com.aliyun.dm20151123.Client client;
    private final StringRedisTemplate redisTemplate;

    @Value("${aliyun.mail.from-address}")
    private String fromAddress;

    public AliyunMailService(
            @Value("${aliyun.mail.access-key-id}") String keyId,
            @Value("${aliyun.mail.access-key-secret}") String keySecret,
            @Value("${aliyun.mail.region}") String region,
            StringRedisTemplate redisTemplate
    ) throws Exception {
        this.redisTemplate = redisTemplate;
        Config config = new Config()
                .setAccessKeyId(keyId)
                .setAccessKeySecret(keySecret);
        config.endpoint = "dm.aliyuncs.com";
        this.client = new com.aliyun.dm20151123.Client(config);
    }

    @Async
    public void sendCaptcha(String toAddress) throws Exception {
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
        redisTemplate.opsForValue().set("captcha:" + toAddress, code, 5, TimeUnit.MINUTES);
        String subject = "您的注册验证码";
        String bodyHtml = "<p>您的验证码是：" + code + "，有效期 5 分钟。</p>";
        SingleSendMailRequest request = new SingleSendMailRequest()
                .setAccountName(fromAddress)
                .setAddressType(1)
                .setReplyToAddress(true)
                .setToAddress(toAddress)
                .setSubject(subject)
                .setHtmlBody(bodyHtml);

        client.singleSendMail(request);
    }

    @Async
    public void sendBookingStatusNotification(String name, String toAddress, String status, String note) throws Exception {
        String subject = "Your Booking Status";
        String content = status.equalsIgnoreCase("Confirmed") ? "Confirmed" : "Rejected";
        String bodyHtml = "<h3>Booking Handling Info</h3>" +
                "<p>Your Booking Status：<strong>" + content + "</strong></p>";

        if (note != null && !note.isEmpty()) {
            bodyHtml += "<p>Note：" + note + "</p>";
        }

        bodyHtml += "<p>Your booking status has been updated by: <strong>" + name +"</strong>. Please log in to the system to view details.</p>";

        SingleSendMailRequest request = new SingleSendMailRequest()
                .setAccountName(fromAddress)
                .setAddressType(1)
                .setReplyToAddress(true)
                .setToAddress(toAddress)
                .setSubject(subject)
                .setHtmlBody(bodyHtml);

        client.singleSendMail(request);
    }

    @Async
    public void sendCancellationNoticeToSpecialist(String toAddress, String slotInfo) throws Exception {
        String subject = "Booking Cancellation and Appointment Slot Release Notice";
        String bodyHtml = "<h3>Booking Cancellation Reminder</h3>" +
                "<p>Dear specialist, the client has canceled the originally scheduled reservation: <strong>" + slotInfo + "</strong></p>" +
                "<p><strong>The slot has now been automatically released</strong>, other customers can make a new booking.</p>";

        SingleSendMailRequest request = new SingleSendMailRequest()
                .setAccountName(fromAddress)
                .setAddressType(1)
                .setReplyToAddress(true)
                .setToAddress(toAddress)
                .setSubject(subject)
                .setHtmlBody(bodyHtml);

        client.singleSendMail(request);
    }
}