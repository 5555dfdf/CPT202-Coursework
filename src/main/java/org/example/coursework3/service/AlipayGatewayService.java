package org.example.coursework3.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.example.coursework3.exception.MsgException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for interacting with the Alipay Open Platform.
 * Provides functionality for generating payment QR codes, querying transaction status,
 * and verifying asynchronous notifications.
 */
@Service
public class AlipayGatewayService {
    @Value("${alipay.gateway-url:https://openapi.alipaydev.com/gateway.do}")
    private String gatewayUrl;

    @Value("${alipay.app-id:}")
    private String appId;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.public-key:}")
    private String publicKey;

    @Value("${alipay.notify-url:}")
    private String notifyUrl;

    /**
     * Initializes and returns an AlipayClient instance.
     * Uses RSA2 (SHA256withRSA) for digital signatures.
     *
     * @return Initialized AlipayClient
     * @throws MsgException if required configuration is missing
     */
    private AlipayClient newClient() {
        if (isBlank(appId) || isBlank(privateKey) || isBlank(publicKey)) {
            throw new MsgException("Incomplete Alipay configuration. Please set alipay.app-id/private-key/public-key.");
        }
        return new DefaultAlipayClient(gatewayUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2");
    }
    /**
     * Pre-creates an Alipay transaction (Face-to-Face Payment).
     * This generates a payment QR code string that the user can scan to complete payment.
     *
     * @param outTradeNo The unique merchant-side order ID.
     * @param amount     Total transaction amount (string representation, e.g., "10.00").
     * @param subject    Brief description/title of the transaction.
     * @return The QR code string (URL format).
     * @throws MsgException if the API call fails or the response is invalid.
     */
    public String precreate(String outTradeNo, String amount, String subject) {
        try {
            AlipayClient client = newClient();
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            // Set business parameters
            model.setOutTradeNo(outTradeNo);
            model.setTotalAmount(amount);
            model.setSubject(subject);
            model.setTimeoutExpress("15m");
            request.setBizModel(model);
            // Set async notification URL if provided
            if (!isBlank(notifyUrl)) {
                request.setNotifyUrl(notifyUrl);
            }
            AlipayTradePrecreateResponse response = client.execute(request);
            // Validate response success and presence of QR code
            if (!response.isSuccess() || isBlank(response.getQrCode())) {
                throw new MsgException("Alipay pre-order failed: " + safe(response.getSubMsg(), response.getMsg()));
            }
            return response.getQrCode();
        } catch (AlipayApiException e) {
            throw new MsgException("Alipay pre-order exception: " + e.getErrMsg());
        }
    }

    /**
     * Queries the status of an existing Alipay transaction.
     *
     * @param outTradeNo The merchant-side order ID to query.
     * @return The trade status (e.g., TRADE_SUCCESS, TRADE_CLOSED, WAIT_BUYER_PAY).
     * @throws MsgException if the query fails or transaction is not found.
     */
    public String queryTradeStatus(String outTradeNo) {
        try {
            AlipayClient client = newClient();
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(outTradeNo);
            request.setBizModel(model);
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess()) {
                throw new MsgException("Alipay query failed: " + safe(response.getSubMsg(), response.getMsg()));
            }
            return response.getTradeStatus();
        } catch (AlipayApiException e) {
            throw new MsgException("Alipay query exception: " + e.getErrMsg());
        }
    }

    /**
     * Verifies the authenticity of an asynchronous callback/notification from Alipay.
     * Uses the RSA public key to check the signature of the parameters map.
     *
     * @param params Map containing all parameters received in the callback.
     * @return true if the signature is valid; false otherwise.
     */
    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(params, publicKey, "UTF-8", "RSA2");
        } catch (AlipayApiException e) {
            return false;
        }
    }
    /**
     * Utility to prioritize error messages (sub_msg usually contains specific business errors).
     */
    private static String safe(String first, String second) {
        if (!isBlank(first)) return first;
        if (!isBlank(second)) return second;
        return "unknown error";
    }
    /**
     * Standard string null/empty/blank check.
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
