/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.payments.gateway;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.payments.PaymentConstants;
import org.apache.fineract.payments.data.AlipayRequest;
import org.apache.fineract.payments.data.AlipayResponse;
import org.apache.fineract.payments.data.AlipayTransferRequest;
import org.apache.fineract.payments.data.UnionPayRequest;
import org.apache.fineract.payments.data.UnionPayResponse;
import org.apache.fineract.payments.data.UnionPayTransferRequest;
import org.apache.fineract.payments.data.WeChatPayRequest;
import org.apache.fineract.payments.data.WeChatPayResponse;
import org.apache.fineract.payments.data.WeChatPayTransferRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final UnionPayGateway unionPayGateway;
    private final AlipayGateway alipayGateway;
    private final WeChatPayGateway weChatPayGateway;

    public UnionPayResponse executeUnionPay(UnionPayRequest request) {
        return unionPayGateway.execute(request);
    }

    public UnionPayResponse transferUnionPay(UnionPayTransferRequest request) {
        return unionPayGateway.transfer(request);
    }

    public AlipayResponse executeAlipay(AlipayRequest request) {
        return alipayGateway.execute(request);
    }

    public AlipayResponse transferAlipay(AlipayTransferRequest request) {
        return alipayGateway.transfer(request);
    }

    public WeChatPayResponse executeWeChatPay(WeChatPayRequest request) {
        return weChatPayGateway.execute(request);
    }

    public WeChatPayResponse transferWeChatPay(WeChatPayTransferRequest request) {
        return weChatPayGateway.transfer(request);
    }

    public boolean isMethodEnabled(String methodName) {
        return switch (methodName.toLowerCase(Locale.ROOT)) {
            case PaymentConstants.METHOD_UNIONPAY -> unionPayGateway.isEnabled();
            case PaymentConstants.METHOD_ALIPAY -> alipayGateway.isEnabled();
            case PaymentConstants.METHOD_WECHATPAY -> weChatPayGateway.isEnabled();
            default -> throw new IllegalArgumentException("Unknown payment method: " + methodName);
        };
    }
}
