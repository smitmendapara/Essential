package com.example.demo.util;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.TraceOrgEvent;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetDetails;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetIpDetails;
import com.motadata.traceorg.ipam.model.TraceOrgVendor;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.net.util.SubnetUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author Hardik.
 */

@SuppressWarnings("ALL")
public class TraceOrgSubnetUtil
{
    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgSubnetUtil.class, "GUI / Subnet Util");

    public boolean getIPFromSubnet(TraceOrgSubnetDetails traceOrgSubnetDetails,TraceOrgService traceOrgService)
    {
        boolean result = false;

        String subnetDetails = traceOrgSubnetDetails.getSubnetAddress() + "/"+ traceOrgSubnetDetails.getSubnetCidr();

        String[] allIpAddress;

        List<String> ipList = new ArrayList<>();

        try
        {
            SubnetUtils subnetUtils = new SubnetUtils(subnetDetails);

            subnetUtils.setInclusiveHostCount(true);

            String networkAddress= subnetUtils.getInfo().getNetworkAddress();

            if(traceOrgSubnetDetails.getSubnetAddress().trim().equals(networkAddress))
            {

                allIpAddress = subnetUtils.getInfo().getAllAddresses();


                ipList = Arrays.asList(allIpAddress);

                result = getMACDetails(traceOrgSubnetDetails,ipList,traceOrgService);
            }
            else
            {
                _logger.warn("INVALID NETWORK ADDRESS");
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }

    private boolean getMACDetails(TraceOrgSubnetDetails traceOrgSubnetDetails, List<String> ipList,TraceOrgService traceOrgService) throws InterruptedException {

        List<TraceOrgSubnetIpDetails> updateIpDetailList  = new ArrayList<>();

        boolean resultOfInsert = false;

        String line;

        HashMap<String,Object> metricDetails = new HashMap<>();

        List<String> usedIpList = new ArrayList<>();

        if (traceOrgSubnetDetails.isAllowIcmp())
        {
            try
            {
                _logger.debug(traceOrgSubnetDetails.getSubnetAddress() + " Ping Start Time ::"+new Date());

                List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+traceOrgSubnetDetails.getId()+"' and deactiveStatus = false ");

                if(traceOrgSubnetIpDetailsList !=null && !traceOrgSubnetIpDetailsList.isEmpty())
                {
                    try
                    {
                        for(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails : traceOrgSubnetIpDetailsList)
                        {
                            TraceOrgTaskExecutor.executeTask(new TraceOrgSubNetScanner(traceOrgSubnetIpDetails.getIpAddress()));
                        }

                        int count = 0;

                        while (TraceOrgTaskExecutor.getTaskExecutor().hasQueuedSubmissions())
                        {
                            Thread.sleep(1000);

                            count++;

                            _logger.debug("executed task for scanning");

                            if (SystemUtils.IS_OS_WINDOWS)
                            {
                                TraceOrgWindowsPingProcessKill.killPingProcess();
                            }
                            else if(SystemUtils.IS_OS_LINUX)
                            {
                                TraceOrgLinuxPingProcessKill.killPingProcess();
                            }
                        }

                        _logger.debug("total count : " + count);
                    }
                    catch (Exception e)
                    {
                        _logger.error(e);
                    }
                }

                _logger.debug(traceOrgSubnetDetails.getSubnetAddress() + " Ping End Time ::"+new Date());

                Runtime runtime = Runtime.getRuntime();

                    //todo arpit changes
                Process process = null;

                File currentFile = new File(TraceOrgCommonConstants.CURRENT_DIR);

                File parentFile = new File(currentFile.getParent());

                String parentFilePath = parentFile.getParent();

                String pluginLocation = parentFilePath + TraceOrgCommonConstants.PATH_SEPARATOR+"python-engine" + TraceOrgCommonConstants.PATH_SEPARATOR +
                        "com" + TraceOrgCommonConstants.PATH_SEPARATOR + "motadata" + TraceOrgCommonConstants.PATH_SEPARATOR + "traceorg" +
                        TraceOrgCommonConstants.PATH_SEPARATOR + "python" + TraceOrgCommonConstants.PATH_SEPARATOR + "remotesubnetdetails.py";

                _logger.debug("Remote subnet file lookup in " + parentFilePath);

                if (new File(pluginLocation).exists() == false)
                {
                    _logger.debug("Remote subnet details cannot be fetched");

                    return false;
                }

                List<String> arguments = new ArrayList<>();

                List<String> defaultArguments = new ArrayList<>();

                defaultArguments.add("python");

                defaultArguments.add(pluginLocation);

                defaultArguments.add(traceOrgSubnetDetails.getGatewayIp());

                defaultArguments.add(traceOrgSubnetDetails.getSnmpCommunity());

                defaultArguments.add(traceOrgSubnetDetails.getSubnetAddress());

                defaultArguments.add(String.valueOf(traceOrgSubnetDetails.getSubnetCidr()));

                ProcessBuilder processBuilder = null;

                _logger.debug("is local subnet : " + traceOrgSubnetDetails.isLocalSubnet());

                if (traceOrgSubnetDetails.isLocalSubnet() == false)
                {
                    if (SystemUtils.IS_OS_LINUX) // todo system compatible with windows os only - need linux packages
                    {
                        processBuilder = new ProcessBuilder(defaultArguments);

                        processBuilder.directory(new File(parentFilePath+TraceOrgCommonConstants.PATH_SEPARATOR+"python"));

                        process = processBuilder.start();
                    }
                    else if (SystemUtils.IS_OS_WINDOWS)
                    {
                        if( TraceOrgCommonConstants.OS_NAME.equals("Windows 95"))
                        {
                            arguments.add("command.com");
                        }
                        else
                        {
                            arguments.add("cmd.exe");
                        }

                        arguments.add("/C");

                        arguments.addAll(defaultArguments);

                        _logger.debug("Parameters passed for process execution : " + arguments);

                        processBuilder = new ProcessBuilder(arguments);

                        processBuilder.directory(new File(parentFilePath+TraceOrgCommonConstants.PATH_SEPARATOR+"python"));

                        process = processBuilder.start();
                    }

                    //process.waitFor();

                    String singleLine;

                    InputStream inputStream = process.getInputStream();

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder scriptOutput = new StringBuilder();

                    while ((singleLine = bufferedReader.readLine())!=null)
                    {
                        scriptOutput.append(singleLine);
                    }

                    _logger.debug("output received from remote subnet : " + scriptOutput);

                    HashMap<String, Object> response = new HashMap<>();

                    if (scriptOutput.length() > 0)
                    {
                        response = TraceOrgCommonUtil.deserialize(scriptOutput.toString().replace("\r", "\\r").replace("\n", "\\n"));
                    }

                    List<HashMap<String, String>> ipMacList = (List<HashMap<String, String>>) response.get("result");

                    if (Objects.nonNull(ipMacList) && ipMacList.size() > 0)
                    {
                        for (HashMap<String, String> singleRecord : ipMacList)
                        {
                            TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = new TraceOrgSubnetIpDetails();

                            String macAddress = singleRecord.values().iterator().next();

                            String ipAddress = singleRecord.keySet().iterator().next();

                            if (!metricDetails.containsValue(macAddress)) {
                                usedIpList.add(ipAddress);

                                metricDetails.put(ipAddress, macAddress);

                                traceOrgSubnetIpDetails.setIpAddress(ipAddress);

                                traceOrgSubnetIpDetails.setMacAddress(macAddress);

                                traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.USED);

                                traceOrgSubnetIpDetails.setLastAliveTime(new Date());

                                traceOrgSubnetIpDetails.setSubnetId(traceOrgSubnetDetails);

                                List<TraceOrgVendor> vendorDetails = (List<TraceOrgVendor>) traceOrgService.commonQuery("", TraceOrgCommonConstants.VENDOR_BY_MAC_ADDRESS.replace(TraceOrgCommonConstants.VENDOR_MAC_VALUE, traceOrgSubnetIpDetails.getMacAddress().substring(0, 8).replace(":", "")));

                                if (vendorDetails != null && !vendorDetails.isEmpty()) {
                                    traceOrgSubnetIpDetails.setDeviceType(vendorDetails.get(0).getVendorName());
                                }

                                updateIpDetailList.add(traceOrgSubnetIpDetails);
                            }
                        }
                    }
                    else
                    {
                        _logger.debug("No records found");
                    }
                }
                else
                {
                    process = runtime.exec(TraceOrgCommonConstants.ARP_QUERY);


                    BufferedReader bufferedInputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    while ((line = bufferedInputStream.readLine())!=null)
                    {
                        TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = new TraceOrgSubnetIpDetails();

                        if (!line.isEmpty())
                        {
                            String[] outputs = line.trim().replace("(", "").replace(")", "").split("\\n");

                            for (String token : outputs)
                            {
                                String[] result = token.trim().replace("-", ":").split("\\s+");

                                if (SystemUtils.IS_OS_WINDOWS)
                                {
                                    if (!token.trim().contains("Interface:") && !token.trim().contains("Internet Address"))
                                    {
                                        if (TraceOrgCommonUtil.isValidIp(traceOrgSubnetDetails, result[0]))
                                        {
                                            if (!result[1].trim().contains("incomplete") && !result[2].trim().equals("static"))
                                            {
                                                if (!metricDetails.containsValue(result[1]))
                                                {
                                                    usedIpList.add(result[0]);

                                                    metricDetails.put(result[0], result[1]);

                                                    traceOrgSubnetIpDetails.setIpAddress(result[0]);

                                                    traceOrgSubnetIpDetails.setMacAddress(result[1]);

                                                    traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.USED);

                                                    traceOrgSubnetIpDetails.setLastAliveTime(new Date());

                                                    traceOrgSubnetIpDetails.setSubnetId(traceOrgSubnetDetails);

                                                    List<TraceOrgVendor> vendorDetails = (List<TraceOrgVendor>) traceOrgService.commonQuery("", TraceOrgCommonConstants.VENDOR_BY_MAC_ADDRESS.replace(TraceOrgCommonConstants.VENDOR_MAC_VALUE, traceOrgSubnetIpDetails.getMacAddress().substring(0, 8).replace(":", "")));

                                                    if(vendorDetails != null && !vendorDetails.isEmpty())
                                                    {
                                                        traceOrgSubnetIpDetails.setDeviceType(vendorDetails.get(0).getVendorName());
                                                    }

                                                    if (traceOrgSubnetDetails.isAllowDns())
                                                    {
                                                        if(traceOrgSubnetDetails.getDnsAddress() != null && !traceOrgSubnetDetails.getDnsAddress().isEmpty())
                                                        {
                                                            String ipToDns = TraceOrgCommonUtil.resolveHost(TraceOrgCommonUtil.getStringValue(traceOrgSubnetIpDetails.getIpAddress()), traceOrgSubnetDetails.getDnsAddress());

                                                            if(ipToDns != null && !ipToDns.isEmpty() && !ipToDns.equals(traceOrgSubnetIpDetails.getIpAddress()))
                                                            {
                                                                traceOrgSubnetIpDetails.setIpToDns(ipToDns);

                                                                String dnsToIp = TraceOrgCommonUtil.resolveIp(ipToDns, traceOrgSubnetDetails.getDnsAddress());

                                                                if (dnsToIp != null && !dnsToIp.isEmpty() && dnsToIp.equals(traceOrgSubnetIpDetails.getIpAddress()))
                                                                {
                                                                    traceOrgSubnetIpDetails.setDnsToIp(dnsToIp);
                                                                }
                                                                else
                                                                {
                                                                    traceOrgSubnetIpDetails.setDnsStatus("Forward DNS Lookup Failed");
                                                                }
                                                            }
                                                            else
                                                            {
                                                                traceOrgSubnetIpDetails.setDnsStatus("Reverse DNS Lookup Failed");
                                                            }
                                                        }
                                                    }

                                                    updateIpDetailList.add(traceOrgSubnetIpDetails);
                                                }
                                            }
                                        }
                                    }
                                }
                                else if (SystemUtils.IS_OS_LINUX)
                                {
                                    if (!result[3].trim().contains("<incomplete>"))
                                    {
                                        if (!metricDetails.containsValue(result[3]))
                                        {
                                            usedIpList.add(result[1]);

                                            metricDetails.put(result[1], result[3]);

                                            traceOrgSubnetIpDetails.setIpAddress(result[1]);

                                            traceOrgSubnetIpDetails.setMacAddress(result[3]);

                                            traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.USED);

                                            traceOrgSubnetIpDetails.setLastAliveTime(new Date());

                                            traceOrgSubnetIpDetails.setSubnetId(traceOrgSubnetDetails);

                                            List<TraceOrgVendor> vendorDetails = (List<TraceOrgVendor>) traceOrgService.commonQuery("", TraceOrgCommonConstants.VENDOR_BY_MAC_ADDRESS.replace(TraceOrgCommonConstants.VENDOR_MAC_VALUE, traceOrgSubnetIpDetails.getMacAddress().substring(0, 8).replace(":", "")));

                                            if (vendorDetails != null && !vendorDetails.isEmpty())
                                            {
                                                traceOrgSubnetIpDetails.setDeviceType(vendorDetails.get(0).getVendorName());
                                            }

                                            if (traceOrgSubnetDetails.isAllowDns())
                                            {
                                                if (traceOrgSubnetDetails.getDnsAddress() != null && !traceOrgSubnetDetails.getDnsAddress().isEmpty())
                                                {
                                                    String ipToDns = TraceOrgCommonUtil.resolveHost(TraceOrgCommonUtil.getStringValue(traceOrgSubnetIpDetails.getIpAddress()), traceOrgSubnetDetails.getDnsAddress());

                                                    if(ipToDns != null && !ipToDns.isEmpty() && !ipToDns.equals(traceOrgSubnetIpDetails.getIpAddress()))
                                                    {
                                                        traceOrgSubnetIpDetails.setIpToDns(ipToDns);

                                                        String dnsToIp = TraceOrgCommonUtil.resolveIp(ipToDns, traceOrgSubnetDetails.getDnsAddress());

                                                        if (dnsToIp != null && !dnsToIp.isEmpty() && dnsToIp.equals(traceOrgSubnetIpDetails.getIpAddress()))
                                                        {
                                                            traceOrgSubnetIpDetails.setDnsToIp(dnsToIp);
                                                        }
                                                        else
                                                        {
                                                            traceOrgSubnetIpDetails.setDnsStatus("Forward DNS Lookup Failed");
                                                        }
                                                    }
                                                    else
                                                    {
                                                        traceOrgSubnetIpDetails.setDnsStatus("Reverse DNS Lookup Failed");
                                                    }
                                                }
                                            }
                                            updateIpDetailList.add(traceOrgSubnetIpDetails);

                                        }
                                    }
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
        }

        for(String ip : ipList)
        {
            if(!usedIpList.contains(ip))
            {
                TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = new TraceOrgSubnetIpDetails();

                traceOrgSubnetIpDetails.setIpAddress(ip);

                traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.AVAILABLE);

                traceOrgSubnetIpDetails.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                traceOrgSubnetIpDetails.setSubnetId(traceOrgSubnetDetails);

                updateIpDetailList.add(traceOrgSubnetIpDetails);

            }
        }

        insertSubnetIp(updateIpDetailList,traceOrgService);

        _logger.debug(traceOrgSubnetDetails.getSubnetAddress() + " Scan End Time :: "+new Date());

        return  resultOfInsert;
    }

    public boolean insertSubnetIp(List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailList,TraceOrgService traceOrgService) throws InterruptedException {
        List<TraceOrgSubnetIpDetails> updatedSubnetIpDetails = new ArrayList<>();

        _logger.debug("Update Start Time :"+new Date());

        traceOrgSubnetIpDetailList.forEach(traceOrgSubnetIpDetails ->
        {

            boolean result = false;

            try
            {
                List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>)traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,traceOrgSubnetIpDetails.getIpAddress()) + " and subnetId = '"+traceOrgSubnetIpDetails.getSubnetId().getId()+"'");

                if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                {
                    TraceOrgSubnetIpDetails traceOrgSubnetIpDetailsExisted =  traceOrgSubnetIpDetailsList.get(0);

                    traceOrgSubnetIpDetailsExisted.setMacAddress(traceOrgSubnetIpDetails.getMacAddress());

                    if(traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED) && traceOrgSubnetIpDetailsExisted.getMacAddress()!=null && traceOrgSubnetIpDetails.getMacAddress()!=null)
                    {
                        if(traceOrgSubnetIpDetailsExisted.getPreviousMacAddress()!=null && !traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().isEmpty())
                        {
                            if(!traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().equalsIgnoreCase(traceOrgSubnetIpDetailsExisted.getMacAddress()))
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

                    switch (traceOrgSubnetIpDetailsExisted.getStatus()+","+traceOrgSubnetIpDetails.getStatus())
                    {

                        case TraceOrgCommonConstants.USED+","+TraceOrgCommonConstants.AVAILABLE:

                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);

                            traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.USED);

                            traceOrgSubnetIpDetails.setModifiedDate(new Date());

                            break;

                        case TraceOrgCommonConstants.TRANSIENT+","+TraceOrgCommonConstants.AVAILABLE:

                            if(traceOrgSubnetIpDetailsExisted.getLastAliveTime()!=null)
                            {
                                if((new Date().getTime() - new Date(traceOrgSubnetIpDetailsExisted.getLastAliveTime()).getTime())/(1000*60*60*24)>=7)
                                {
                                    traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.AVAILABLE);
                                }
                                else
                                {
                                    traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);
                                }
                            }
                            else
                            {
                                traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);
                            }

                            traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                            traceOrgSubnetIpDetails.setModifiedDate(new Date());

                            break;

                        case TraceOrgCommonConstants.TRANSIENT+","+TraceOrgCommonConstants.USED:

                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                            traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                            traceOrgSubnetIpDetails.setModifiedDate(new Date());

                            break;
                        case TraceOrgCommonConstants.RESERVED+","+TraceOrgCommonConstants.USED:

                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                            traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                            traceOrgSubnetIpDetails.setModifiedDate(new Date());

                            break;
                        case TraceOrgCommonConstants.RESERVED+","+TraceOrgCommonConstants.AVAILABLE:

                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.RESERVED);

                            traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                            break;

                        default:

                            traceOrgSubnetIpDetailsExisted.setPreviousStatus(traceOrgSubnetIpDetailsExisted.getStatus());

                            traceOrgSubnetIpDetailsExisted.setStatus(traceOrgSubnetIpDetails.getStatus());

                            break;
                    }

                    updatedSubnetIpDetails.add(traceOrgSubnetIpDetailsExisted);

                    if(traceOrgSubnetIpDetailsExisted.getConflictMac() != null && traceOrgSubnetIpDetailsExisted.getMacAddress()!=null)
                    {
                        TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                        traceOrgEvent.setTimestamp(new Date());

                        traceOrgEvent.setEventType("Conflict IP");

                        traceOrgEvent.setEventContext("IP Address "+traceOrgSubnetIpDetailsExisted.getIpAddress()+" with  Mac Address "+traceOrgSubnetIpDetailsExisted.getMacAddress()+" conflicted with  Mac Address "+traceOrgSubnetIpDetailsExisted.getConflictMac()+" in IP Address Manager " );

                        traceOrgEvent.setSeverity(0);

                        traceOrgService.insert(traceOrgEvent);
                    }

                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);
            }
        });

        traceOrgService.updateAll(updatedSubnetIpDetails);

        while (TraceOrgCommonUtil.getScanCount() != 0)
        {
            Thread.sleep(1000);
        }
        return true;
    }

    private void checkProcessStatus(LinkedHashMap<Thread, Long> processes)
    {
        try
        {
            for (Thread thread : processes.keySet())
            {
                processes.put(thread, processes.get(thread) - 1000);

                if(processes.get(thread) <= 0)
                {
                    try
                    {
                        thread.interrupt();
                    }
                    catch (Exception ignored){}

                    processes.remove(thread);
                }
            }
        }
        catch (Exception e)
        {
            _logger.error(e);
        }
    }

    public boolean insertSubnetIp(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails,TraceOrgService traceOrgService)
    {
        boolean result = false;

        try
        {
            List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>)traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,traceOrgSubnetIpDetails.getIpAddress()) + " and subnetId = '"+traceOrgSubnetIpDetails.getSubnetId().getId()+"'");

            if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
            {
                TraceOrgSubnetIpDetails traceOrgSubnetIpDetailsExisted =  traceOrgSubnetIpDetailsList.get(0);

                traceOrgSubnetIpDetailsExisted.setMacAddress(traceOrgSubnetIpDetails.getMacAddress());

                if(traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED) && traceOrgSubnetIpDetailsExisted.getMacAddress()!=null && traceOrgSubnetIpDetails.getMacAddress()!=null)
                {
                    if(traceOrgSubnetIpDetailsExisted.getPreviousMacAddress()!=null && !traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().isEmpty())
                    {
                        if(!traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().equalsIgnoreCase(traceOrgSubnetIpDetailsExisted.getMacAddress()))
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

                switch (traceOrgSubnetIpDetailsExisted.getStatus()+","+traceOrgSubnetIpDetails.getStatus())
                {

                    case TraceOrgCommonConstants.USED+","+TraceOrgCommonConstants.AVAILABLE:

                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());

                        break;

                    case TraceOrgCommonConstants.TRANSIENT+","+TraceOrgCommonConstants.AVAILABLE:

                        if(traceOrgSubnetIpDetailsExisted.getLastAliveTime()!=null)
                        {
                            if((new Date().getTime() - new Date(traceOrgSubnetIpDetailsExisted.getLastAliveTime()).getTime())/(1000*60*60*24)>=7)
                            {
                                traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.AVAILABLE);
                            }
                            else
                            {
                                traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);
                            }
                        }
                        else
                        {
                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);
                        }

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());

                        break;

                    case TraceOrgCommonConstants.TRANSIENT+","+TraceOrgCommonConstants.USED:

                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());

                        break;
                    case TraceOrgCommonConstants.RESERVED+","+TraceOrgCommonConstants.USED:

                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());

                        break;
                    case TraceOrgCommonConstants.RESERVED+","+TraceOrgCommonConstants.AVAILABLE:

                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.RESERVED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        break;

                    default:

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(traceOrgSubnetIpDetailsExisted.getStatus());

                        traceOrgSubnetIpDetailsExisted.setStatus(traceOrgSubnetIpDetails.getStatus());

                        break;
                }

                boolean updateStatus = traceOrgService.insert(traceOrgSubnetIpDetailsExisted);

                if (updateStatus)
                {
                    result = true;

                    if(traceOrgSubnetIpDetailsExisted.getConflictMac() != null && traceOrgSubnetIpDetailsExisted.getMacAddress()!=null)
                    {
                        TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                        traceOrgEvent.setTimestamp(new Date());

                        traceOrgEvent.setEventType("Conflict IP");

                        traceOrgEvent.setEventContext("IP Address "+traceOrgSubnetIpDetailsExisted.getIpAddress()+" with  Mac Address "+traceOrgSubnetIpDetailsExisted.getMacAddress()+" conflicted with  Mac Address "+traceOrgSubnetIpDetailsExisted.getConflictMac()+" in IP Address Manager " );

                        traceOrgEvent.setSeverity(0);

                        traceOrgService.insert(traceOrgEvent);
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
}