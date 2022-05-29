package com.example.demo.util;

import java.text.SimpleDateFormat;

public interface TraceOrgCommonConstants
{
    String WHITE_LABEL = "Motadata";

    String CURRENT_DIR = System.getProperty("user.dir");

    String PATH_SEPARATOR = System.getProperty("file.separator");

    String LINK_SEPARATOR = "~";

    String NEW_LINE = System.lineSeparator();

    String OS_NAME = System.getProperty("os.name");

    SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    String CLIENT_KEY = "motadata_client";

    String SECRET_KEY = "motadata_secret";

    String PLUGIN_ID = "plugin-id";

    String ERROR_CODE = "error-code";

    String ERROR_PING_FAILED = "Ping Failed [%s]";

    String ERROR_SERVICE_DOWN = "Port [%s] is not available on [%s]";

    String ERROR_AUTH = "Invalid Credentials [%s]";

    String RESULT = "result";

    String PLUGIN_CONTEXT = "plugin-context";

    String ARP_QUERY = "arp -a";

    String PING_RETRY_COUNT = TraceOrgConfigUtil.loadConfigFile(TraceOrgCommonConstants.IPM_CONF).get("max-ping-check-retry-count");

    String PING_TIMEOUT = TraceOrgConfigUtil.loadConfigFile(TraceOrgCommonConstants.IPM_CONF).get("max-ping-check-timeout");

    String WINDOWS_PING_QUERY = "ping -n "+ TraceOrgCommonConstants.PING_RETRY_COUNT +" -w "+TraceOrgCommonConstants.PING_TIMEOUT +" ";

    String LINUX_PING_QUERY = "ping -c "+ TraceOrgCommonConstants.PING_RETRY_COUNT +" -w 2" + " ";

    String USER_NAME = "userName";

    String HOST_ADDRESS = "hostAddress";

    String CREDENTIAL_NAME = "credentialName";

    String ID = "id";

    String ROLE_ADMIN = "ROLE_ADMIN";

    String AUTHORIZATION_CODE = "authorization_code";

    String PASSWORD = "password";

    String READ = "read";

    String WRITE = "write";

    String LOGIN = "login";

    String URL = "url";

    String LOGIN_URL = "/login";

    String AUTHORIZE_URL = "/oauth/authorize";

	String HOME_URL = "/loadHomePage";

    String HOME_PAGE = "WEB-INF/home";

    String LOGOUT_URL = "/logout.html";

    String USER = "User";

    String TRACE_ORG_GLOBAL_SETTING = "TraceOrgGlobalSetting";

    String ROLE_ = "ROLE_";

    String TRACE_ORG_SUBNET_DETAILS = "TraceOrgSubnetDetails";

    String TRACE_ORG_REPORT_SCHEDULER ="TraceOrgReportScheduler";

    String SUBNET_IP_DETAILS_BY_STATUS = "TraceOrgSubnetIpDetails where status='statusValue'";

    String SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID = "TraceOrgSubnetIpDetails where status='statusValue' and subnetId = 'subnetIdValue' and deactiveStatus = false";

    String STATUS_VALUE = "statusValue";

    String TRACE_ORG_SUBNET_IP_DETAILS = "TraceOrgSubnetIpDetails";

    String TRACE_ORG_DHCP_UTILIZATION = "TraceOrgDhcpUtilization";

    String USERROLE = "UserRole";

    String REDIRECT = "redirect:";

    String ACCESSTOKEN = "accessToken";

    String AUTHORITIES = "authorities";

    String SCAN_SUBNET = "ScanSubnet";

    String SCAN_DHCP = "ScanDhcp";

    String ACTIVE = "active";

    boolean TRUE = true;

    boolean FALSE = false;

    String STRING_FALSE = "false";

    String USER_REST_URL = "/user/";

    String GLOBAL_SETTING_REST_URL = "/globalSetting/";

    String USER_ROLE_REST_URL = "/userRole/";

    String USER_BY_USERNAME = "User where userName='userNameValue'";

    String USER_NAME_VALUE = "userNameValue";

    String SUBNET_BY_SUBNET_ADDRESS = "TraceOrgSubnetDetails where subnetAddress='subnetAddressValue'";

    String SUBNET_ADDRESS_VALUE = "subnetAddressValue";

    String SUBNET_REST_URL = "/subnet/";

    String SUBNET_SCAN_STATUS = "/statusScanSubnet/";

