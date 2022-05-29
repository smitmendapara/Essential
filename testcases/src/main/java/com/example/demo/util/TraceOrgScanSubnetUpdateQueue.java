package com.example.demo.util;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetDetails;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.HashMap;

public class TraceOrgScanSubnetUpdateQueue implements Job {

    private final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgScanDhcpSchedulerJob.class, "GUI / Scan Subnet Update Queue ExecuteJob");

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        try
        {
            JobDataMap dataMap = jobExecutionContext.getMergedJobDataMap();

            TraceOrgCommonUtil traceOrgCommonUtil = (TraceOrgCommonUtil) dataMap.get(TraceOrgCommonConstants.TRACE_ORG_COMMON_UTIL);

            TraceOrgService traceOrgService= (TraceOrgService) dataMap.get(TraceOrgCommonConstants.TRACE_ORG_SERVICE);

            TraceOrgSubnetDetails traceOrgSubnetDetails = (TraceOrgSubnetDetails) dataMap.get("subnetDetails");

            HashMap<String,Object> mapData = new HashMap<>();

            mapData.put("subnetDetails",traceOrgSubnetDetails);

            mapData.put(TraceOrgCommonConstants.TRACE_ORG_SERVICE,traceOrgService);

            mapData.put(TraceOrgCommonConstants.TRACE_ORG_COMMON_UTIL,traceOrgCommonUtil);

            mapData.put("id",traceOrgSubnetDetails.getSubnetName());

            mapData.put(TraceOrgCommonConstants.SCAN_TYPE,TraceOrgCommonConstants.SUBNET_SCAN);

            TraceOrgCommonUtil.m_scheduleScanSubnet.put(traceOrgSubnetDetails.getSubnetAddress(),mapData);

        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }
}
