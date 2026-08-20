# Apache Fineract 东南亚三国上线前检查清单

## 文件索引

| 文件 | 说明 | 适用国家 |
|------|------|---------|
| [PHILIPPINES_CHECKLIST.md](file:///d:/workspace/Mifos%20X%202/fineract/deploy/sea/checklist/PHILIPPINES_CHECKLIST.md) | 菲律宾上线检查清单 | 🇵🇭 Philippines |
| [INDONESIA_CHECKLIST.md](file:///d:/workspace/Mifos%20X%202/fineract/deploy/sea/checklist/INDONESIA_CHECKLIST.md) | 印度尼西亚上线检查清单 | 🇮🇩 Indonesia |
| [CAMBODIA_CHECKLIST.md](file:///d:/workspace/Mifos%20X%202/fineract/deploy/sea/checklist/CAMBODIA_CHECKLIST.md) | 柬埔寨上线检查清单 | 🇰🇭 Cambodia |
| [COMMON_TECHNICAL_CHECKLIST.md](file:///d:/workspace/Mifos%20X%202/fineract/deploy/sea/checklist/COMMON_TECHNICAL_CHECKLIST.md) | 通用技术检查清单 | 所有国家 |

---

## 使用说明

### 1. 执行顺序

建议按以下顺序执行检查：

```
Step 1: COMMON_TECHNICAL_CHECKLIST.md (通用技术检查)
         ↓
Step 2: 国家特定检查清单
         ├── 菲律宾 → PHILIPPINES_CHECKLIST.md
         ├── 印尼   → INDONESIA_CHECKLIST.md
         └── 柬埔寨 → CAMBODIA_CHECKLIST.md
```

### 2. 检查分类概览

所有清单涵盖以下 10 大类别：

| 类别 | 说明 | 通用/国家特定 |
|------|------|-------------|
| **监管合规** | 牌照、数据隐私、AML、消费者保护 | 国家特定 |
| **支付对接** | 支付网关集成与测试 | 国家特定 |
| **基础设施** | 云、网络、数据库、安全 | 混合 |
| **业务功能** | 核心业务流程验证 | 通用 |
| **用户体验** | 本地化、国际化、移动端 | 国家特定 |
| **监控告警** | 系统、应用、业务监控 | 通用 |
| **灾备应急** | 备份、恢复、应急预案 | 通用 |
| **培训文档** | 团队培训、文档就绪 | 通用 |
| **上线检查** | 灰度测试、全量上线 | 通用 |
| **上线回顾** | 30 天回顾与改进 | 通用 |

### 3. 建议时间线

```
T-8 周:  开始执行清单
T-6 周:  基础设施部署完成
T-4 周:  支付对接完成
T-3 周:  功能测试通过
T-2 周:  灰度测试开始
T-1 周:  预上线检查完成
T-Day:   正式上线
T+30天:  上线回顾
```

### 4. 关键路径检查项

以下为最关键的检查项，**必须全部通过**才能上线：

- ☐ 监管牌照已获批
- ☐ 支付网关沙箱全链路测试通过
- ☐ 数据库备份恢复测试通过
- ☐ 安全渗透测试无高危漏洞
- ☐ 灰度测试期间支付成功率 > 99.9%
- ☐ 无数据不一致
- ☐ 应急联系人已确认

---

## 各国特殊关注点

### 🇵🇭 菲律宾 (BSP)
- **数据隐私**: Data Privacy Act of 2012，需通过 DPC 的 PIA 评估
- **支付生态**: GCash (Globe/Alipay) + Maya (PLDT) + PESONet
- **英语环境**: 语言障碍较低，但需考虑本地语言
- **网络状况**: 相对较好，4G 覆盖率高

### 🇮🇩 印度尼西亚 (OJK)
- **数据本地化**: 必须存储在印尼境内
- **支付生态**: QRIS + Gojek Pay + OVO + ShopeePay
- **人口规模**: 2.7 亿人口，规模效应大
- **网络状况**: 大城市 4G，农村较弱

### 🇰🇭 柬埔寨 (NBC)
- **外汇管制**: 严格的外汇管理政策
- **支付生态**: Wing Money + PiPay + Bongloy
- **双币种**: KHR + USD 常用
- **网络状况**: 基础设施相对薄弱，需弱网优化
- **电力供应**: 需备用电源

---

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0 | 2026-08-17 | 初版发布，涵盖三国完整检查项 |