    String SUBNET_IMPORT_STATUS = "/importSubnetStatus/";

    String REPORT_SCHEDULER_REST_URL = "/reportScheduler/";

    String DHCP_SUBNET_REST_URL = "/dhcpSubnet/";

    String NORMAL_SUBNET_REST_URL = "/normalSubnet/";

    String EXPORT_CSV_SUBNET_REST_URL = "/exportCsvSubnet/";

    String EXPORT_PDF_SUBNET_REST_URL = "/exportPdfSubnet/";

    String EXPORT_PDF_NORMAL_SUBNET_REST_URL = "/exportNormalPdfSubnet/";

    String EXPORT_PDF_DHCP_SUBNET_REST_URL = "/exportDhcpPdfSubnet/";

    String EXPORT_ALL_IP_PDF_REST_URL = "/exportAllIpPdf/";

    String EXPORT_ALL_AVAILABLE_IP_PDF_REST_URL = "/exportAllAvailableIpPdf/";

    String EXPORT_ALL_TRANSIENT_IP_PDF_REST_URL = "/exportAllTransientIpPdf/";

    String EXPORT_ALL_RESERVED_IP_PDF_REST_URL = "/exportAllReservedIpPdf/";

    String EXPORT_ALL_USED_IP_PDF_REST_URL = "/exportAllUsedIpPdf/";

    String EXPORT_EVENT_PDF_REST_URL = "/exportEventPdf/";

    String EXPORT_EVENT_CSV_REST_URL = "/exportEventCsv/";

    String EXPORT_EVENT_BY_DATE_PDF_REST_URL = "/exportEventPdfByDate/";

    String EVENT_BY_DATE_REST_URL = "/eventsByDate/";

    String SUBNET_CSV_REST_URL = "/subnetByCSV/";

    String IP_SUMMARY_REST_URL = "/ipSummary/";

    String PING_IP_SUMMARY_REST_URL = "/pingIpSummary/";

    String SUBNET_SCAN_REST_URL = "/scanSubnet/";

    String DHCP_SCAN_REST_URL = "/scanDhcp/";

    String ROGUE_SUBNET_IP_REST_URL = "/rogueSubnetIp/";

    String SUBNET_IP_REST_URL = "/subnetIp/";

    String CONFLICT_SUBNET_IP_REST_URL = "/conflictSubnetIp/";

    String EXPORT_CSV_SUBNET_IP_REST_URL = "/exportCsvSubnetIp/";

    String EXPORT_PDF_SUBNET_IP_REST_URL = "/exportPdfSubnetIp/";

    String EXPORT_PDF_SUBNET_CONFLICT_IP_REST_URL = "/exportPdfSubnetConflictIp/";

    String SUBNET_IP_BY_SUBNET_REST_URL = "/subnetIpBySubnet/";

    String USED_SUBNET_IP_BY_SUBNET_REST_URL = "/usedSubnetIpBySubnet/";

    String AVAILABLE_SUBNET_IP_BY_SUBNET_REST_URL = "/availableSubnetIpBySubnet/";

    String RESERVED_SUBNET_IP_BY_SUBNET_REST_URL = "/reservedSubnetIpBySubnet/";

    String SUBNET_AVAILABLE_IP_REST_URL = "/availableSubnetIp/";

    String SUBNET_RESERVED_IP_REST_URL = "/reservedSubnetIp/";

    String SUBNET_TRANSIENT_IP_REST_URL = "/transientSubnetIp/";

    String SUBNET_USED_IP_REST_URL = "/usedSubnetIp/";

    String SUBNET_CHECK_REST_URL = "/checkSubnet/";

    String BRAND_REST_URL = "/brand/";

    String CISCO = "cisco";

    String WINDOWS = "windows";

    String TRACE_ORG_BRAND = "TraceOrgBrand";

    String SUBNET_ADDRESS = "subnetAddress";

    String MAIL_REST_URL = "/mail/";

    String INSERT_MAIL_REST_URL = "/insertMail/";

    String DATABASE_MAINTENANCE = "/databaseMaintenance/";

    String TRACE_ORG_MAIL_SERVER = "TraceOrgMailServer";

    String CATEGORY_REST_URL = "/category/";

    String TOP_EVENT_REST_URL = "/topEvent/";

    String EVENT_REST_URL = "/event/";

    String EVENT_SUMMARY_REST_URL = "/eventSummary/";

    String DHCP_CREDENTIAL_REST_URL = "/dhcpCredential/";

