package com.example.demo.util;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.TraceOrgDhcpCredentialDetails;
import org.quartz.*;

import java.util.HashMap;

public class TraceOrgDhcpScanQueue implements Job {

    private final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgScanDhcpSchedulerJob.class, "DHCP Scan Queue");

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        try
        {
            JobDataMap dataMap = jobExecutionContext.getMergedJobDataMap();

            HashMap<String,Object> subnetDetails = new HashMap<>();

            TraceOrgCommonUtil traceOrgCommonUtil = (TraceOrgCommonUtil)dataMap.get(TraceOrgCommonConstants.TRACE_ORG_COMMON_UTIL);

            subnetDetails.put("traceOrgDhcpCredentialDetails",dataMap.get("traceOrgDhcpCredentialDetails"));

            subnetDetails.put(TraceOrgCommonConstants.TRACE_ORG_SERVICE,dataMap.get(TraceOrgCommonConstants.TRACE_ORG_SERVICE));

            subnetDetails.put(TraceOrgCommonConstants.TRACE_ORG_COMMON_UTIL,traceOrgCommonUtil);

            subnetDetails.put("traceOrgCiscoDHCPServerUtil",dataMap.get("traceOrgCiscoDHCPServerUtil"));

            subnetDetails.put("traceOrgWindowsDhcpServerUtil",dataMap.get("traceOrgWindowsDhcpServerUtil"));

            subnetDetails.put(TraceOrgCommonConstants.SCAN_TYPE,TraceOrgCommonConstants.DHCP_SCAN);

            TraceOrgCommonUtil.m_scheduleScanSubnet.put(((TraceOrgDhcpCredentialDetails)dataMap.get("traceOrgDhcpCredentialDetails")).getHostAddress(),subnetDetails);

        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }
}
