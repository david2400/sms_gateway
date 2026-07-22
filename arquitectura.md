# File Tree: sms_gateway

**Generated:** 7/22/2026, 7:46:19 AM
**Root Path:** `c:\AppServ\java\sms_gateway`

```
├── 📁 .mvn
│   └── 📁 wrapper
│       └── 📄 maven-wrapper.properties
├── 📁 src
│   ├── 📁 main
│   │   ├── 📁 java
│   │   │   └── 📁 com
│   │   │       └── 📁 essenza
│   │   │           └── 📁 sms_gateway
│   │   │               ├── 📁 config
│   │   │               │   ├── ☕ ApiPrefixConfiguration.java
│   │   │               │   ├── ☕ AsyncConfig.java
│   │   │               │   ├── ☕ MaestraConnectionConfig.java
│   │   │               │   ├── ☕ MaestraPackageScanner.java
│   │   │               │   └── ☕ OpenApiConfig.java
│   │   │               ├── 📁 modules
│   │   │               │   └── 📁 SMSService
│   │   │               │       ├── 📁 application
│   │   │               │       │   ├── 📁 input
│   │   │               │       │   │   ├── 📁 Campanas
│   │   │               │       │   │   │   ├── ☕ CreateSmsCampaignUseCase.java
│   │   │               │       │   │   │   └── ☕ GetSmsCampaignResultsUseCase.java
│   │   │               │       │   │   ├── 📁 Otp
│   │   │               │       │   │   │   ├── ☕ SendOtpAutoGenerateUseCase.java
│   │   │               │       │   │   │   ├── ☕ SendOtpDefaultMessageUseCase.java
│   │   │               │       │   │   │   └── ☕ SendOtpUseCase.java
│   │   │               │       │   │   └── 📁 Sms
│   │   │               │       │   │       ├── ☕ ManageSmsProvidersUseCase.java
│   │   │               │       │   │       ├── ☕ SendSmsBatchUseCase.java
│   │   │               │       │   │       ├── ☕ SendSmsUseCase.java
│   │   │               │       │   │       ├── ☕ SendSmsWithTypeUseCase.java
│   │   │               │       │   │       └── ☕ SmsServiceAvailabilityUseCase.java
│   │   │               │       │   └── 📁 services
│   │   │               │       │       ├── ☕ SmsCampaniasService.java
│   │   │               │       │       └── ☕ SmsOperationsService.java
│   │   │               │       ├── 📁 domain
│   │   │               │       │   ├── 📁 dto
│   │   │               │       │   │   ├── 📁 Campanas
│   │   │               │       │   │   │   ├── ☕ CreateSmsCampaignCommand.java
│   │   │               │       │   │   │   └── ☕ SmsCampaignResultQuery.java
│   │   │               │       │   │   ├── 📁 Otp
│   │   │               │       │   │   │   ├── ☕ GenerateOtpTokenCommand.java
│   │   │               │       │   │   │   ├── ☕ OtpSendResult.java
│   │   │               │       │   │   │   └── ☕ SendOtpRequest.java
│   │   │               │       │   │   ├── 📁 Sms
│   │   │               │       │   │   │   ├── ☕ SendInalambriaBatchCommand.java
│   │   │               │       │   │   │   ├── ☕ SendSMSCommand.java
│   │   │               │       │   │   │   ├── ☕ SendSmsBatchCommand.java
│   │   │               │       │   │   │   ├── ☕ SmsMessageType.java
│   │   │               │       │   │   │   └── ☕ SmsTypeRequest.java
│   │   │               │       │   │   └── 📁 Usuario
│   │   │               │       │   │       ├── ☕ ActualizarUsuarioDto.java
│   │   │               │       │   │       └── ☕ ActualizarUsuarioResult.java
│   │   │               │       │   ├── 📁 model
│   │   │               │       │   │   ├── ☕ OtpTokenRecord.java
│   │   │               │       │   │   ├── ☕ OtpTokenResult.java
│   │   │               │       │   │   ├── ☕ PhoneNumber.java
│   │   │               │       │   │   ├── ☕ ProviderCapabilities.java
│   │   │               │       │   │   ├── ☕ SMS.java
│   │   │               │       │   │   ├── ☕ SMSPriority.java
│   │   │               │       │   │   ├── ☕ SMSResult.java
│   │   │               │       │   │   ├── ☕ SmsBatchResult.java
│   │   │               │       │   │   ├── ☕ SmsCampaignOperationResult.java
│   │   │               │       │   │   ├── ☕ SmsCampaignReport.java
│   │   │               │       │   │   ├── ☕ SmsProvider.java
│   │   │               │       │   │   └── ☕ SmsProviderConfig.java
│   │   │               │       │   ├── 📁 ports
│   │   │               │       │   │   ├── ☕ MessageRouterPort.java
│   │   │               │       │   │   ├── ☕ SmsCampaignPort.java
│   │   │               │       │   │   ├── ☕ SmsInalambriaPort.java
│   │   │               │       │   │   ├── ☕ SmsProviderPort.java
│   │   │               │       │   │   └── ☕ UsuarioServicePort.java
│   │   │               │       │   └── 📁 services
│   │   │               │       │       ├── ☕ MessageOrchestrator.java
│   │   │               │       │       └── ☕ PinGeneratorService.java
│   │   │               │       └── 📁 infrastructure
│   │   │               │           ├── 📁 config
│   │   │               │           │   ├── ☕ AsyncConfiguration.java
│   │   │               │           │   ├── ☕ InalambriaAccountsProperties.java
│   │   │               │           │   ├── ☕ SMSServiceConfiguration.java
│   │   │               │           │   ├── ☕ SmsHttpClientConfig.java
│   │   │               │           │   ├── ☕ SmsProviderAutoConfiguration.java
│   │   │               │           │   ├── ☕ SmsProviderRouterConfig.java
│   │   │               │           │   ├── ☕ SmsProvidersProperties.java
│   │   │               │           │   └── ☕ UsuarioServiceProperties.java
│   │   │               │           ├── 📁 inbound
│   │   │               │           │   └── 📁 rest
│   │   │               │           │       ├── ☕ OtpController.java
│   │   │               │           │       ├── ☕ SmsCampaignController.java
│   │   │               │           │       └── ☕ SmsController.java
│   │   │               │           └── 📁 outbound
│   │   │               │               ├── 📁 adapters
│   │   │               │               │   ├── ☕ Go4ClientsSmsCampaignAdapter.java
│   │   │               │               │   ├── ☕ InalambriaHttpClient.java
│   │   │               │               │   ├── ☕ InalambriaPayloadBuilder.java
│   │   │               │               │   ├── ☕ InalambriaResponseTransformer.java
│   │   │               │               │   ├── ☕ InalambriaSmsAdapter.java
│   │   │               │               │   └── ☕ UsuarioServiceAdapter.java
│   │   │               │               ├── 📁 config
│   │   │               │               │   └── ☕ JacksonConfig.java
│   │   │               │               ├── 📁 mappers
│   │   │               │               │   ├── 📁 Go4Clients
│   │   │               │               │   │   ├── ☕ Go4ClientsCampaignToPayloadMapper.java
│   │   │               │               │   │   ├── ☕ Go4ClientsHttpRequestMapper.java
│   │   │               │               │   │   └── ☕ Go4ClientsResponseToDomainMapper.java
│   │   │               │               │   └── 📁 inalambria
│   │   │               │               │       ├── ☕ EmptyArraySerializer.java
│   │   │               │               │       ├── ☕ InalambriaCommandToPayloadMapper.java
│   │   │               │               │       ├── ☕ InalambriaHttpRequestMapper.java
│   │   │               │               │       └── ☕ InalambriaResponseToDomainMapper.java
│   │   │               │               └── 📁 providers
│   │   │               │                   ├── 📁 base
│   │   │               │                   │   ├── ☕ AbstractSmsProvider.java
│   │   │               │                   │   └── ☕ NewProviderTemplate.java
│   │   │               │                   ├── 📁 inalambria
│   │   │               │                   │   └── ☕ InalambriaAccountOrchestrator.java
│   │   │               │                   ├── 📁 routing
│   │   │               │                   │   ├── ☕ ProviderExecutionTracker.java
│   │   │               │                   │   ├── ☕ ProviderHealthStatus.java
│   │   │               │                   │   ├── ☕ ProviderResolver.java
│   │   │               │                   │   ├── ☕ ProviderRoutingStrategy.java
│   │   │               │                   │   ├── ☕ SmsProviderRegistry.java
│   │   │               │                   │   └── ☕ SmsProviderRouter.java
│   │   │               │                   ├── ☕ Go4ClientsSmsProvider.java
│   │   │               │                   ├── ☕ InalambriaAccountType.java
│   │   │               │                   ├── ☕ InalambriaSmsProvider.java
│   │   │               │                   └── ☕ MockSmsProvider.java
│   │   │               ├── 📁 security
│   │   │               │   └── ☕ SpringSecurityConfig.java
│   │   │               ├── 📁 shared
│   │   │               │   ├── 📁 common
│   │   │               │   │   ├── 📁 anotations
│   │   │               │   │   │   ├── ☕ ForeignKey.java
│   │   │               │   │   │   └── ☕ NullAttribute.java
│   │   │               │   │   ├── 📁 domain
│   │   │               │   │   │   ├── 📁 dto
│   │   │               │   │   │   │   ├── ☕ AuditDatesDto.java
│   │   │               │   │   │   │   ├── ☕ BaseDto.java
│   │   │               │   │   │   │   ├── ☕ BaseStateDto.java
│   │   │               │   │   │   │   ├── ☕ InformacionUsuarioDto.java
│   │   │               │   │   │   │   ├── ☕ LoginDto.java
│   │   │               │   │   │   │   └── ☕ UsuarioDto.java
│   │   │               │   │   │   └── 📁 entity
│   │   │               │   │   │       ├── ☕ AuditDatesEntity.java
│   │   │               │   │   │       ├── ☕ BaseEntity.java
│   │   │               │   │   │       └── ☕ BaseStateEntity.java
│   │   │               │   │   ├── 📁 functions
│   │   │               │   │   │   └── ☕ TriFunction.java
│   │   │               │   │   ├── 📁 genericService
│   │   │               │   │   │   ├── ☕ DomainGenericService.java
│   │   │               │   │   │   └── ☕ DomainGenericServiceImpl.java
│   │   │               │   │   ├── 📁 modelmapper
│   │   │               │   │   │   └── ☕ Mapper.java
│   │   │               │   │   ├── 📁 repository
│   │   │               │   │   │   ├── ☕ DynamicQueryService.java
│   │   │               │   │   │   └── ☕ JpaRepositoryExtended.java
│   │   │               │   │   ├── 📁 request
│   │   │               │   │   │   └── ☕ RequestType.java
│   │   │               │   │   ├── 📁 response
│   │   │               │   │   │   ├── 📁 entity
│   │   │               │   │   │   │   └── ☕ Response.java
│   │   │               │   │   │   └── 📁 service
│   │   │               │   │   │       └── ☕ ResponseService.java
│   │   │               │   │   └── 📁 service
│   │   │               │   │       └── ☕ GeneralEntityService.java
│   │   │               │   └── 📁 util
│   │   │               │       ├── ☕ EmailSender.java
│   │   │               │       ├── ☕ FtpUploadFile.java
│   │   │               │       ├── ☕ Helper.java
│   │   │               │       ├── ☕ HelpersUsuarios.java
│   │   │               │       ├── ☕ HtmlToPdfGenerator.java
│   │   │               │       ├── ☕ MultipartFileUtil.java
│   │   │               │       ├── ☕ ObjectFlattener.java
│   │   │               │       ├── ☕ ObjectMapperUtils.java
│   │   │               │       └── ☕ Utilities.java
│   │   │               └── ☕ SmsGatewayApplication.java
│   │   └── 📁 resources
│   │       └── ⚙️ application.yaml
│   └── 📁 test
│       └── 📁 java
│           └── 📁 com
│               └── 📁 essenza
│                   └── 📁 sms_gateway
│                       └── ☕ SmsGatewayApplicationTests.java
├── ⚙️ .gitattributes
├── ⚙️ .gitignore
├── 📄 mvnw
├── 📄 mvnw.cmd
└── ⚙️ pom.xml
```

---
*Generated by FileTree Pro Extension*