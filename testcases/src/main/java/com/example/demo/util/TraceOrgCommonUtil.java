package com.example.demo.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.*;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.writer.CsvWriter;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.net.util.SubnetUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.multipart.MultipartFile;
import org.xbill.DNS.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@SuppressWarnings("ALL")
public class TraceOrgCommonUtil
{
    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgCommonUtil.class, "GUI / Common Util");

    private static final JSONSerializer m_jsonSerializer = new JSONSerializer();

    private static final JSONDeserializer<HashMap<String, Object>> m_jsonDeserializer = new JSONDeserializer<>();

    private final static AtomicInteger m_logLevel = new AtomicInteger(2);

    public  static AtomicInteger m_scanStatus = new AtomicInteger(0);

    private static final AtomicInteger m_csvImportStatus = new AtomicInteger(0);

    private static final AtomicInteger m_ScanStatus = new AtomicInteger(0);

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private TraceOrgService traceOrgService;

    public static final String error = "Error";

    private AccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();

    public static ConcurrentHashMap<String,String> m_scanSubnet = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String,Object> m_scheduleScanSubnet = new ConcurrentHashMap<>();

    public static Scheduler quartzThread = null;

    public static final String SCHEDULE_SUBNET_SCAN_JOB_CRON_EXPRESSION = "0 * * ? * *";

    public static void initializeQuartzThread()
    {
        try
        {
            quartzThread = new StdSchedulerFactory().getScheduler();

            quartzThread.start();
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }

    public static void createTmpDirForReport() {
        File file = new File(new File(TraceOrgCommonConstants.CURRENT_DIR).getParent() + TraceOrgCommonConstants.PATH_SEPARATOR + "temp");

        if (file.exists() == false) {
            boolean isFileCreated = file.mkdir();
            if (isFileCreated == false) {
                _logger.error(new FileNotFoundException(("Required folders for report download not created.")));
            }
        }
    }

    public String generateUUID()
    {
        SecureRandom random = new SecureRandom();

        BigInteger bigInteger = new BigInteger(30, random);

        return bigInteger.toString(32);

    }

    public String getToken(HttpServletRequest request)
    {
        String token = null;

        if (request.getHeader("Cookie") != null && !request.getHeader("Cookie").isEmpty())
        {

            Cookie[] cookies = request.getCookies();

            if (cookies != null)
            {
                for (Cookie cookie : cookies)
                {
                    if (cookie.getName().equals("token"))
                    {
                        token = cookie.getValue();
                    }
                }
            }
        }
        if(checkToken(token))
        {
            return token;
        }
        else
        {
            return null;
        }
    }

    public static String getAccessToken()
    {
        try
        {
            OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();

            String json = new Gson().toJson(oAuth2Authentication.getDetails());

            Type listType = new TypeToken<TraceOrgDetail>() {}.getType();

            TraceOrgDetail TraceOrgDetail = new Gson().fromJson(json, listType);

            return TraceOrgDetail.getTokenValue();

        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return null;
    }

    public String getUserName(HttpServletRequest request)
    {
        String userName = null;

        try
        {
            if (request.getHeader("Cookie") != null && !request.getHeader("Cookie").isEmpty())
            {
                Cookie[] cookies = request.getCookies();

                if (cookies != null)
                {
                    for (Cookie cookie : cookies)
                    {
                        if (cookie.getName().equals("userName"))
                        {
                            userName = cookie.getValue();
                        }
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return userName;
    }

    public static String getUserName()
    {
        OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();

        return oAuth2Authentication.getName();
    }

    @SuppressWarnings("unchecked")
    public List<String> getAuthorityList(TraceOrgService service, String userName)
    {
        List<String> authorityList = new ArrayList<>();

        try
        {
            if (userName != null)
            {
                List<User> userList = (List<User>) service.commonQuery("", "User where userName='" + userName+ "' and status=true");

                if (userList != null && userList.size() > 0)
                {
                    for (User user : userList)
                    {
                        List<UserRole> userRoleList =(List<UserRole>)service.commonQuery("","UserRole where id='"+ user.getUserRoleId().getId()+"'");

                        if(userRoleList != null && !userRoleList.isEmpty())
                        {
                            for (UserRole userRole : userRoleList)
                            {
                                authorityList.add("ROLE_"+ userRole.getRole().toUpperCase());
                            }
                        }
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return authorityList;
    }

    @SuppressWarnings("unchecked")
    public boolean checkToken(String value)
    {
        boolean tokenStatus = Boolean.FALSE;

        Map<String, Object> response = new HashMap<>();

        try
        {
            if(value != null)
            {
                OAuth2AccessToken token = tokenStore.readAccessToken(value);

                if (token == null)
                {
                    response.put("active", false);
                }
                else if (token.isExpired())
                {
                    response.put("active", false);
                }
                else
                {
                    OAuth2Authentication authentication = tokenStore.readAuthentication(token.getValue());

                    response = (Map<String, Object>)accessTokenConverter.convertAccessToken(token, authentication);

                    response.put("authorities",getAuthorityList(traceOrgService,String.valueOf(response.get("user_name"))));

                    response.put("active", true);

                    tokenStatus = Boolean.TRUE;
                }
            }
            else
            {
                _logger.warn("Token is null..");
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return tokenStatus;
    }

    @SuppressWarnings("unchecked")
    public String currentUserRole(String accessToken)
    {
        Map<String, Object> response = new HashMap<>();

        String role = null;

        try
        {
            OAuth2AccessToken token = tokenStore.readAccessToken(accessToken);

            if(token!=null)
            {
                OAuth2Authentication authentication = tokenStore.readAuthentication(token.getValue());

                response = (Map<String, Object>)accessTokenConverter.convertAccessToken(token, authentication);

                if(getAuthorityList(traceOrgService,String.valueOf(response.get("user_name")))!=null && !getAuthorityList(traceOrgService,String.valueOf(response.get("user_name"))).isEmpty())
                {
                    role = getAuthorityList(traceOrgService,String.valueOf(response.get("user_name"))).get(0);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return role;
    }


    public String currentUserName(String accessToken)
    {
        Map<String, Object> response;

        String userName = null;

        try
        {
            OAuth2AccessToken token = tokenStore.readAccessToken(accessToken);

            if (token == null)
            {
                _logger.warn("Token Invalid");
            }
            else if (token.isExpired())
            {
                _logger.warn("Token Expired");
            }
            else
            {
                OAuth2Authentication authentication = tokenStore.readAuthentication(token.getValue());

                response = (Map<String, Object>)accessTokenConverter.convertAccessToken(token, authentication);

                if(response.get("user_name") != null)
                {
                    userName = response.get("user_name").toString();
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return userName;
    }


    public User currentUser(String accessToken)
    {
        Map<String, Object> response;

        User user = null;

        try
        {
            OAuth2AccessToken token = tokenStore.readAccessToken(accessToken);

            if (token == null)
            {
                _logger.warn("Token Invalid");
            }
            else if (token.isExpired())
            {
                _logger.warn("Token Expired");
            }
            else
            {
                OAuth2Authentication authentication = tokenStore.readAuthentication(token.getValue());

                response = (Map<String, Object>)accessTokenConverter.convertAccessToken(token, authentication);

                if(response.get("user_name") != null)
                {
                    String userName = getStringValue(response.get("user_name"));

                    user = (User)this.traceOrgService.findByUserName(userName);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return user;
    }

    public Response testMailServer(TraceOrgMailServer traceOrgMailServer)
    {
        Response response = new Response();

        try
        {
            TraceOrgCommonUtil.sendMail(traceOrgMailServer.getMailHost(),traceOrgMailServer.getMailPort(),"Test Mail","Hello Admin, Thank You ",traceOrgMailServer.getMailFromEmail(),"dontreplymebuddy@gmail.com",traceOrgMailServer.getMailProtocol(),traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailPassword(),traceOrgMailServer.getMailTimeout());

            response.setSuccess(TraceOrgCommonConstants.TRUE);
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            response.setSuccess(TraceOrgCommonConstants.FALSE);
        }
        return response;
    }


    //Hardik Vala

    public static boolean debugEnabled()
    {
        return 1 >= getLogLevel();
    }

    public static boolean warningEnabled()
    {
        return 3 >= getLogLevel();
    }

    public static boolean infoEnabled()
    {
        return 2 >= getLogLevel();
    }

    public static boolean errorEnabled()
    {
        return 4 >= getLogLevel();
    }

    public static boolean traceEnabled()
    {
        return 0 >= getLogLevel();
    }

    public static void setLogLevel(int logLevel)
    {
        m_logLevel.set(logLevel);
    }

    public static short getLogLevel()
    {
        return m_logLevel.shortValue();
    }

    public static short getSubnetScanStatus()
    {
        return m_scanStatus.shortValue();
    }

    public static void sendMail(String mailServerHost, int mailServerPort, String subject, String message, String sender, String recipients, String securityType, String userName, String password, int timeout) throws Exception
    {
        HtmlEmail email = new HtmlEmail();
        email.setHostName(mailServerHost);
        email.setSmtpPort(mailServerPort);
        email.setFrom(sender);
        //email.setDebug(true);
        email.setSubject(subject);
        if (message != null && message.length() > 0) {
            email.setHtmlMsg(message);
        } else {
            email.setHtmlMsg("Empty Email Body !!!");
        }

        if (recipients != null && recipients.length() > 0) {
            String[] var11 = recipients.split(",");
            int var12 = var11.length;

            for (int var13 = 0; var13 < var12; ++var13) {
                String recipient = var11[var13];
                email.addTo(recipient);
            }
        }

        email.setSocketTimeout(timeout * 1000);
        email.setSocketConnectionTimeout(timeout * 1000);
        if (userName != null && password != null && userName.length() > 0 && password.length() > 0) {
            email.setAuthentication(userName, password);
        }

        if (securityType.equalsIgnoreCase("ssl")) {
            email.setSslSmtpPort(String.valueOf(mailServerPort));
            email.setSSLOnConnect(true);
        } else if (securityType.equalsIgnoreCase("tls")) {
            email.setStartTLSEnabled(true);
        }

        email.send();
    }

    public static String getJSON(Object target) {
        return m_jsonSerializer.deepSerialize(target);
    }

    public static HashMap<String, Object> deserialize(String target) {
        return m_jsonDeserializer.deserialize(target, HashMap.class);
    }

    public static HashMap<String, Object> deserialize(FileReader fileReader) {
        return m_jsonDeserializer.deserialize(fileReader, HashMap.class);
    }

    public static void extractMetricValue(HashMap<String, Object> metricDetails, String counterValue, String counterValues, String key)
    {
        if(!counterValue.trim().matches("\\d+") && !counterValue.trim().matches("\\d+.\\d+"))
        {
            metricDetails.put(key, convertToLongValue(counterValues.trim()));
        }
        else
        {
            metricDetails.put(key, convertToLongValue(counterValue.trim()));
        }

    }

    static int convertToSeconds(long milliSeconds) {
        return (int)(milliSeconds / 1000L);
    }

    static String formatTime(long seconds) {
        long days = seconds / 86400L;
        long hours = seconds % 86400L / 3600L;
        long minutes = seconds % 86400L % 3600L / 60L;
        return days + " days " + hours + " hours " + minutes + " minutes ";
    }

    static int getIntegerValue(Object target)
    {
        int value = 0;
        if (target != null) {
            value = Integer.parseInt(target.toString());
        }

        return value;
    }

    private static short getShortValue(Object target)
    {
        short value = 0;
        if (target != null) {
            value = Short.parseShort(target.toString());
        }

        return value;
    }

    public static float getFloatValue(Object target)
    {
        float value = 0.0F;
        if (target != null) {
            value = Float.parseFloat(target.toString());
        }

        return value;
    }

    public static double getDoubleValue(Object target)
    {
        double value = 0.0D;
        if (target != null) {
            value = Double.parseDouble(target.toString());
        }

        return value;
    }

    static long convertToLongValue(Object target)
    {
        long value = 0L;
        if (target != null && target.toString().length() > 0 && !target.toString().equalsIgnoreCase("-Infinity") && !target.toString().equalsIgnoreCase("Infinity") && !target.toString().equalsIgnoreCase("NaN")) {
            value = Math.round((new BigDecimal(target.toString())).doubleValue());
        }

        return value;
    }

    static long getLongValue(Object target)
    {
        long value = 0L;
        if (target != null) {
            value = Long.parseLong(target.toString());
        }

        return value;
    }

    static HashMap<String, Object> getMapValue(Object target)
    {
        HashMap<String, Object> value = null;
        if (target != null) {
            value = (HashMap<String, Object>) target;
        }

        return value;
    }

    public static byte[] getByteValues(Object target)
    {
        byte[] value = null;
        if (target != null) {
            value = target.toString().getBytes();
        }

        return value;
    }

    public static String getStringValue(Object target)
    {
        String value = null;
        if (target != null) {
            value = String.valueOf(target);
        }

        return value;
    }

    public void fileUpload(MultipartFile multipartFile, HttpServletRequest request)
    {
        String appendFilePath = File.separator;

        String originalFileName = null;

        @SuppressWarnings("deprecation")
        String UPLOAD_PATH = request.getRealPath("/images/") + File.separator + appendFilePath;

        String  imageForReportPath = TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Images"+TraceOrgCommonConstants.PATH_SEPARATOR;

        File file = new File(UPLOAD_PATH);

        if (!file.exists())
        {
            file.mkdirs();
        }

        try
        {
            originalFileName = "logo.png";

            File saveFile = new File(UPLOAD_PATH + originalFileName);

            File reportImageFile = new File(imageForReportPath + originalFileName);

            if (!saveFile.exists())
                saveFile.createNewFile();

            if (!reportImageFile.exists())
                reportImageFile.createNewFile();

            byte[] bytes = multipartFile.getBytes();

            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(saveFile));

            bufferedOutputStream.write(bytes);

            bufferedOutputStream.close();

            BufferedOutputStream bufferedOutputStream2 = new BufferedOutputStream(new FileOutputStream(reportImageFile));

            bufferedOutputStream2.write(bytes);

            bufferedOutputStream2.close();

        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }

    static String resolveHost(String ip,String dnsAddress)
    {
        String result;

        try
        {
            if (Pattern.compile("(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}").matcher(ip).matches())
            {
                result = reverseLookup(ip,dnsAddress);
            }
            else
            {
                result = ip;
            }
        }
        catch (Exception exception)
        {
            result = ip;

            _logger.error(exception);
        }
        return result;
    }

    static String resolveIp(String host,String dnsAddress)
    {
        String result;

        try
        {
            if (!Pattern.compile("(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}").matcher(host).matches())
            {
                result = lookup(host,dnsAddress);
            }
            else
            {
                result = host;
            }
        }
        catch (Exception exception)
        {
            result = host;

            _logger.error(exception);
        }
        return result;
    }

    private static String lookup(String hostIp,String dnsAddress)
    {
        try
        {
            if (dnsAddress != null && dnsAddress.trim().length() > 0)
            {
                Lookup lookup = new Lookup(hostIp, org.xbill.DNS.Type.A);

                String[] dns = dnsAddress.split(",");

                Resolver extendedResolver = new ExtendedResolver(dns);

                extendedResolver.setTimeout(60);

                lookup.setResolver(extendedResolver);

                lookup.setCache(null);

                Record[] records = lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL && records != null && records.length > 0)
                {
                    for (Record record : records)
                    {
                        if (record instanceof ARecord)
                        {
                            hostIp = ((ARecord) record).getAddress().getHostAddress();

                            break;
                        }
                    }
                }
            }
            else
            {
                Lookup lookup = new Lookup(hostIp, org.xbill.DNS.Type.A);

                Resolver extendedResolver = new ExtendedResolver();

                extendedResolver.setTimeout(60);

                lookup.setResolver(extendedResolver);

                lookup.setCache(null);

                Record[] records = lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL && records != null && records.length > 0)
                {
                    for (Record record : records)
                    {
                        if (record instanceof ARecord)
                        {
                            hostIp = ((ARecord) record).getAddress().getHostAddress();

                            break;
                        }
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            _logger.warn("lookup failed for host "+hostIp + " and dns address "+dnsAddress);
        }

        return hostIp;
    }

    private static String reverseLookup(String hostIp,String dnsAddress)
    {
        try
        {
            if (dnsAddress != null && dnsAddress.trim().length() > 0)
            {
                String[] dns = dnsAddress.split(",");

                Resolver extendedResolver = new ExtendedResolver(dns);

                extendedResolver.setTimeout(60);

                Name name = ReverseMap.fromAddress(hostIp);

                Record record = Record.newRecord(name, org.xbill.DNS.Type.PTR, DClass.IN);

                Message query = Message.newQuery(record);

                Message response = extendedResolver.send(query);

                Record[] answers = response.getSectionArray(Section.ANSWER);

                if (answers.length != 0)
                {
                    hostIp = answers[0].rdataToString();

                    if (hostIp.charAt(hostIp.length() - 1) == '.')
                    {
                        hostIp = hostIp.substring(0, hostIp.length() - 1);
                    }
                }
            }

            else
            {
                Resolver extendedResolver = new ExtendedResolver();

                extendedResolver.setTimeout(60);

                Name name = ReverseMap.fromAddress(hostIp);

                Record record = Record.newRecord(name, org.xbill.DNS.Type.PTR, DClass.IN);

                Message query = Message.newQuery(record);

                Message response = extendedResolver.send(query);

                Record[] answers = response.getSectionArray(Section.ANSWER);

                if (answers.length != 0)
                {
                    hostIp = answers[0].rdataToString();

                    if (hostIp.charAt(hostIp.length() - 1) == '.')
                    {
                        hostIp = hostIp.substring(0, hostIp.length() - 1);
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            _logger.warn("reverse lookup failed for host "+hostIp + " and dns address "+dnsAddress);
        }

        return hostIp;
    }

    public static int getMaxPingCheckTimeout()
    {
        int second = 10;

        try
        {
            HashMap<String, String> configDetails = TraceOrgConfigUtil.loadConfigFile(TraceOrgCommonConstants.IPM_CONF);

            if(configDetails != null && configDetails.get("max-ping-check-timeout") != null && configDetails.size() > 0)
            {
                second = TraceOrgCommonUtil.getIntegerValue(configDetails.get("max-ping-check-timeout"));
            }
        }
        catch (Exception var2)
        {
            _logger.error(var2);
        }

        return second;
    }

    static int getMaxPingCheckRetryCount()
    {
        int retry = 3;

        try
        {
            HashMap<String, String> configDetails = TraceOrgConfigUtil.loadConfigFile(TraceOrgCommonConstants.IPM_CONF);

            if(configDetails != null && configDetails.get("max-ping-check-retry-count") != null && configDetails.size() > 0)
            {
                retry = TraceOrgCommonUtil.getIntegerValue(configDetails.get("max-ping-check-retry-count"));
            }
        }
        catch (Exception var2)
        {
            _logger.error(var2);
        }

        return retry;
    }

    static boolean isHostReachable(String ipAddress) throws Exception
    {
        return isHostReachable(ipAddress, (short) getMaxPingCheckRetryCount(), (long) getMaxPingCheckTimeout());
    }

    static boolean isHostReachable(String ipAddress, short retryCount, long timeout) throws Exception
    {
        _logger.debug("pinging " + ipAddress);

        boolean isReachable = false;

        Process process = null;

        BufferedReader bufferedReader = null;

        InputStream inputStream = null;

        String fileSeparator = System.getProperty("file.separator");

        boolean startLogging = false;

        ArrayList<String> commands = new ArrayList<>();

        try
        {
            if(fileSeparator.equalsIgnoreCase("/"))
            {
                commands.add("ping");

                commands.add("-c");

                commands.add(String.valueOf(retryCount));

                commands.add("-w");

                commands.add(String.valueOf(timeout));

                commands.add(ipAddress);

                ProcessBuilder processBuilder = new ProcessBuilder(commands);

                process = processBuilder.start();

                inputStream = process.getInputStream();

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while((line = bufferedReader.readLine()) != null)
                {
                    line = line.toLowerCase();

                    if(line.trim().contains("ping statistics"))
                    {
                        _logger.debug("ping statistics are available of" + ipAddress);

                        startLogging = true;
                    }

                    if(startLogging)
                    {
                        if(line.trim().contains("min"))
                        {
                            isReachable = true;

                            break;
                        }
                        isReachable = false;
                    }
                }

            }
            else if(fileSeparator.equalsIgnoreCase("\\"))
            {
                commands.add("ping");

                commands.add("-n");

                commands.add(String.valueOf(retryCount));

                commands.add("-w");

                commands.add(String.valueOf(timeout));

                commands.add(ipAddress);

                ProcessBuilder processBuilder = new ProcessBuilder(commands);

                process = processBuilder.start();

                inputStream = process.getInputStream();

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while((line = bufferedReader.readLine()) != null)
                {
                    line = line.toLowerCase();

                    if(line.trim().contains("ping statistics"))
                    {
                        _logger.debug("ping statistics are available of" + ipAddress);

                        startLogging = true;
                    }

                    if(startLogging)
                    {
                        if(line.trim().startsWith("ping statistics for "+ipAddress+":"))
                        {
                            isReachable = true;

                            break;
                        }
                        isReachable = false;
                    }
                }

            }

            if(isReachable)
            {
                _logger.debug("[" + ipAddress + "] is reachable.");

            }
            else
            {
                _logger.warn("[" + ipAddress + "] is not reachable.");
            }

            if (inputStream != null)
            {
                inputStream.close();
            }

            if (bufferedReader != null)
            {
                bufferedReader.close();
            }

            if (process != null)
            {
                process.getErrorStream().close();

                process.getOutputStream().close();

                process.getInputStream().close();

                process.destroy();
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            _logger.warn(ipAddress + " host is not reachable");
        }
        return isReachable;
    }

    static boolean isPortReachable(String ipAddress, int port)
    {
        _logger.debug("checking port " + port + " of " + ipAddress);

        boolean result = false;

        try
        {
            Socket socket = new Socket(ipAddress, port);

            socket.close();

            result = true;

            _logger.debug(port + " port is open of " + ipAddress);
        }
        catch (Exception var4)
        {
            _logger.error(var4);

            _logger.warn(port + " port is not open of " + ipAddress);
        }

        return result;
    }
    //Krunal Thakkar

    public boolean checkSubnet(String ip,int cidr)
    {
        boolean result = Boolean.FALSE;

        try
        {
            SubnetUtils utils = new SubnetUtils(ip+"/"+cidr);

            result =  ip.equals(utils.getInfo().getNetworkAddress());
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }

    public String getSubnetMask(String ip,int cidr)
    {
        String subnetMask = "";

        try
        {
            SubnetUtils utils = new SubnetUtils(ip+"/"+cidr);

            subnetMask =  utils.getInfo().getNetmask();
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return subnetMask;
    }

    public Long countTotalIp(String ip,int cidr)
    {
        Long count = 0L;

        try
        {
            SubnetUtils utils = new SubnetUtils(ip+"/"+cidr);

            utils.setInclusiveHostCount(true);

            count =  (long) utils.getInfo().getAddressCount();
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return count;
    }



    /*public boolean ipList(TraceOrgSubnetDetails traceOrgSubnetDetails)
    {

        boolean result = false;

        List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = new ArrayList<>();

        SubnetUtils utils = new SubnetUtils(traceOrgSubnetDetails.getSubnetAddress()+"/"+traceOrgSubnetDetails.getSubnetCidr());

        utils.setInclusiveHostCount(true);

        String[] addresses;

        addresses = utils.getInfo().getAllAddresses();

        List<TraceOrgSubnetDetails> traceOrgSubnetDetailsList = (List<TraceOrgSubnetDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_BY_SUBNET_ADDRESS.replace(TraceOrgCommonConstants.SUBNET_ADDRESS_VALUE,traceOrgSubnetDetails.getSubnetAddress()));

        if(traceOrgSubnetDetailsList != null && !traceOrgSubnetDetailsList.isEmpty())
        {
            for (String address : addresses)
            {
                TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = new TraceOrgSubnetIpDetails();

                traceOrgSubnetIpDetails.setIpAddress(address);

                traceOrgSubnetIpDetails.setSubnetId(traceOrgSubnetDetailsList.get(0));

                traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.AVAILABLE);

                traceOrgSubnetIpDetails.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                traceOrgSubnetIpDetails.setCreatedDate(new Date());

                traceOrgSubnetIpDetails.setModifiedDate(new Date());

                traceOrgSubnetIpDetailsList.add(traceOrgSubnetIpDetails);

            }
            result =  this.traceOrgService.insertAll(traceOrgSubnetIpDetailsList);
        }

        return result;
    }*/

    public boolean ipList(TraceOrgSubnetDetails traceOrgSubnetDetails)
    {
        boolean result = false;

        List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = new ArrayList<>();

        try
        {
            SubnetUtils utils = new SubnetUtils(traceOrgSubnetDetails.getSubnetAddress()+"/"+traceOrgSubnetDetails.getSubnetCidr());

            utils.setInclusiveHostCount(true);

            String[] addresses;

            addresses = utils.getInfo().getAllAddresses();

            List<TraceOrgSubnetDetails> traceOrgSubnetDetailsList = (List<TraceOrgSubnetDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_BY_SUBNET_ADDRESS.replace(TraceOrgCommonConstants.SUBNET_ADDRESS_VALUE,traceOrgSubnetDetails.getSubnetAddress()));

            if(traceOrgSubnetDetailsList != null && !traceOrgSubnetDetailsList.isEmpty())
            {
                TraceOrgSubnetIpDetails traceOrgSubnetIpDetailsFirst = new TraceOrgSubnetIpDetails();

                traceOrgSubnetIpDetailsFirst.setIpAddress(addresses[0]);

                traceOrgSubnetIpDetailsFirst.setSubnetId(traceOrgSubnetDetailsList.get(0));

                traceOrgSubnetIpDetailsFirst.setStatus(TraceOrgCommonConstants.RESERVED);

                traceOrgSubnetIpDetailsFirst.setPreviousStatus(TraceOrgCommonConstants.RESERVED);

                traceOrgSubnetIpDetailsFirst.setCreatedDate(new Date());

                traceOrgSubnetIpDetailsFirst.setModifiedDate(new Date());

                traceOrgSubnetIpDetailsList.add(traceOrgSubnetIpDetailsFirst);

                IntStream.range(1, addresses.length - 1).forEach(index -> {
                    TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = new TraceOrgSubnetIpDetails();

                    traceOrgSubnetIpDetails.setIpAddress(addresses[index]);

                    traceOrgSubnetIpDetails.setSubnetId(traceOrgSubnetDetailsList.get(0));

                    traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.AVAILABLE);

                    traceOrgSubnetIpDetails.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                    traceOrgSubnetIpDetails.setCreatedDate(new Date());

                    traceOrgSubnetIpDetails.setModifiedDate(new Date());

                    traceOrgSubnetIpDetailsList.add(traceOrgSubnetIpDetails);
                });

                TraceOrgSubnetIpDetails traceOrgSubnetIpDetailsLast = new TraceOrgSubnetIpDetails();

                traceOrgSubnetIpDetailsLast.setIpAddress(addresses[addresses.length-1]);

                traceOrgSubnetIpDetailsLast.setSubnetId(traceOrgSubnetDetailsList.get(0));

                traceOrgSubnetIpDetailsLast.setStatus(TraceOrgCommonConstants.RESERVED);

                traceOrgSubnetIpDetailsLast.setPreviousStatus(TraceOrgCommonConstants.RESERVED);

                traceOrgSubnetIpDetailsLast.setCreatedDate(new Date());

                traceOrgSubnetIpDetailsLast.setModifiedDate(new Date());

                traceOrgSubnetIpDetailsList.add(traceOrgSubnetIpDetailsLast);

                result = this.traceOrgService.insertAll(traceOrgSubnetIpDetailsList);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return result;
    }

    public String getIdList(String []subnetIpIdString)
    {
        String idList = "";

        try
        {
            if (subnetIpIdString != null && subnetIpIdString.length > 0)
            {
                Set<String> idSet = new HashSet<>(Arrays.asList(subnetIpIdString));

                // removing subnet IP id from set
                idSet.remove(subnetIpIdString[0]);

                if (!idSet.isEmpty())
                {
                    idList = "'" + StringUtils.join(idSet, "','") + "'";
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return idList;
    }

    public List<TraceOrgSubnetIpDetails> getSubnetIpDetailsList(String[] subnetIpIdString)
    {
        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = new ArrayList<>();

        try
        {
            if (subnetIpIdString.length == 1)
            {
                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_BY_SUBNET_ID.replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,subnetIpIdString[0]));
            }
            else
            {
                String idList = getIdList(subnetIpIdString);

                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SELECTED_IP_SUBNET_ID.replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,subnetIpIdString[0]).replace(TraceOrgCommonConstants.SUBNET_ID_LIST, idList));
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return subnetIpDetailsList;
    }

    public boolean importCSVFile(MultipartFile multipartFile, HttpServletRequest request, String fileName)
    {
        boolean result = false;

        try
        {
            String appendFilePath = File.separator;

            String originalFileName = null;

            @SuppressWarnings("deprecation")
            String UPLOAD_PATH = request.getRealPath("/csv/") + File.separator + appendFilePath;

            File file = new File(UPLOAD_PATH);

            if (!file.exists())
            {
                file.mkdirs();
            }

            originalFileName = fileName;

            File saveFile = new File(UPLOAD_PATH + originalFileName);

            if (!saveFile.exists())
            {
                saveFile.createNewFile();
            }

            byte[] bytes = multipartFile.getBytes();

            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(saveFile));

            bufferedOutputStream.write(bytes);

            bufferedOutputStream.close();

            result = true;
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }

    public boolean checkSubnetFileData(CsvRow csvRow)
    {
        boolean result = false;

        try
        {
            if(csvRow.getField(0).contains("Category Name") && csvRow.getField(1).contains("Subnet Address") && csvRow.getField(2).contains("Subnet Mask")
                    && csvRow.getField(3).contains("Subnet CIDR") && csvRow.getField(4).contains("Subnet Name") && csvRow.getField(5).contains("VLAN Name")
                    && csvRow.getField(6).contains("Location") && csvRow.getField(7).contains("Description") && csvRow.getField(8).contains("DNS Address")
                    && csvRow.getField(9).contains("Scheduled Hours") && csvRow.getField(10).contains("Duration") && csvRow.getField(11).contains("Local Subnet")
                    && csvRow.getField(12).contains("Gateway IP") && csvRow.getField(13).contains("SNMP Community"))
            {
                result = true;
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }
    public boolean checkSubnetIPFileData(CsvRow csvRow)
    {
        boolean result = false;

        try
        {
            if(csvRow.getField(0).contains("IP Address") && csvRow.getField(1).contains("Mac Address") && csvRow.getField(2).contains("Status")
                    && csvRow.getField(3).contains("IP To Dns") && csvRow.getField(4).contains("Dns To Ip") && csvRow.getField(5).contains("Vendor")
                    &&  csvRow.getField(6).contains("Rogue"))
            {
                result = true;
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }

    public boolean insertSubnetIp(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails)
    {
        boolean result = false;

        try
        {
            if(this.traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,TraceOrgCommonConstants.SUBNET_ID,traceOrgSubnetIpDetails.getSubnetId().getId().toString()))
            {
                List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,traceOrgSubnetIpDetails.getIpAddress()));

                if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                {
                    TraceOrgSubnetIpDetails traceOrgSubnetIpDetailsExisted =  traceOrgSubnetIpDetailsList.get(0);

                    traceOrgSubnetIpDetailsExisted.setMacAddress(traceOrgSubnetIpDetails.getMacAddress());

                    if(traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED) && traceOrgSubnetIpDetailsExisted.getMacAddress()!=null)
                    {
                        if(traceOrgSubnetIpDetailsExisted.getPreviousMacAddress()!=null && !traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().isEmpty())
                        {
                            if(!traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().equals(traceOrgSubnetIpDetailsExisted.getMacAddress()))
                            {
                                traceOrgSubnetIpDetailsExisted.setConflictMac(traceOrgSubnetIpDetailsExisted.getPreviousMacAddress());
                            }
                            else
                            {
                                traceOrgSubnetIpDetailsExisted.setConflictMac(null);
                            }
                            traceOrgSubnetIpDetailsExisted.setPreviousMacAddress(traceOrgSubnetIpDetailsExisted.getMacAddress());
                        }
                        else
                        {
                            traceOrgSubnetIpDetailsExisted.setPreviousMacAddress(traceOrgSubnetIpDetails.getMacAddress());
                        }
                    }

                    traceOrgSubnetIpDetailsExisted.setDescription(traceOrgSubnetIpDetails.getDescription());

                    traceOrgSubnetIpDetailsExisted.setDeviceType(traceOrgSubnetIpDetails.getDeviceType());

                    traceOrgSubnetIpDetailsExisted.setDnsStatus(traceOrgSubnetIpDetails.getDnsStatus());

                    traceOrgSubnetIpDetailsExisted.setHostName(traceOrgSubnetIpDetails.getHostName());

                    if(traceOrgSubnetIpDetails.getLastAliveTime()!=null)
                    {
                        traceOrgSubnetIpDetailsExisted.setLastAliveTime(new Date(traceOrgSubnetIpDetails.getLastAliveTime()));
                    }

                    traceOrgSubnetIpDetailsExisted.setIpToDns(traceOrgSubnetIpDetails.getIpToDns());

                    traceOrgSubnetIpDetailsExisted.setDnsToIp(traceOrgSubnetIpDetails.getDnsToIp());

                    //traceOrgSubnetIpDetailsExisted.setDeactiveStatus(traceOrgSubnetIpDetails.isDeactiveStatus());

                    if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.USED) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.AVAILABLE))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.TRANSIENT) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.AVAILABLE))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.TRANSIENT) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.RESERVED) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.RESERVED) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.AVAILABLE))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.RESERVED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else
                    {
                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(traceOrgSubnetIpDetailsExisted.getStatus());

                        traceOrgSubnetIpDetailsExisted.setStatus(traceOrgSubnetIpDetails.getStatus());
                    }

                    boolean updateStatus = this.traceOrgService.insert(traceOrgSubnetIpDetailsExisted);

                    if (updateStatus)
                    {
                        result = true;
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }


    public String exportSubnetIpCsv(HttpServletRequest request, List<TraceOrgSubnetIpDetails> subnetIpDetailsList)
    {
        String fileName = ("Subnet Ip Summary "+subnetIpDetailsList.get(0).getSubnetId().getSubnetAddress()+"_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv").replace(" ","_").replace(":","_").replace(",","");

        try
        {
            File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR + fileName);

            CsvWriter csvWriter = new CsvWriter();

            Collection<String[]> data = new ArrayList<>();

            data.add(new String[] { "IP Address","Mac Address","Status","IP To Dns","Dns To Ip","Vendor","Rogue","Last Alive Time"});

            for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails:subnetIpDetailsList)
            {
                if(traceOrgSubnetIpDetails.isRogueStatus())
                {
                    data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(), traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),traceOrgSubnetIpDetails.getDeviceType(),"Yes",traceOrgSubnetIpDetails.getLastAliveTime()});
                }
                else
                {
                    data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(), traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),traceOrgSubnetIpDetails.getDeviceType(),"No",traceOrgSubnetIpDetails.getLastAliveTime()});
                }
            }

            csvWriter.write(file, StandardCharsets.UTF_8, data);
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return fileName;
    }

    public String exportSubnetCsv(HttpServletRequest request, List<TraceOrgSubnetDetails> subnetDetailsList)
    {
        String fileName = ("Subnet Summary"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv").replace(" ","_").replaceAll(":","_").replace(",","");

        try
        {
            File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR + fileName);

            CsvWriter csvWriter = new CsvWriter();

            Collection<String[]> data = new ArrayList<>();
            data.add(new String[] { "Category Name","Subnet Address","Subnet Mask","Subnet CIDR","Subnet Name" ,"VLAN Name","Location","Description","DNS Address","Scheduled Hours" });

            for(TraceOrgSubnetDetails traceOrgSubnetDetails:subnetDetailsList)
            {
                String vlanName = "";
                if(traceOrgSubnetDetails.getVlanName()!=null && !traceOrgSubnetDetails.getVlanName().isEmpty())
                {
                    vlanName = traceOrgSubnetDetails.getVlanName();
                }

                String location = "";
                if(traceOrgSubnetDetails.getLocation()!=null && !traceOrgSubnetDetails.getLocation().isEmpty())
                {
                    location = traceOrgSubnetDetails.getLocation();
                }

                String description = "";
                if(traceOrgSubnetDetails.getDescription()!=null && !traceOrgSubnetDetails.getDescription().isEmpty())
                {
                    description = traceOrgSubnetDetails.getDescription();
                }

                String dnsAddress = "";
                if(traceOrgSubnetDetails.getDnsAddress()!=null && !traceOrgSubnetDetails.getDnsAddress().isEmpty())
                {
                    dnsAddress = traceOrgSubnetDetails.getDnsAddress();
                }

                data.add(new String[] { traceOrgSubnetDetails.getTraceOrgCategory().getCategoryName(),traceOrgSubnetDetails.getSubnetAddress(),traceOrgSubnetDetails.getSubnetMask(),traceOrgSubnetDetails.getSubnetCidr().toString(),traceOrgSubnetDetails.getSubnetName(),vlanName,location,description,dnsAddress,traceOrgSubnetDetails.getScheduleHour().toString()});
            }

            csvWriter.write(file, StandardCharsets.UTF_8, data);
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return fileName;
    }

    //SUBNET MASK TO CIDR

    public int convertNetmaskToCIDR(InetAddress netmask)
    {
        int cidr = 0;

        try
        {
            byte[] netmaskBytes = netmask.getAddress();

            boolean zero = false;

            for(byte b : netmaskBytes)
            {
                int mask = 0x80;

                for(int index = 0; index < 8; index++)
                {

                    int result = b & mask;

                    if(result == 0)
                    {
                        zero = true;
                    }
                    else if(zero)
                    {
                        throw new IllegalArgumentException("Invalid netmask.");
                    }
                    else
                    {
                        cidr++;
                    }
                    mask >>>= 1;
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return cidr;
    }


    public String exportAllIpReportPdf(Integer exportTimeline ,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and  subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE())and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +"  where subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                List<HashMap<String, Object>> ipSummaryObject = new ArrayList<>();

                Integer availableIp = 0;

                Integer usedIp = 0;

                Integer transientIp = 0;

                gridReport.put("Title", "All Ip Report");

                try
                {
                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("IP Address");

                        add("Status");

                        add("Scope");

                        add("Mac Address");

                        add("Vendor");

                        add("IP To DNS");

                        add("DNS To IP");

                        add("Rogue");

                        add("Last Alive Time");

                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult;


                    for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList)
                    {
                        if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.AVAILABLE))
                        {
                            availableIp++;
                        }
                        else if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.USED))
                        {
                            usedIp++;
                        }
                        else if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.TRANSIENT))
                        {
                            transientIp++;
                        }

                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                        pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                        pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                        pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                        pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                        if(traceOrgSubnetIpDetail.isRogueStatus())
                            pdfResult.add("Yes");
                        else
                            pdfResult.add("No");

                        pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                        pdfResults.add(pdfResult);
                    }

                    HashMap<String, Object> availableIpSummary = new HashMap<>();

                    availableIpSummary.put("status","Available (%)");

                    availableIpSummary.put("value",new DecimalFormat("#.00").format((double)(availableIp*100)/subnetIpDetailsList.size()));

                    HashMap<String, Object> usedIpSummary = new HashMap<>();

                    usedIpSummary.put("status","Used (%)");

                    usedIpSummary.put("value",new DecimalFormat("#.00").format((double)(usedIp*100)/subnetIpDetailsList.size()));

                    HashMap<String, Object> transientIpSummary = new HashMap<>();

                    transientIpSummary.put("status","Transient (%)");

                    transientIpSummary.put("value",new DecimalFormat("#.00").format((double)(transientIp*100)/subnetIpDetailsList.size()));

                    ipSummaryObject.add(availableIpSummary);

                    ipSummaryObject.add(usedIpSummary);

                    ipSummaryObject.add(transientIpSummary);

                    HashMap<String, Object> results = new HashMap<>();

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    results.put("logFor", "IP_REPORT");

                    results.put("ipSummary", ipSummaryObject);

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "All IP Report ");

                    fileName = "All IP Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportAllIpReportCsv(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE())and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                try
                {
                    fileName = "ALL_IP_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR + fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "IP Address","Status","Scope","Mac Address","Vendor","IP To DNS","DNS To IP","Rogue","Last Alive Time"});

                    for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails:subnetIpDetailsList)
                    {
                        if(traceOrgSubnetIpDetails.isRogueStatus())
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"Yes",traceOrgSubnetIpDetails.getLastAliveTime()});
                        else
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"No",traceOrgSubnetIpDetails.getLastAliveTime()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportAllUsedIpReportPdf(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and Date(modifiedDate) = CURDATE() and subnetId in ("+subnetId+")   order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  DATE(modifiedDate) = DATE(CURDATE() -1) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and subnetId in ("+subnetId+")   order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  YEAR(modifiedDate) = YEAR(curdate()) and subnetId in ("+subnetId+")   order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                gridReport.put("Title", "Used Ip Report");

                try
                {

                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("IP Address");

                        add("Status");

                        add("Scope");

                        add("Mac Address");

                        add("Vendor");

                        add("IP To DNS");

                        add("DNS To IP");

                        add("Rogue");

                        add("Last Alive Time");

                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult;

                    String subnetAddress = null;


                    for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                        pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                        pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                        pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                        pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                        if(traceOrgSubnetIpDetail.isRogueStatus())
                            pdfResult.add("Yes");
                        else
                            pdfResult.add("No");

                        pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                        pdfResults.add(pdfResult);
                    }
                    HashMap<String, Object> results = new HashMap<>();

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    results.put("logFor", "IP_REPORT");

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "Used IP Report ");

                    fileName = "Used IP Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportAllUsedIpReportCsv(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and Date(modifiedDate) = CURDATE() and subnetId in ("+subnetId+")   order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  DATE(modifiedDate) = DATE(CURDATE() -1) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  deactiveStatus = false and  QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+")   order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+")   order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and subnetId in ("+subnetId+") and  deactiveStatus = false  order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.USED+"' and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                try
                {
                    fileName = "USED_IP"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "IP Address","Status","Scope","Mac Address","Vendor","IP To DNS","DNS To IP","Rogue","Last Alive Time"});

                    for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails:subnetIpDetailsList)
                    {
                        if(traceOrgSubnetIpDetails.isRogueStatus())
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"Yes",traceOrgSubnetIpDetails.getLastAliveTime()});
                        else
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"No",traceOrgSubnetIpDetails.getLastAliveTime()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportSubnetUtilizationReportPdf(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgSubnetDetails> subnetDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where Date(modifiedDate) = CURDATE() ");
                    break;
                case 2 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1)");
                    break;
                case 3 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 4 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 5 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 6 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 7 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 ");
                    break;
                case 8 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   YEAR(modifiedDate) = YEAR(curdate()) ");
                    break;
                case 9 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   YEAR(modifiedDate) = YEAR(curdate()) - 1");
                    break;
                case 10 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS);
                    break;
                case 11 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 12 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                default:
                    subnetDetailsList = null;
                    break;
            }

            if(subnetDetailsList !=null && !subnetDetailsList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                gridReport.put("Title", "Subnet Utilization Report");
                try
                {

                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("Subnet Address");

                        add("Subnet Type");

                        add("Subnet Name");

                        add("All IP");

                        add("Used IP");

                        add("Avaliable IP");

                        add("Transient IP");

                        add("% In Space  Used");

                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult;

                    String subnetAddress = null;

                    for (TraceOrgSubnetDetails traceOrgSubnetDetail : subnetDetailsList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgSubnetDetail.getSubnetAddress());

                        pdfResult.add(traceOrgSubnetDetail.getType());

                        pdfResult.add(traceOrgSubnetDetail.getSubnetName());

                        pdfResult.add(traceOrgSubnetDetail.getTotalIp());

                        pdfResult.add(traceOrgSubnetDetail.getUsedIp());

                        pdfResult.add(traceOrgSubnetDetail.getAvailableIp());

                        pdfResult.add(traceOrgSubnetDetail.getTransientIp());

                        pdfResult.add(traceOrgSubnetDetail.getUsedIpPercentage());

                        pdfResults.add(pdfResult);
                    }
                    HashMap<String, Object> results = new HashMap<>();

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "Subnet Utilization Report ");

                    fileName = "Subnet Utilization Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportSubnetUtilizationReportCsv(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgSubnetDetails> subnetDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where Date(modifiedDate) = CURDATE() ");
                    break;
                case 2 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1)");
                    break;
                case 3 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 4 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 5 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 6 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 7 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 ");
                    break;
                case 8 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   YEAR(modifiedDate) = YEAR(curdate()) ");
                    break;
                case 9 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where   YEAR(modifiedDate) = YEAR(curdate()) - 1");
                    break;
                case 10 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS);
                    break;
                case 11 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 12 :
                    subnetDetailsList = (List<TraceOrgSubnetDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS + " where  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                default:
                    subnetDetailsList = null;
                    break;
            }

            if(subnetDetailsList !=null && !subnetDetailsList.isEmpty())
            {
                try
                {
                    fileName = "SUBNET_UTLIZATION_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "Subnet Address","Subnet Type","Subnet Name","All IP","Used IP","Available IP","Transient IP","% In Space  Used"});

                    for(TraceOrgSubnetDetails traceOrgSubnetDetails:subnetDetailsList)
                    {
                        data.add(new String[] { traceOrgSubnetDetails.getSubnetAddress(),traceOrgSubnetDetails.getType(),traceOrgSubnetDetails.getSubnetName(),traceOrgSubnetDetails.getTotalIp().toString(),traceOrgSubnetDetails.getUsedIp().toString(),traceOrgSubnetDetails.getAvailableIp().toString(),traceOrgSubnetDetails.getTransientIp().toString(),""+traceOrgSubnetDetails.getUsedIpPercentage()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportDHCPUtilizationReportPdf(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgDhcpCredentialDetails> dhcpCredentialDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where Date(modifiedDate) = CURDATE() ");
                    break;
                case 2 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where DATE(modifiedDate) = DATE(CURDATE() -1)");
                    break;
                case 3 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 4 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 5 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 6 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 7 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 ");
                    break;
                case 8 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   YEAR(modifiedDate) = YEAR(curdate()) ");
                    break;
                case 9 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   YEAR(modifiedDate) = YEAR(curdate()) - 1");
                    break;
                case 10 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL);
                    break;
                case 11 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 12 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                default:
                    dhcpCredentialDetailsList = null;
                    break;
            }

            if(dhcpCredentialDetailsList !=null && !dhcpCredentialDetailsList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                gridReport.put("Title", "DHCP Server Utilization Report");
                try
                {
                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("Host Address");

                        add("Server Type");

                        add("Total Scopes");

                        add("Declines");

                        add("Request");

                        add("Releases");

                        add("Naks");

                        add("Offers");

                        add("Discovers");

                        add("Ack");

                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult;

                    String subnetAddress = null;

                    for (TraceOrgDhcpCredentialDetails traceOrgDhcpCredentialDetails : dhcpCredentialDetailsList)
                    {
                        TraceOrgDhcpUtilization traceOrgDhcpUtilization = (TraceOrgDhcpUtilization)traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_DHCP_UTILIZATION,traceOrgDhcpCredentialDetails.getId());

                        if(traceOrgDhcpUtilization !=null)
                        {
                            pdfResult = new ArrayList<>();

                            pdfResult.add(traceOrgDhcpCredentialDetails.getHostAddress());

                            pdfResult.add(traceOrgDhcpCredentialDetails.getType());

                            pdfResult.add(traceOrgDhcpUtilization.getAddressScopes());

                            pdfResult.add(traceOrgDhcpUtilization.getDeclines());

                            pdfResult.add(traceOrgDhcpUtilization.getRequests());

                            pdfResult.add(traceOrgDhcpUtilization.getReleases());

                            pdfResult.add(traceOrgDhcpUtilization.getNaks());

                            pdfResult.add(traceOrgDhcpUtilization.getOffers());

                            pdfResult.add(traceOrgDhcpUtilization.getDiscovers());

                            pdfResult.add(traceOrgDhcpUtilization.getAcks());

                            pdfResults.add(pdfResult);
                        }
                    }
                    HashMap<String, Object> results = new HashMap<>();

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "DHCP Server Utilization Report ");

                    fileName = "DHCP Server Utilization Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportDHCPUtilizationReportCsv(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgDhcpCredentialDetails> dhcpCredentialDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where Date(modifiedDate) = CURDATE() ");
                    break;
                case 2 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where DATE(modifiedDate) = DATE(CURDATE() -1)");
                    break;
                case 3 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 4 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 5 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 6 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) ");
                    break;
                case 7 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 ");
                    break;
                case 8 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   YEAR(modifiedDate) = YEAR(curdate()) ");
                    break;
                case 9 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where   YEAR(modifiedDate) = YEAR(curdate()) - 1");
                    break;
                case 10 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL);
                    break;
                case 11 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                case 12 :
                    dhcpCredentialDetailsList = (List<TraceOrgDhcpCredentialDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL + " where  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE())");
                    break;
                default:
                    dhcpCredentialDetailsList = null;
                    break;
            }

            if(dhcpCredentialDetailsList !=null && !dhcpCredentialDetailsList.isEmpty())
            {
                try
                {
                    fileName = "DHCP_UTLIZATION_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "Host Address", "Server Type", "Total Scopes", "Declines", "Request", "Releases", "Naks", "Offers", "Discovers", "Ack"});

                    for(TraceOrgDhcpCredentialDetails traceOrgDhcpCredentialDetails:dhcpCredentialDetailsList)
                    {
                        TraceOrgDhcpUtilization traceOrgDhcpUtilization = (TraceOrgDhcpUtilization)traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_DHCP_UTILIZATION,traceOrgDhcpCredentialDetails.getId());

                        if(traceOrgDhcpUtilization !=null)
                        {
                            data.add(new String[] { traceOrgDhcpCredentialDetails.getHostAddress(), traceOrgDhcpCredentialDetails.getType(), traceOrgDhcpUtilization.getAddressScopes(), traceOrgDhcpUtilization.getDeclines(), traceOrgDhcpUtilization.getRequests(), traceOrgDhcpUtilization.getReleases(), traceOrgDhcpUtilization.getNaks(), traceOrgDhcpUtilization.getOffers(), traceOrgDhcpUtilization.getDiscovers(), traceOrgDhcpUtilization.getAcks()});
                        }

                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportAllReservedIpReportPdf(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                gridReport.put("Title", "Reserved Ip Report");

                try
                {
                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("IP Address");

                        add("Status");

                        add("Scope");

                        add("Mac Address");

                        add("Vendor");

                        add("IP To DNS");

                        add("DNS To IP");

                        add("Rogue");

                        add("Last Alive Time");

                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult;

                    String subnetAddress = null;

                    for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                        pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                        pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                        pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                        pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                        if(traceOrgSubnetIpDetail.isRogueStatus())
                            pdfResult.add("Yes");
                        else
                            pdfResult.add("No");

                        pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                        pdfResults.add(pdfResult);
                    }
                    HashMap<String, Object> results = new HashMap<>();

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "Reserved IP Report ");

                    fileName = "Reserved IP Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportAllReservedIpReportCsv(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.RESERVED+"' and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                try
                {
                    fileName = "RESERVED_IP"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR + fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "IP Address","Status","Scope","Mac Address","Vendor","IP To DNS","DNS To IP","Rogue","Last Alive Time"});

                    for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails:subnetIpDetailsList)
                    {
                        if(traceOrgSubnetIpDetails.isRogueStatus())
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"Yes",traceOrgSubnetIpDetails.getLastAliveTime()});
                        else
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"No",traceOrgSubnetIpDetails.getLastAliveTime()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportAllAvailableIpReportPdf(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false  and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                gridReport.put("Title", "Available Ip Report");

                try
                {
                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("IP Address");

                        add("Status");

                        add("Scope");

                        add("Mac Address");

                        add("Vendor");

                        add("IP To DNS");

                        add("DNS To IP");

                        add("Rogue");

                        add("Last Alive Time");
                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult = new ArrayList<>();

                    String subnetAddress = null;

                    HashMap<String, Object> results = new HashMap<>();

                    for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                        pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                        pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                        pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                        pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                        if(traceOrgSubnetIpDetail.isRogueStatus())
                            pdfResult.add("Yes");
                        else
                            pdfResult.add("No");

                        pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                        pdfResults.add(pdfResult);
                    }

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "Available IP Report");

                    fileName = "Available IP Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportAllAvailableIpReportCsv(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and Date(modifiedDate) = CURDATE() and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  DATE(modifiedDate) = DATE(CURDATE() -1) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6  and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  YEAR(modifiedDate) = YEAR(curdate()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.AVAILABLE+"' and  deactiveStatus = false and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                try
                {
                    fileName =  "AVAILABLE_IP"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR + fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "IP Address","Status","Scope","Mac Address","Vendor","IP To DNS","DNS To IP","Rogue","Last Alive Time"});

                    for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails:subnetIpDetailsList)
                    {
                        if(traceOrgSubnetIpDetails.isRogueStatus())
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"Yes",traceOrgSubnetIpDetails.getLastAliveTime()});
                        else
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"No",traceOrgSubnetIpDetails.getLastAliveTime()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportVendorSummaryReportPdf(List< Map<String,Object>> vendorSummaryList,String ipStatus)
    {
        String fileName = null;

        HashMap<String, Object> gridReport = new HashMap<>();

        List<HashMap<String, Object>> ipSummaryObject = new ArrayList<>();

        try
        {
            if(vendorSummaryList !=null && !vendorSummaryList.isEmpty())
            {
                gridReport.put("Title","Vendor Summary Report" );

                LinkedHashSet<String> columns = new LinkedHashSet<String>()
                {{

                    add("Vendor Name");

                    add("Vendor Count");

                    add("Percentage");

                }};

                List<Object> pdfResults = new ArrayList<>();

                List<Object> pdfResult = new ArrayList<>();

                HashMap<String, Object> results = new HashMap<>();

                for (Map<String,Object> vendorDetails : vendorSummaryList)
                {
                    pdfResult = new ArrayList<>();

                    pdfResult.add(vendorDetails.get(TraceOrgCommonConstants.VENDOR_NAME));

                    pdfResult.add(vendorDetails.get(TraceOrgCommonConstants.VENDOR_COUNT));

                    pdfResult.add(vendorDetails.get(TraceOrgCommonConstants.VENDOR_PERCENTAGE));

                    pdfResults.add(pdfResult);
                }

                results.put("grid-result", pdfResults);

                results.put("columns", columns);

                results.put("logFor", "VENDOR_REPORT");

                List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                visualizationResults.add(results);

                fileName = ipStatus.toUpperCase()+" Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportIpReportPdf(List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetails,String ipStatus)
    {
        String fileName = null;

        HashMap<String, Object> gridReport = new HashMap<>();

        List<HashMap<String, Object>> ipSummaryObject = new ArrayList<>();

        Integer availableIp = 0;

        Integer usedIp = 0;

        Integer transientIp = 0;

        try
        {
            if(traceOrgSubnetIpDetails !=null && !traceOrgSubnetIpDetails.isEmpty())
            {
                gridReport.put("Title",ipStatus+" IP Report" );

                LinkedHashSet<String> columns = new LinkedHashSet<String>()
                {{

                    add("IP Address");

                    add("Status");

                    add("Scope");

                    add("Mac Address");

                    add("Vendor");

                    add("IP To DNS");

                    add("DNS To IP");

                    add("Rogue");

                    add("Last Alive Time");
                }};

                List<Object> pdfResults = new ArrayList<>();

                List<Object> pdfResult = new ArrayList<>();

                HashMap<String, Object> results = new HashMap<>();

                for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : traceOrgSubnetIpDetails)
                {

                    if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.AVAILABLE))
                    {
                        availableIp++;
                    }
                    else if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.USED))
                    {
                        usedIp++;
                    }
                    else if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.TRANSIENT))
                    {
                        transientIp++;
                    }

                    pdfResult = new ArrayList<>();

                    pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                    pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                    pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                    pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                    pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                    pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                    pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                    if(traceOrgSubnetIpDetail.isRogueStatus())
                        pdfResult.add("Yes");
                    else
                        pdfResult.add("No");

                    pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                    pdfResults.add(pdfResult);
                }

                if(ipStatus.equalsIgnoreCase("All"))
                {
                    HashMap<String, Object> availableIpSummary = new HashMap<>();

                    availableIpSummary.put("status","Available (%)");

                    availableIpSummary.put("value",new DecimalFormat("#.00").format((double)(availableIp*100)/traceOrgSubnetIpDetails.size()));

                    HashMap<String, Object> usedIpSummary = new HashMap<>();

                    usedIpSummary.put("status","Used (%)");

                    usedIpSummary.put("value",new DecimalFormat("#.00").format((double)(usedIp*100)/traceOrgSubnetIpDetails.size()));

                    HashMap<String, Object> transientIpSummary = new HashMap<>();

                    transientIpSummary.put("status","Transient (%)");

                    transientIpSummary.put("value",new DecimalFormat("#.00").format((double)(transientIp*100)/traceOrgSubnetIpDetails.size()));

                    ipSummaryObject.add(availableIpSummary);

                    ipSummaryObject.add(usedIpSummary);

                    ipSummaryObject.add(transientIpSummary);

                    results.put("ipSummary", ipSummaryObject);
                }

                results.put("grid-result", pdfResults);

                results.put("columns", columns);

                results.put("logFor", "IP_REPORT");

                List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                visualizationResults.add(results);

                fileName = ipStatus+" IP Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportVendorSummaryReportCsv(List<Map<String,Object>> vendorSummaryList,String ipStatus)
    {
        String fileName = null;

        if(vendorSummaryList !=null && !vendorSummaryList.isEmpty())
        {
            try
            {
                fileName =  ipStatus.toUpperCase()+ "_" +TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                CsvWriter csvWriter = new CsvWriter();

                Collection<String[]> data = new ArrayList<>();

                data.add(new String[] { "Vendor Name","Vendor Count","Percentage"});

                for(Map<String ,Object> vendorDetail:vendorSummaryList)
                {
                    data.add(new String[] { (String)vendorDetail.get(TraceOrgCommonConstants.VENDOR_NAME),vendorDetail.get(TraceOrgCommonConstants.VENDOR_COUNT).toString(),vendorDetail.get(TraceOrgCommonConstants.VENDOR_PERCENTAGE).toString()});
                }

                csvWriter.write(file, StandardCharsets.UTF_8, data);
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }

        }
        return fileName;
    }

    public String exportIpReportCsv(List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetails,String ipStatus)
    {
        String fileName = null;

        if(traceOrgSubnetIpDetails !=null && !traceOrgSubnetIpDetails.isEmpty())
        {
            try
            {
                fileName =  ipStatus.toUpperCase()+ "_" +TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                CsvWriter csvWriter = new CsvWriter();

                Collection<String[]> data = new ArrayList<>();

                data.add(new String[] { "IP Address","Status","Scope","Mac Address","Vendor","IP To DNS","DNS To IP","Rogue","Last Alive Time"});

                for(TraceOrgSubnetIpDetails subnetIpDetails:traceOrgSubnetIpDetails)
                {
                    if(subnetIpDetails.isRogueStatus())
                        data.add(new String[] { subnetIpDetails.getIpAddress(),subnetIpDetails.getStatus(),subnetIpDetails.getSubnetId().getSubnetName(),subnetIpDetails.getMacAddress(),subnetIpDetails.getDeviceType(),subnetIpDetails.getIpToDns(),subnetIpDetails.getDnsToIp(),"Yes",subnetIpDetails.getLastAliveTime()});
                    else
                        data.add(new String[] { subnetIpDetails.getIpAddress(),subnetIpDetails.getStatus(),subnetIpDetails.getSubnetId().getSubnetName(),subnetIpDetails.getMacAddress(),subnetIpDetails.getDeviceType(),subnetIpDetails.getIpToDns(),subnetIpDetails.getDnsToIp(),"No",subnetIpDetails.getLastAliveTime()});
                }
                csvWriter.write(file, StandardCharsets.UTF_8, data);
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }

        }
        return fileName;
    }

    public String exportAllTransientIpReportPdf(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            HashMap<String, Object> gridReport = new HashMap<>();

            gridReport.put("Title", "Transient Ip Report");
            try
            {
                LinkedHashSet<String> columns = new LinkedHashSet<String>()
                {{
                    add("IP Address");

                    add("Status");

                    add("Scope");

                    add("Mac Address");

                    add("Vendor");

                    add("IP To DNS");

                    add("DNS To IP");

                    add("Rogue");

                    add("Last Alive Time");
                }};

                List<Object> pdfResults = new ArrayList<>();

                List<Object> pdfResult = new ArrayList<>();

                String subnetAddress = null;

                HashMap<String, Object> results = new HashMap<>();

                if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
                {
                    for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                        pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                        pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                        pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                        pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                        if(traceOrgSubnetIpDetail.isRogueStatus())
                            pdfResult.add("Yes");
                        else
                            pdfResult.add("No");

                        pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                        pdfResults.add(pdfResult);
                    }

                }
                else
                {
                    pdfResult.add(" ");

                    pdfResults.add(pdfResult);
                }

                results.put("grid-result", pdfResults);

                results.put("columns", columns);

                results.put("logFor", "IP_REPORT");

                List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                visualizationResults.add(results);

                gridReport.put("Title", "Transient IP Report ");

                fileName = "Transient IP Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return fileName;
    }

    public String exportAllTransientIpReportCsv(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = '"+TraceOrgCommonConstants.TRANSIENT+"' and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                try
                {
                    fileName = "TRANSIENT_IP_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "IP Address","Status","Scope","Mac Address","Vendor","IP To DNS","DNS To IP","Rogue","Last Alive Time"});

                    for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails:subnetIpDetailsList)
                    {
                        if(traceOrgSubnetIpDetails.isRogueStatus())
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"Yes",traceOrgSubnetIpDetails.getLastAliveTime()});
                        else
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"No",traceOrgSubnetIpDetails.getLastAliveTime()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportAllRogueIpReportPdf(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6  and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  YEAR(modifiedDate) = YEAR(curdate()) and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true  and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            HashMap<String, Object> gridReport = new HashMap<>();

            gridReport.put("Title", "Rogue Ip Report");
            try
            {

                LinkedHashSet<String> columns = new LinkedHashSet<String>()
                {{
                    add("IP Address");

                    add("Status");

                    add("Scope");

                    add("Mac Address");

                    add("Vendor");

                    add("IP To DNS");

                    add("DNS To IP");

                    add("Rogue");

                    add("Last Alive Time");

                }};

                List<Object> pdfResults = new ArrayList<>();

                List<Object> pdfResult;

                String subnetAddress = null;

                if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
                {
                    for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                        pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                        pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                        pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                        pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                        pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                        if(traceOrgSubnetIpDetail.isRogueStatus())
                            pdfResult.add("Yes");
                        else
                            pdfResult.add("No");

                        pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                        pdfResults.add(pdfResult);
                    }
                }
                HashMap<String, Object> results = new HashMap<>();

                results.put("grid-result", pdfResults);

                results.put("columns", columns);

                results.put("logFor", "IP_REPORT");

                List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                visualizationResults.add(results);

                gridReport.put("Title", "ROGUE IP Report ");

                fileName = "ROGUE IP Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportAllRogueIpReportCsv(Integer exportTimeline,String subnetId)
    {
        String fileName = null;

        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 2 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 3 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 4 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 5 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 6 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 7 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 8 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  YEAR(modifiedDate) = YEAR(curdate()) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 9 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 10 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
                    break;
                case 11 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  WEEK(modifiedDate) =  WEEK(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                case 12 :
                    subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and  MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                    break;
                default:
                    subnetIpDetailsList = null;
                    break;
            }

            if(subnetIpDetailsList !=null && !subnetIpDetailsList.isEmpty())
            {
                try
                {
                    fileName = "ROGUE_IP_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "IP Address","Status","Scope","Mac Address","Vendor","IP To DNS","DNS To IP","Rogue","Last Alive Time"});

                    for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails:subnetIpDetailsList)
                    {
                        if(traceOrgSubnetIpDetails.isRogueStatus())
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"Yes",traceOrgSubnetIpDetails.getLastAliveTime()});
                        else
                            data.add(new String[] { traceOrgSubnetIpDetails.getIpAddress(),traceOrgSubnetIpDetails.getStatus(),traceOrgSubnetIpDetails.getSubnetId().getSubnetName(),traceOrgSubnetIpDetails.getMacAddress(),traceOrgSubnetIpDetails.getDeviceType(),traceOrgSubnetIpDetails.getIpToDns(),traceOrgSubnetIpDetails.getDnsToIp(),"No",traceOrgSubnetIpDetails.getLastAliveTime()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportAllEventReportPdf(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgEvent> traceOrgEventList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  Date(timestamp) = CURDATE() order by id desc");
                    break;
                case 2 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where DATE(timestamp) = DATE(CURDATE() -1) order by id desc");
                    break;
                case 3 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where WEEK(timestamp) =  WEEK(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 4 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where MONTH(timestamp)= MONTH(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 5 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  QUARTER(timestamp) = QUARTER(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 6 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where QUARTER(timestamp) = QUARTER(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 7 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where TIMESTAMPDIFF(MONTH, timestamp, NOW()) < 6  order by id desc");
                    break;
                case 8 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where YEAR(timestamp) = YEAR(curdate()) order by id desc ");
                    break;
                case 9 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where YEAR(timestamp) = YEAR(curdate()) - 1 order by id desc");
                    break;
                case 10 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT +" order by id desc" );
                    break;
                case 11 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  WEEK(timestamp) =  WEEK(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 12 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  MONTH(timestamp)= MONTH(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                default:
                    traceOrgEventList = null;
                    break;
            }

            if(traceOrgEventList !=null && !traceOrgEventList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                gridReport.put("Title", "Event Log Report");
                try
                {

                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("Event Type");

                        add("Event Context");

                        add("Time");

                        add("Username");

                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult;

                    for (TraceOrgEvent traceOrgEvent : traceOrgEventList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgEvent.getEventType());

                        pdfResult.add(traceOrgEvent.getEventContext());

                        pdfResult.add(traceOrgEvent.getTimestamp());

                        if (traceOrgEvent.getDoneBy() != null)
                        {
                            pdfResult.add(traceOrgEvent.getDoneBy().getUserName());
                        }
                        else
                        {
                            pdfResult.add(" ");
                        }

                        pdfResults.add(pdfResult);
                    }

                    HashMap<String, Object> results = new HashMap<>();

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    results.put("logFor","EVENT");

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "Event Log Report ");

                    fileName = "Event Log Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportAllEventReportCsv(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgEvent> traceOrgEventList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  Date(timestamp) = CURDATE() order by id desc");
                    break;
                case 2 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where DATE(timestamp) = DATE(CURDATE() -1) order by id desc");
                    break;
                case 3 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where WEEK(timestamp) =  WEEK(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 4 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where MONTH(timestamp)= MONTH(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 5 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  QUARTER(timestamp) = QUARTER(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 6 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where QUARTER(timestamp) = QUARTER(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 7 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where TIMESTAMPDIFF(MONTH, timestamp, NOW()) < 6  order by id desc");
                    break;
                case 8 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where YEAR(timestamp) = YEAR(curdate()) order by id desc ");
                    break;
                case 9 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where YEAR(timestamp) = YEAR(curdate()) - 1 order by id desc");
                    break;
                case 10 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT +" order by id desc" );
                    break;
                case 11 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  WEEK(timestamp) =  WEEK(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 12 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  MONTH(timestamp)= MONTH(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                default:
                    traceOrgEventList = null;
                    break;
            }

            if(traceOrgEventList !=null && !traceOrgEventList.isEmpty())
            {
                try
                {
                    fileName = "Event_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "Event Type","Event Context","Time","Username"});

                    for(TraceOrgEvent traceOrgEvent:traceOrgEventList)
                    {
                        if (traceOrgEvent.getDoneBy() != null)
                        {
                            data.add(new String[] { traceOrgEvent.getEventType(),traceOrgEvent.getEventContext(),traceOrgEvent.getTimestamp(),traceOrgEvent.getDoneBy().getUserName()});
                        }
                        else
                        {
                            data.add(new String[] { traceOrgEvent.getEventType(),traceOrgEvent.getEventContext(),traceOrgEvent.getTimestamp(),"-"});
                        }

                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public String exportAllConflictIpReportPdf(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgEvent> traceOrgEventList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and Date(timestamp) = CURDATE() order by id desc");
                    break;
                case 2 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and DATE(timestamp) = DATE(CURDATE() -1) order by id desc");
                    break;
                case 3 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and WEEK(timestamp) =  WEEK(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 4 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and MONTH(timestamp)= MONTH(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 5 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  eventType ='Conflict IP'  and QUARTER(timestamp) = QUARTER(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 6 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and QUARTER(timestamp) = QUARTER(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 7 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and TIMESTAMPDIFF(MONTH, timestamp, NOW()) < 6  order by id desc");
                    break;
                case 8 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and YEAR(timestamp) = YEAR(curdate()) order by id desc ");
                    break;
                case 9 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and YEAR(timestamp) = YEAR(curdate()) - 1 order by id desc");
                    break;
                case 10 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT +" where eventType ='Conflict IP' order by id desc" );
                    break;
                case 11 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and  WEEK(timestamp) =  WEEK(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 12 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and  MONTH(timestamp)= MONTH(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                default:
                    traceOrgEventList = null;
                    break;
            }

            if(traceOrgEventList !=null && !traceOrgEventList.isEmpty())
            {
                HashMap<String, Object> gridReport = new HashMap<>();

                gridReport.put("Title", "Conflict Ip Report");
                try
                {

                    LinkedHashSet<String> columns = new LinkedHashSet<String>()
                    {{
                        add("Event Type");

                        add("Event Context");

                        add("Time");

                    }};

                    List<Object> pdfResults = new ArrayList<>();

                    List<Object> pdfResult;

                    for (TraceOrgEvent traceOrgEvent : traceOrgEventList)
                    {
                        pdfResult = new ArrayList<>();

                        pdfResult.add(traceOrgEvent.getEventType());

                        pdfResult.add(traceOrgEvent.getEventContext());

                        pdfResult.add(traceOrgEvent.getTimestamp());

                        pdfResults.add(pdfResult);
                    }
                    HashMap<String, Object> results = new HashMap<>();

                    results.put("grid-result", pdfResults);

                    results.put("columns", columns);

                    List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                    visualizationResults.add(results);

                    gridReport.put("Title", "Conflict Ip Report ");

                    fileName = "Conflict Ip Report " + TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date()) + ".pdf";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }

    public String exportAllConflictIpReportCsv(Integer exportTimeline)
    {
        String fileName = null;

        List<TraceOrgEvent> traceOrgEventList = null;

        try
        {
            switch (exportTimeline)
            {
                case 1 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and Date(timestamp) = CURDATE() order by id desc");
                    break;
                case 2 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and DATE(timestamp) = DATE(CURDATE() -1) order by id desc");
                    break;
                case 3 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and WEEK(timestamp) =  WEEK(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 4 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and MONTH(timestamp)= MONTH(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 5 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where  eventType ='Conflict IP'  and QUARTER(timestamp) = QUARTER(curdate()) and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 6 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and QUARTER(timestamp) = QUARTER(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 7 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and TIMESTAMPDIFF(MONTH, timestamp, NOW()) < 6  order by id desc");
                    break;
                case 8 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and YEAR(timestamp) = YEAR(curdate()) order by id desc ");
                    break;
                case 9 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and YEAR(timestamp) = YEAR(curdate()) - 1 order by id desc");
                    break;
                case 10 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT +" where eventType ='Conflict IP' order by id desc" );
                    break;
                case 11 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and  WEEK(timestamp) =  WEEK(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                case 12 :
                    traceOrgEventList = (List<TraceOrgEvent>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_EVENT + " where eventType ='Conflict IP'  and  MONTH(timestamp)= MONTH(curdate()) - 1 and YEAR(timestamp) = YEAR(CURDATE()) order by id desc");
                    break;
                default:
                    traceOrgEventList = null;
                    break;
            }

            if(traceOrgEventList !=null && !traceOrgEventList.isEmpty())
            {
                try
                {
                    fileName = "Conflict_IP_"+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".csv";

                    fileName = fileName.replace(" ", "_").replace(":", "_").replace(",", "");

                    File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR +fileName);

                    CsvWriter csvWriter = new CsvWriter();

                    Collection<String[]> data = new ArrayList<>();

                    data.add(new String[] { "Event Type","Event Context","Time"});

                    for(TraceOrgEvent traceOrgEvent:traceOrgEventList)
                    {
                        data.add(new String[] { traceOrgEvent.getEventType(),traceOrgEvent.getEventContext(),traceOrgEvent.getTimestamp()});
                    }
                    csvWriter.write(file, StandardCharsets.UTF_8, data);
                }
                catch (Exception exception)
                {
                    _logger.error(exception);
                }

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return fileName;
    }


    public static boolean isValidIp(TraceOrgSubnetDetails traceOrgSubnetDetails,String ipAddress)
    {
        boolean result = Boolean.FALSE;

        try
        {
            SubnetUtils subnetUtils = new SubnetUtils(traceOrgSubnetDetails.getSubnetAddress()+"/"+traceOrgSubnetDetails.getSubnetCidr());

            subnetUtils.setInclusiveHostCount(true);

            SubnetUtils.SubnetInfo  subnetInfo = (subnetUtils).getInfo();

            result = subnetInfo.isInRange(ipAddress);
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }

    public void scheduleCustomJob(TraceOrgReportScheduler traceOrgReportScheduler,TraceOrgService traceOrgService,TraceOrgCommonUtil traceOrgCommonUtil)
    {
        Integer index = 0;

        try
        {
            if (traceOrgReportScheduler != null)
            {
                String date = TraceOrgCommonUtil.getStringValue(traceOrgReportScheduler.getSchedulerDate());

                String time = TraceOrgCommonUtil.getStringValue(traceOrgReportScheduler.getSchedulerTime());

                TraceOrgCronExpressionManager cronExpressionManager = new TraceOrgCronExpressionManager();

                DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                Date schedulerTime = simpleDateFormat.parse(traceOrgReportScheduler.getSchedulerDate()+" "+traceOrgReportScheduler.getSchedulerTime());

                if (date != null && time != null && schedulerTime.after(new Date()))
                {
                    // recurring cron expression using date and time

                    cronExpressionManager.setRecurring(true);

                    cronExpressionManager.setDate(date);

                    cronExpressionManager.setTime(time);

                }

                String cronExpressions = cronExpressionManager.getCronExpression();

                if (cronExpressions != null && cronExpressions.length() > 0)
                {
                    queueReportCustomJob(index, cronExpressions,traceOrgReportScheduler,traceOrgService,traceOrgCommonUtil);
                }

                short timeLine = TraceOrgCommonUtil.getShortValue(traceOrgReportScheduler.getSchedulerTimeLine());

                String jobTime = null;

                if (traceOrgReportScheduler.getRepeatHourTime() != null)
                {
                    jobTime = TraceOrgCommonUtil.getStringValue(traceOrgReportScheduler.getRepeatHourTime());
                }

                cronExpressionManager = new TraceOrgCronExpressionManager();

                if (timeLine == TraceOrgCommonConstants.SCHEDULER_TIMELINE_DAILY)
                {
                    cronExpressionManager.setDaily(true);

                    cronExpressionManager.setTime(jobTime);
                }
                else if (timeLine == TraceOrgCommonConstants.SCHEDULER_TIMELINE_WEEKLY)
                {
                    cronExpressionManager.setWeekDay(TraceOrgCommonUtil.getStringValue(traceOrgReportScheduler.getRepeatDay()));

                    cronExpressionManager.setWeekly(true);

                    cronExpressionManager.setTime(jobTime);

                }
                else if (timeLine == TraceOrgCommonConstants.SCHEDULER_TIMELINE_MONTHLY)
                {
                    cronExpressionManager.setMonth(TraceOrgCommonUtil.getStringValue(traceOrgReportScheduler.getRepeatMonth()));

                    cronExpressionManager.setDay(TraceOrgCommonUtil.getStringValue(traceOrgReportScheduler.getRepeatDate()));

                    cronExpressionManager.setMonthly(true);

                    cronExpressionManager.setTime(jobTime);

                }

                cronExpressions = cronExpressionManager.getCronExpression();

                if (cronExpressions != null && cronExpressions.length() > 0)
                {

                    for (String cronExpression : cronExpressions.split(TraceOrgCommonConstants.LINK_SEPARATOR))
                    {
                        index++;

                        queueReportCustomJob(index, cronExpression,traceOrgReportScheduler,traceOrgService,traceOrgCommonUtil);

                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }

    public void queueReportCustomJob(Integer index, String cronExpression, TraceOrgReportScheduler traceOrgReportScheduler,TraceOrgService traceOrgService,TraceOrgCommonUtil traceOrgCommonUtil)
    {
        if (org.quartz.CronExpression.isValidExpression(cronExpression))
        {
            try
            {
                HashMap<String,Object> mapData = new HashMap<>();

                mapData.put("traceOrgReportScheduler",traceOrgReportScheduler);

                mapData.put("traceOrgService",traceOrgService);

                mapData.put("traceOrgCommonUtil",traceOrgCommonUtil);

                JobKey jobKey = JobKey.jobKey(traceOrgReportScheduler.getId()+"ReportScheduler"+index,"ReportScheduler");

                JobDetail job = JobBuilder.newJob(TraceOrgReportSchedulerJob.class).withIdentity(jobKey).usingJobData(new JobDataMap(mapData)).storeDurably().build();

                Trigger trigger = TriggerBuilder
                        .newTrigger()
                        .withIdentity(traceOrgReportScheduler.getId()+"ReportScheduler"+index, "ReportScheduler")
                        .withSchedule(
                                CronScheduleBuilder.cronSchedule(cronExpression))
                        .build();

                quartzThread.scheduleJob(job,trigger);
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }
        }
        else
        {
            _logger.warn("Cron Expression is Not valid");
        }
    }

    public void scanSubnetCronJob(String cronExpression, TraceOrgSubnetDetails traceOrgSubnetDetails,TraceOrgService traceOrgService,TraceOrgCommonUtil traceOrgCommonUtil)
    {
        _logger.info("cronExpression ::"+cronExpression);

        if (org.quartz.CronExpression.isValidExpression(cronExpression))
        {
            try
            {
                _logger.info(traceOrgSubnetDetails.getSubnetAddress() +" subnet Scheduled for Every "+traceOrgSubnetDetails.getScheduleHour()+" "+traceOrgSubnetDetails.getDuration());

                HashMap<String,Object> mapData = new HashMap<>();

                mapData.put("subnetDetails",traceOrgSubnetDetails);

                mapData.put("traceOrgService",traceOrgService);

                mapData.put("traceOrgCommonUtil",traceOrgCommonUtil);

                JobKey jobKey = JobKey.jobKey(TraceOrgCommonConstants.SCAN_SUBNET+traceOrgSubnetDetails.getSubnetAddress(),TraceOrgCommonConstants.SCAN_SUBNET);

                JobDetail job = JobBuilder.newJob(TraceOrgScanSubnetUpdateQueue.class).withIdentity(jobKey).usingJobData(new JobDataMap(mapData)).storeDurably().build();

                Trigger trigger = TriggerBuilder
                        .newTrigger()
                        .withIdentity(TraceOrgCommonConstants.SCAN_SUBNET+traceOrgSubnetDetails.getSubnetAddress(),TraceOrgCommonConstants.SCAN_SUBNET)
                        .withSchedule(
                                CronScheduleBuilder.cronSchedule(cronExpression))
                        .build();

                quartzThread.scheduleJob(job,trigger);
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }
        }
        else
        {
            _logger.warn("Cron Expression is Not valid");
        }
    }

    public void removeScanSubnetCron(TraceOrgSubnetDetails traceOrgSubnetDetails)
    {
        try
        {
            for (JobKey jobKey : quartzThread.getJobKeys(GroupMatcher.jobGroupEquals(TraceOrgCommonConstants.SCAN_SUBNET)))
            {
                if (jobKey.getName().trim().startsWith(TraceOrgCommonConstants.SCAN_SUBNET+traceOrgSubnetDetails.getSubnetAddress()))
                {
                    quartzThread.deleteJob(jobKey);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }


    public void scanDhcpCronJob(String cronExpression, TraceOrgDhcpCredentialDetails traceOrgDhcpCredentialDetails,TraceOrgService traceOrgService,TraceOrgCommonUtil traceOrgCommonUtil,TraceOrgCiscoDHCPServerUtil traceOrgCiscoDHCPServerUtil,TraceOrgWindowsDhcpServerUtil traceOrgWindowsDhcpServerUtil)
    {
        if (org.quartz.CronExpression.isValidExpression(cronExpression))
        {
            try
            {

                _logger.info(traceOrgDhcpCredentialDetails.getHostAddress() +" Server Scheduled for Every "+traceOrgDhcpCredentialDetails.getScheduleHour()+" "+traceOrgDhcpCredentialDetails.getDuration());

                HashMap<String,Object> subnetDetails = new HashMap<>();

                subnetDetails.put("traceOrgDhcpCredentialDetails",traceOrgDhcpCredentialDetails);

                subnetDetails.put(TraceOrgCommonConstants.TRACE_ORG_SERVICE,this.traceOrgService);

                subnetDetails.put(TraceOrgCommonConstants.TRACE_ORG_COMMON_UTIL,traceOrgCommonUtil);

                subnetDetails.put("traceOrgCiscoDHCPServerUtil",traceOrgCiscoDHCPServerUtil);

                subnetDetails.put("traceOrgWindowsDhcpServerUtil",traceOrgWindowsDhcpServerUtil);

                subnetDetails.put(TraceOrgCommonConstants.SCAN_TYPE,TraceOrgCommonConstants.DHCP_SCAN);

                JobKey jobKey = JobKey.jobKey(TraceOrgCommonConstants.SCAN_DHCP+traceOrgDhcpCredentialDetails.getHostAddress(),TraceOrgCommonConstants.SCAN_DHCP);

                JobDetail job = JobBuilder.newJob(TraceOrgDhcpScanQueue.class).withIdentity(jobKey).usingJobData(new JobDataMap(subnetDetails)).storeDurably().build();

                Trigger trigger = TriggerBuilder
                        .newTrigger()
                        .withIdentity(TraceOrgCommonConstants.SCAN_DHCP+traceOrgDhcpCredentialDetails.getHostAddress(),TraceOrgCommonConstants.SCAN_DHCP)
                        .withSchedule(
                                CronScheduleBuilder.cronSchedule(cronExpression))
                        .build();

                quartzThread.scheduleJob(job,trigger);
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }
        }
        else
        {
            _logger.warn("Cron Expression is Not valid");
        }
    }

    public void removeScanDhcpCron(TraceOrgDhcpCredentialDetails traceOrgDhcpCredentialDetails)
    {
        try
        {
            for (JobKey jobKey : quartzThread.getJobKeys(GroupMatcher.jobGroupEquals(TraceOrgCommonConstants.SCAN_DHCP)))
            {
                if (jobKey.getName().trim().startsWith(TraceOrgCommonConstants.SCAN_DHCP+traceOrgDhcpCredentialDetails.getHostAddress()))
                {
                    quartzThread.deleteJob(jobKey);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }


    public void removeReportCustomJob(TraceOrgReportScheduler traceOrgReportScheduler)
    {
        try
        {
            for (JobKey jobKey : quartzThread.getJobKeys(GroupMatcher.jobGroupEquals("ReportScheduler")))
            {
                if (jobKey.getName().trim().startsWith(traceOrgReportScheduler.getId() + "ReportScheduler"))
                {
                    quartzThread.deleteJob(jobKey);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }

    static void scheduleSubnetScanCheckJob()
    {
        try
        {
            JobDetail scheduleSubnetScanJob = JobBuilder.newJob(TraceOrgSubnetScheduleScanJob.class).withIdentity("schedule-subnet-scan-job", "fixed-job").build();

            Trigger scheduleSubnetScanJobTrigger = TriggerBuilder.newTrigger().withIdentity("schedule-subnet-scan-job-trigger", "fixed-job").withSchedule(CronScheduleBuilder.cronSchedule(TraceOrgCommonUtil.SCHEDULE_SUBNET_SCAN_JOB_CRON_EXPRESSION)).build();


            TraceOrgCommonUtil.quartzThread.scheduleJob(scheduleSubnetScanJob, scheduleSubnetScanJobTrigger);

        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }

    public static int getBufferSize()
    {

        int bufferSize = 32;

        try
        {
            com.sun.management.OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            int memory = (int) operatingSystemMXBean.getTotalPhysicalMemorySize() / (1024 * 1024 * 1024);

            if (memory < 4)
            {
                bufferSize = 32;
            }
            else if (memory > 4 && memory < 8)
            {
                bufferSize = 64;
            }

            else if (memory > 8 && memory < 12)
            {
                bufferSize = 128;
            }

            else if (memory > 12 && memory < 16)
            {
                bufferSize = 256;
            }

            else if (memory > 16 && memory < 24)
            {
                bufferSize = 512;
            }

            else if (memory > 24)
            {
                bufferSize = 1024;
            }

        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return bufferSize;
    }

    public static Integer getCSVImportCount()
    {
        return m_csvImportStatus.intValue();
    }

    public static Integer getScanCount()
    {
        return m_ScanStatus.intValue();
    }

    public static void incrementCSVImportCount()
    {
        m_csvImportStatus.set(m_csvImportStatus.intValue() + 1);
    }

    public static void incrementScanStatusCount()
    {
        m_ScanStatus.set(m_ScanStatus.intValue() + 1);
    }

    public static void decrementCSVImportCount()
    {
        m_csvImportStatus.set(m_csvImportStatus.intValue() - 1);
    }

    public static void decrementScanStatusCount()
    {
        m_ScanStatus.set(m_ScanStatus.intValue() - 1);
    }

    public static Object convertToFormattedValue(Object value, DecimalFormat decimalFormat)
    {
        try
        {
            value = decimalFormat.format(value);
        }
        catch (Exception var3)
        {
            _logger.error(var3);
        }

        return value;
    }
}