    String DHCP_UTILIZATION_REST_URL = "/dhcpUtilization/";

    String CISCO_DHCP_CREDENTIAL_REST_URL = "/ciscoDhcpCredential/";

    String WINDOWS_DHCP_CREDENTIAL_REST_URL = "/windowsDhcpCredential/";

    String CHECK_DHCP_CREDENTIAL_REST_URL = "/checkDhcpCredential/";

    String SUBNET_BY_CATEGORY = "/subnetByCategory/";

    String SUBNET_BY_REPORT = "/subnetByReport/";

    String SUBNET_IP_BY_REPORT_TIMELINE = "/subnetIpByReportTimeline/";

    String SUBNET_ROGUE_IP_BY_REPORT_TIMELINE = "/subnetIpRogueByReportTimeline/";

    String TRACE_ORG_CATEGORY = "TraceOrgCategory";

    String TRACE_ORG_EVENT = "TraceOrgEvent";

    String TRACE_ORG_FORGOT_PASSWORD = "TraceOrgForgotPassword";

    String TRACE_ORG_DHCP_CREDENTIAL = "TraceOrgDhcpCredentialDetails";

    String CATEGORY_NAME = "categoryName";

    String MESSAGE = "message";

    String LOGO_PNG = "logo.png";

    String IP_ADDRESS = "ipAddress";

    String AVAILABLE = "Available";

    String TRANSIENT = "Transient";

    String RESERVED = "Reserved";

    String USED = "Used";

    String VENDOR_BY_MAC_ADDRESS = "TraceOrgVendor where vendorMac like '%vendorMacValue%'";

    String VENDOR_MAC_VALUE = "vendorMacValue";

    String SUBNET_IP_BY_SUBNET_ID = "TraceOrgSubnetIpDetails where deactiveStatus=false and subnetId=subnetIdValue order by INET_ATON(ipAddress)";

    String SELECTED_IP_SUBNET_ID = "TraceOrgSubnetIpDetails where deactiveStatus=false and subnetId=subnetIdValue and id in (subnetIdList)";

    String SUBNET_ID_LIST = "subnetIdList";

    String SUBNET_ID_VALUE = "subnetIdValue";

    String SUBNET_ID = "subnetId";

    String TOTAL_IP = "totalIp";

    String USED_IP = "usedIp";

    String AVAILABLE_IP = "availableIp";

    String AVAILABLE_IP_PERCENTAGE = "availableIpPercentage";

    String TRANSIENT_IP = "transientIp";

    String TRANSIENT_IP_PERCENTAGE = "transientIpPercentage";

    String USED_IP_PERCENTAGE = "usedIpPercentage";

    SimpleDateFormat VISUAL_DATE_FORMAT = new SimpleDateFormat("dd MMM, YYYY hh:mm:ss a");

    String CATEGORY_BY_NAME = "TraceOrgCategory where categoryName='categoryNameValue'";

    String CATEGORY_NAME_VALUE = "categoryNameValue";

    String SUBNET_DETAIL_CSV_NAME = "subnetDetails.csv";

    String SUBNET_DETAIL_CSV_PATH = "/csv/subnetDetails.csv";

    String SUBNET_IP_CSV_REST_URL = "/subnetIpByCSV/";

    String ACTIVE_SUBNET_IP_RANGE_REST_URL = "/activeSubnetIpRange/";

    String FORGOT_REST_URL = "/forgotPassword/";

    String NEW_PASSWORD_REST_URL = "/newPassword/";

    String VERIFY_PASSWORD_TOKEN_REST_URL = "/verifyPasswordToken/";

    String UPDATE_SUBNET_IP_RANGE_REST_URL = "/updateSubnetIpRange/";

    String DELETE_SUBNET_IP_RANGE_REST_URL = "/deleteSubnetIpRange/";

    String SUBNET_IP_DETAIL_CSV_NAME = "subnetIpDetails.csv";

    String SUBNET_IP_DETAIL_CSV_PATH = "/csv/subnetIpDetails.csv";

    String SUBNET_IP_DETAILS_BY_IP_ADDRESS= "TraceOrgSubnetIpDetails where ipAddress='ipAddressValue'";

    String IP_ADDRESS_VALUE = "ipAddressValue";

    String SELECT_VENDOR_WITH_COUNT = "Select count(*) as devicenumber,deviceType";

