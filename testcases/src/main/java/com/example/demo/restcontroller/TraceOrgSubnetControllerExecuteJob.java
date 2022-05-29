package com.example.demo.restcontroller;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.TraceOrgEvent;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetDetails;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetIpDetails;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.*;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Chaitas.
 */

@SuppressWarnings("ALL")
public class TraceOrgSubnetControllerExecuteJob implements Job
{
    private final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgSubnetControllerExecuteJob.class, "Subnet Execute Job Controller");

    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        try
        {
            TraceOrgSubnetUtil traceOrgSubnetUtil = new TraceOrgSubnetUtil();

            TraceOrgCiscoDHCPServerUtil traceOrgCiscoDHCPServerUtil = new TraceOrgCiscoDHCPServerUtil();

            TraceOrgWindowsDhcpServerUtil traceOrgWindowsDhcpServerUtil = new TraceOrgWindowsDhcpServerUtil();

            JobDataMap dataMap = context.getMergedJobDataMap();

            TraceOrgSubnetDetails traceOrgSubnetDetails = (TraceOrgSubnetDetails) dataMap.get("subnetDetails");

            TraceOrgCommonUtil traceOrgCommonUtil = (TraceOrgCommonUtil) dataMap.get(TraceOrgCommonConstants.TRACE_ORG_COMMON_UTIL);

            TraceOrgCommonUtil.m_scanStatus = new AtomicInteger(1);

            TraceOrgCommonUtil.m_scanSubnet.put("jobKey",traceOrgSubnetDetails.getSubnetName());

            _logger.info(traceOrgSubnetDetails.getSubnetAddress()+" Scanning");

            TraceOrgService traceOrgService = (TraceOrgService) dataMap.get(TraceOrgCommonConstants.TRACE_ORG_SERVICE);

            if (traceOrgSubnetDetails != null && traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,"id",traceOrgSubnetDetails.getId().toString()))
            {
                if(traceOrgSubnetDetails.getType().equalsIgnoreCase("Normal"))
                {
                    _logger.info("call started execute method for scheduler at time : " + new Date());

                    traceOrgSubnetUtil.getIPFromSubnet(traceOrgSubnetDetails,traceOrgService);
                }
                else if(traceOrgSubnetDetails.getType().equalsIgnoreCase(TraceOrgCommonConstants.CISCO))
                {
                    traceOrgCiscoDHCPServerUtil.getNetworkInterfaceForSpecificSubnet(traceOrgSubnetDetails,traceOrgService);
                }
                else if(traceOrgSubnetDetails.getType().equalsIgnoreCase(TraceOrgCommonConstants.WINDOWS))
                {
                    traceOrgWindowsDhcpServerUtil.getIpDetailsBySubnet(traceOrgSubnetDetails,traceOrgService);
                }

                TraceOrgSubnetDetails updatedTraceOrgSubnetDetails = (TraceOrgSubnetDetails) traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS, traceOrgSubnetDetails.getId()); // IPAM-124 bug 2

                List<TraceOrgSubnetIpDetails> totalSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+updatedTraceOrgSubnetDetails.getId()+"' and  deactiveStatus = false");

                List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE, TraceOrgCommonUtil.getStringValue(updatedTraceOrgSubnetDetails.getId()))+" and  deactiveStatus = false");

                List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(updatedTraceOrgSubnetDetails.getId())) +" and  deactiveStatus = false");

                List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(updatedTraceOrgSubnetDetails.getId())) +" and  deactiveStatus = false");

                updatedTraceOrgSubnetDetails.setAvailableIp((long) availableSubnetIpDetailsList.size());

                updatedTraceOrgSubnetDetails.setUsedIp((long) usedSubnetIpDetailsList.size());

                updatedTraceOrgSubnetDetails.setTransientIp((long) transientSubnetIpDetailsList.size());

                updatedTraceOrgSubnetDetails.setTotalIp((long)totalSubnetIpDetailsList.size());

                updatedTraceOrgSubnetDetails.setLastScanTime(new Date());

                updatedTraceOrgSubnetDetails.setModifiedDate(new Date());

                _logger.info("call ended execute method for scheduler at time : " + new Date());

                traceOrgService.insert(updatedTraceOrgSubnetDetails);

                if(traceOrgSubnetDetails.getUsedIpPercentage() > 80)
                {
                    TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                    traceOrgEvent.setTimestamp(new Date());

                    if(traceOrgService.findByUserName(traceOrgSubnetDetails.getCreatedBy())!=null)
                    {
                        traceOrgEvent.setDoneBy(traceOrgService.findByUserName(traceOrgSubnetDetails.getCreatedBy()));

                        traceOrgEvent.setEventContext("Subnet "+traceOrgSubnetDetails.getSubnetAddress()+" Utilization goes above 80 Percentage in IP Address Manager Scanned by "+traceOrgSubnetDetails.getCreatedBy());
                    }
                    else
                    {
                        traceOrgEvent.setEventContext("Subnet "+traceOrgSubnetDetails.getSubnetAddress()+" Utilization goes above 80 Percentage in IP Address Manager");
                    }

                    traceOrgEvent.setEventType("Subnet Utilization");

                    traceOrgEvent.setSeverity(0);

                    traceOrgService.insert(traceOrgEvent);
                }

                //EVENT LOG
                TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                traceOrgEvent.setTimestamp(new Date());

                if (traceOrgService.findByUserName(traceOrgSubnetDetails.getCreatedBy()) !=null )
                {
                    traceOrgEvent.setDoneBy(traceOrgService.findByUserName(traceOrgSubnetDetails.getCreatedBy()));

                    traceOrgEvent.setEventContext("Subnet "+traceOrgSubnetDetails.getSubnetAddress()+" is scanned in IP Address Manager by "+traceOrgSubnetDetails.getCreatedBy());
                }
                else
                {
                    traceOrgEvent.setEventContext("Subnet "+traceOrgSubnetDetails.getSubnetAddress()+" is scanned in IP Address Manager by Scheduler");
                }

                _logger.debug("Subnet "+traceOrgSubnetDetails.getSubnetAddress()+" is scanned");

                traceOrgEvent.setEventType("Scan Subnet");

                traceOrgEvent.setSeverity(2);

                traceOrgService.insert(traceOrgEvent);

                TraceOrgCommonUtil.m_scanSubnet.put("jobKey","");

                TraceOrgCommonUtil.m_scanStatus = new AtomicInteger(0);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

    }
}
