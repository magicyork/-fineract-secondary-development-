--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--



-- External Payment Gateway Payment Type Insert Script
-- UnionPay (100), Alipay (101), WeChatPay (102)

-- UnionPay
INSERT INTO m_payment_type (id, value, description, is_cash_payment, order_position, code_name, is_system_defined)
VALUES (100, 'UnionPay', 'UnionPay Credit/Debit Card', 0, 10, 'UNIONPAY', 1)
ON DUPLICATE KEY UPDATE
    value = VALUES(value),
    description = VALUES(description),
    is_cash_payment = VALUES(is_cash_payment),
    order_position = VALUES(order_position),
    code_name = VALUES(code_name),
    is_system_defined = VALUES(is_system_defined);

-- Alipay
INSERT INTO m_payment_type (id, value, description, is_cash_payment, order_position, code_name, is_system_defined)
VALUES (101, 'Alipay', 'Alipay Mobile Payment', 0, 11, 'ALIPAY', 1)
ON DUPLICATE KEY UPDATE
    value = VALUES(value),
    description = VALUES(description),
    is_cash_payment = VALUES(is_cash_payment),
    order_position = VALUES(order_position),
    code_name = VALUES(code_name),
    is_system_defined = VALUES(is_system_defined);

-- WeChat Pay
INSERT INTO m_payment_type (id, value, description, is_cash_payment, order_position, code_name, is_system_defined)
VALUES (102, 'WeChatPay', 'WeChat Pay Mobile Payment', 0, 12, 'WECHATPAY', 1)
ON DUPLICATE KEY UPDATE
    value = VALUES(value),
    description = VALUES(description),
    is_cash_payment = VALUES(is_cash_payment),
    order_position = VALUES(order_position),
    code_name = VALUES(code_name),
    is_system_defined = VALUES(is_system_defined);