    String VENDOR_COUNT_BY_USED_IP = "TraceOrgSubnetIpDetails where deactiveStatus = false group by deviceType order by devicenumber desc";

    String SELECT_VENDOR_WITH_COUNT_FOR_REPORT = "Select count(*) as devicenumber,deviceType,(COUNT(*) / (SELECT COUNT(*) FROM TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in(subnetIdValue)))* 100 AS Percentage";

    String VENDOR_REST_URL = "/vendor/";

    String VENDOR_COUNT ="VendorCount";

    String VENDOR_NAME = "VendorName";

    String VENDOR_PERCENTAGE = "VendorPercentage";

    String BRAND_NAME = "brandName";

    String CSS_MODE = "cssMode";

    String SCOPE = "scope";

    String EXPIRES_IN = "expires_in";

    String REFRESH_TOKEN = "refresh_token";

    String TOKEN_TYPE = "token_type";

    String ACCESS_TOKEN = "access_token";

    String AUTHORIZATION= "Authorization";

    String BASIC = "Basic ";

    String SET_COOKIE = "Set-Cookie";

    String LOGIN_USER_URL = "/loginUser.html";

    String CHANGE_LOGIN_STATUS_URL = "/changeLoginStatus";

    String CHANGE_LOGOUT_STATUS_URL = "/changeLogoutStatus";

    String LEFT_SQUARE_BRACKET = "[";

    String RIGHT_SQUARE_BRACKET = "]";

    String ALL_IP_REPORT = "All IP";

    String USED_IP_REPORT = "Used IP";

    String RESERVED_IP_REPORT = "Reserved IP";

    String AVAILABLE_IP_REPORT = "Available IP";

    String ROGUE_IP_REPORT = "Rogue IP";

    String CONFLICT_IP_REPORT = "Conflict IP";

    String SUBNET_UTILIZATION_REPORT = "Subnet Utilization";

    String DHCP_UTILIZATION_REPORT = "DHCP Utilization";

    String VENDOR_SUMMARY_REPORT = "Vendor Summary";

    String TRANSIENT_IP_REPORT = "Transient IP";

    String EVENT_LOG_REPORT = "Event Log";

    Integer SCHEDULER_TIMELINE_DAILY = 0;

    Integer SCHEDULER_TIMELINE_WEEKLY = 1;

    Integer SCHEDULER_TIMELINE_MONTHLY = 2;

    String SCAN_TYPE = "scanType";

    short SUBNET_SCAN = 0;

    short DHCP_SCAN = 1;

    String TRACE_ORG_COMMON_UTIL = "traceOrgCommonUtil";

    String TRACE_ORG_SERVICE = "traceOrgService";

    String EXPORT_SUBNET_IP_BY_REPORT_TIMELINE = "/exportsubnetIpByReportTimeline/";

    String EXPORT_SUBNET_IP_CSV_BY_REPORT_TIMELINE = "/exportsubnetIpCsvByReportTimeline/";

    String SUBNET_CATEGORY_REST_URL = "/subnetCategory/";

    // Global Configuration
    String CONFIG_DIR = "config";

    String IPM_CONF = "ipm-conf.yml";

    String DOMAIN_NAME = TraceOrgConfigUtil.loadConfigFile(TraceOrgCommonConstants.IPM_CONF).get("server-host");

    String SERVER_PORT = TraceOrgConfigUtil.loadConfigFile(TraceOrgCommonConstants.IPM_CONF).get("server-port");

    //String PROTOCOL = TraceOrgConfigUtil.loadConfigFile(TraceOrgCommonConstants.IPM_CONF).get("server-protocol");

    String PROTOCOL = "http";

    String AUTH_SERVER_URL = TraceOrgCommonConstants.PROTOCOL+"://"+TraceOrgCommonConstants.DOMAIN_NAME + ":"+TraceOrgCommonConstants.SERVER_PORT;

    String AUTH_SERVER_TOKEN_URL = TraceOrgCommonConstants.PROTOCOL+"://"+TraceOrgCommonConstants.DOMAIN_NAME + ":"+TraceOrgCommonConstants.SERVER_PORT +"/oauth/token";

    String AVAILABILITY_AVAILABLE_PERCENTAGE = "Available (%)";

    String AVAILABILITY_USED_PERCENTAGE = "Used (%)";

    String AVAILABILITY_TRANSIENT_PERCENTAGE = "Transient (%)";

    int BATCH_SIZE = 66000;

    String LOGO_DIR = "/src/main/webapp/images";
}
