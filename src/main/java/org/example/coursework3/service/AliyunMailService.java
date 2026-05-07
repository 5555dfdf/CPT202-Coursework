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


    /**
     * Initializes the Aliyun DM Client using access credentials and region configuration.
     *
     * @param keyId         Aliyun AccessKey ID
     * @param keySecret     Aliyun AccessKey Secret
     * @param region        Aliyun region ID
     * @param redisTemplate Spring Data Redis template for key-value storage
     * @throws Exception    If client initialization fails
     */
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
    /**
     * Generates a 6-digit captcha, stores it in Redis, and sends it to the recipient.
     * This method is executed asynchronously to prevent blocking the main thread
     * during the SMTP/API call.
     *
     * @param toAddress  The recipient's email address.
     * @throws Exception If email delivery via Aliyun DM fails.
     */
    @Async("taskExecutor")
    public void sendCaptcha(String toAddress) throws Exception {
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
        redisTemplate.opsForValue().set("captcha:" + toAddress, code, 5, TimeUnit.MINUTES);
        String subject = "Your registration verification code";
        String bodyHtml = "<p>Your verification code is：" + code + ". Valid for 5 minutes。</p>";
        SingleSendMailRequest request = new SingleSendMailRequest()
                .setAccountName(fromAddress)
                .setAddressType(1)
                .setReplyToAddress(true)
                .setToAddress(toAddress)
                .setSubject(subject)
                .setHtmlBody(bodyHtml);

        client.singleSendMail(request);
    }

    /**
     * Sends an asynchronous email notification to a specialist when a booking is canceled.
     * This method informs the specialist that their specific time slot has been freed
     * and is now available for other clients to book.
     *
     * @param toAddress The specialist's email address.
     * @param slotInfo  A formatted string describing the canceled slot.
     * @throws Exception If the Aliyun DM API call fails.
     */
    @Async
    public void sendCancellationNoticeToSpecialist(String toAddress, String slotInfo) throws Exception {

        // email head
        String subject = "Booking Cancellation and Appointment Slot Release Notice";
        // email body content
        String bodyHtml = "<h3>Booking Cancellation Reminder</h3>" +
                "<p>Dear specialist, the client has canceled the originally scheduled reservation: <strong>" + slotInfo + "</strong></p>" +
                "<p><strong>The slot has now been automatically released</strong>, other customers can make a new booking.</p>";
        // initialize the Aliyun Mail request with sender and recipient details
        SingleSendMailRequest request = new SingleSendMailRequest()
                .setAccountName(fromAddress)
                .setAddressType(1)
                .setReplyToAddress(true)
                .setToAddress(toAddress)
                .setSubject(subject)
                .setHtmlBody(bodyHtml);

        client.singleSendMail(request);
    }

    /**
     * Sends a versatile, asynchronous status update notification to either a specialist or a customer.
     * This generic method handles various state changes in the booking lifecycle, including
     * confirmation, completion, or administrative adjustments.
     *
     * @param toAddress  The recipient's email address.
     * @param role       The recipient's role used in the salutation.
     * @param status     The new status of the booking.
     * @param slotInfo   Formatted time information for the appointment.
     * @param note       An optional message or reason to include in the body.
     * @throws Exception If the Aliyun DM API call fails.
     */
    @Async
    public void sendGenericStatusNotification(String toAddress, String role, String status, String slotInfo, String note) throws Exception {
        // email head
        String subject = "Booking Status Update: " + status;
        // email body content
        String bodyHtml = "<h3>Booking Status Update</h3>" +
                "<p>Dear " + role + ",</p>" +
                "<p>The status of the booking for the time slot <strong>" + slotInfo
                + "</strong> has been updated to: <strong style='color:blue'>" + status
                + "</strong>.</p>";
        // Append the optional note
        if (note != null && !note.isEmpty()) {
            bodyHtml += "<p>Note/Reason: " + note + "</p>";
        }

        bodyHtml += "<p>Please log in to the system to view the latest details.</p>";
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