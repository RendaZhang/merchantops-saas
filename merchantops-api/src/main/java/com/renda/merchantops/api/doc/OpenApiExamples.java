package com.renda.merchantops.api.doc;

public final class OpenApiExamples {

    private OpenApiExamples() {
    }

    public static final String LOGIN_REQUEST_ADMIN = "{\"tenantCode\":\"demo-shop\",\"username\":\"admin\",\"password\":\"123456\"}";
    public static final String LOGIN_REQUEST_OPS = "{\"tenantCode\":\"demo-shop\",\"username\":\"ops\",\"password\":\"123456\"}";
    public static final String LOGIN_REQUEST_VIEWER = "{\"tenantCode\":\"demo-shop\",\"username\":\"viewer\",\"password\":\"123456\"}";
    public static final String REQ_DEV_ECHO = "{\"message\":\"hello merchantops\"}";
    public static final String REQ_USER_CREATE = "{\"username\":\"cashier\",\"displayName\":\"Cashier User\",\"email\":\"cashier@demo-shop.local\",\"password\":\"123456\",\"roleCodes\":[\"READ_ONLY\"]}";

    public static final String RESP_SUCCESS_LOGIN = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"accessToken\":\"<jwt-token>\",\"tokenType\":\"Bearer\",\"expiresIn\":7200}}";
    public static final String RESP_BAD_REQUEST_CREDENTIAL = "{\"code\":\"BAD_REQUEST\",\"message\":\"username or password is incorrect\",\"data\":null}";
    public static final String RESP_FORBIDDEN_USER_INACTIVE = "{\"code\":\"FORBIDDEN\",\"message\":\"user is not active\",\"data\":null}";
    public static final String RESP_BAD_REQUEST_USERNAME_EXISTS = "{\"code\":\"BAD_REQUEST\",\"message\":\"username already exists in tenant\",\"data\":null}";
    public static final String RESP_BAD_REQUEST_ROLE_CODES = "{\"code\":\"BAD_REQUEST\",\"message\":\"roleCodes must exist in current tenant\",\"data\":null}";

    public static final String RESP_UNAUTHORIZED = "{\"code\":\"UNAUTHORIZED\",\"message\":\"authentication required\",\"data\":null}";
    public static final String RESP_FORBIDDEN = "{\"code\":\"FORBIDDEN\",\"message\":\"permission denied\",\"data\":null}";
    public static final String RESP_VALIDATION_ERROR_ECHO = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"message: message must not be blank\",\"data\":null}";
    public static final String RESP_VALIDATION_ERROR_USER_CREATE = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"username: username must not be blank\",\"data\":null}";
    public static final String RESP_VALIDATION_ERROR_PASSWORD_WHITESPACE = "{\"code\":\"VALIDATION_ERROR\",\"message\":\"password: password must not start or end with whitespace\",\"data\":null}";

    public static final String RESP_HEALTH = "{\"status\":\"UP\",\"service\":\"merchantops-saas\"}";
    public static final String RESP_CONTEXT = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"tenantId\":1,\"tenantCode\":\"demo-shop\",\"userId\":1,\"username\":\"admin\"}}";
    public static final String RESP_USER_PROFILE = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"userId\":1,\"tenantId\":1,\"tenantCode\":\"demo-shop\",\"username\":\"admin\",\"roles\":[\"TENANT_ADMIN\"],\"permissions\":[\"USER_READ\",\"USER_WRITE\",\"ORDER_READ\",\"BILLING_READ\",\"FEATURE_FLAG_MANAGE\"]}}";
    public static final String RESP_USER_LIST = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"items\":[{\"id\":1,\"username\":\"admin\",\"displayName\":\"Demo Admin\",\"email\":\"admin@demo-shop.local\",\"status\":\"ACTIVE\"},{\"id\":2,\"username\":\"ops\",\"displayName\":\"Ops User\",\"email\":\"ops@demo-shop.local\",\"status\":\"ACTIVE\"},{\"id\":3,\"username\":\"viewer\",\"displayName\":\"Viewer User\",\"email\":\"viewer@demo-shop.local\",\"status\":\"ACTIVE\"}],\"page\":0,\"size\":10,\"total\":3,\"totalPages\":1}}";
    public static final String RESP_USER_CREATED = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"id\":5,\"tenantId\":1,\"username\":\"cashier\",\"displayName\":\"Cashier User\",\"email\":\"cashier@demo-shop.local\",\"status\":\"ACTIVE\",\"roleCodes\":[\"READ_ONLY\"],\"createdAt\":\"2026-03-10T11:00:00\",\"updatedAt\":\"2026-03-10T11:00:00\"}}";

    public static final String RESP_DEV_PING = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"status\":\"UP\",\"module\":\"merchantops-api\"}}";
    public static final String RESP_DEV_ECHO = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"message\":\"hello merchantops\"}}";
    public static final String RESP_BIZ_ERROR = "{\"code\":\"BIZ_ERROR\",\"message\":\"demo business exception\",\"data\":null}";

    public static final String RESP_RBAC_READ_USERS = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"action\":\"read users\",\"result\":\"allowed\"}}";
    public static final String RESP_RBAC_MANAGE_USERS = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"action\":\"manage users\",\"result\":\"allowed\"}}";
    public static final String RESP_RBAC_FEATURE_FLAGS = "{\"code\":\"SUCCESS\",\"message\":\"ok\",\"data\":{\"action\":\"manage feature flags\",\"result\":\"allowed\"}}";
}
