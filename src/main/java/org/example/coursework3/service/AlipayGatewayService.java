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

    private AlipayClient newClient() {
        if (isBlank(appId) || isBlank(privateKey) || isBlank(publicKey)) {
            throw new MsgException("支付宝配置不完整，请设置 alipay.app-id/private-key/public-key");
        }
        return new DefaultAlipayClient(gatewayUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2");
    }

    public String precreate(String outTradeNo, String amount, String subject) {
        try {
            AlipayClient client = newClient();
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(outTradeNo);
            model.setTotalAmount(amount);
            model.setSubject(subject);
            model.setTimeoutExpress("15m");
            request.setBizModel(model);
            if (!isBlank(notifyUrl)) {
                request.setNotifyUrl(notifyUrl);
            }
            AlipayTradePrecreateResponse response = client.execute(request);
            if (!response.isSuccess() || isBlank(response.getQrCode())) {
                throw new MsgException("支付宝预下单失败: " + safe(response.getSubMsg(), response.getMsg()));
            }
            return response.getQrCode();
        } catch (AlipayApiException e) {
            throw new MsgException("支付宝预下单异常: " + e.getErrMsg());
        }
    }

    public String queryTradeStatus(String outTradeNo) {
        try {
            AlipayClient client = newClient();
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(outTradeNo);
            request.setBizModel(model);
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess()) {
                throw new MsgException("支付宝查询失败: " + safe(response.getSubMsg(), response.getMsg()));
            }
            return response.getTradeStatus();
        } catch (AlipayApiException e) {
            throw new MsgException("支付宝查询异常: " + e.getErrMsg());
        }
    }

    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(params, publicKey, "UTF-8", "RSA2");
        } catch (AlipayApiException e) {
            return false;
        }
    }

    private static String safe(String first, String second) {
        if (!isBlank(first)) return first;
        if (!isBlank(second)) return second;
        return "unknown error";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
