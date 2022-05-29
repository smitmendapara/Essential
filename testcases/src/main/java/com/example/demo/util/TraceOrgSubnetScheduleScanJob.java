package com.example.demo.util;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.restcontroller.TraceOrgSubnetControllerExecuteJob;
import org.quartz.*;

import java.util.HashMap;
import java.util.Map;

public class TraceOrgSubnetScheduleScanJob implements Job
{

    private final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgSubnetScheduleScanJob.class, "GUI / Subnet Scan Schedule Job");

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        try
        {
            if(TraceOrgCommonUtil.m_scheduleScanSubnet.size() > 0 && TraceOrgCommonUtil.getSubnetScanStatus() == 0)
            {
                Map.Entry<String,Object> scheduleEntry = TraceOrgCommonUtil.m_scheduleScanSubnet.entrySet().iterator().next();

                String subnetId = scheduleEntry.getKey();

                HashMap<String,Object> subnetDetails = (HashMap<String, Object>) TraceOrgCommonUtil.m_scheduleScanSubnet.get(subnetId);

                HashMap<String,Object> subnetScanDetails = new HashMap<>();

                short scanType = (short) subnetDetails.get(TraceOrgCommonConstants.SCAN_TYPE);

                if(subnetDetails.containsKey("accessToken") && subnetDetails.get("accessToken") != null)
                {
                    subnetScanDetails.put("accessToken",subnetDetails.get("accessToken"));
                }

                subnetScanDetails.put("traceOrgService",subnetDetails.get("traceOrgService"));

                subnetScanDetails.put("traceOrgCommonUtil",subnetDetails.get("traceOrgCommonUtil"));

                if(scanType == TraceOrgCommonConstants.SUBNET_SCAN)
                {
                    subnetScanDetails.put("subnetDetails",subnetDetails.get("subnetDetails"));

                    subnetScanDetails.put("id",subnetId);

                    JobKey jobKey = JobKey.jobKey(TraceOrgCommonConstants.SCAN_SUBNET);

                    JobDetail job = JobBuilder.newJob(TraceOrgSubnetControllerExecuteJob.class).withIdentity(jobKey).usingJobData(new JobDataMap(subnetScanDetails)).storeDurably().build();

                    TraceOrgCommonUtil.quartzThread.addJob(job, true);

                    TraceOrgCommonUtil.quartzThread.triggerJob(jobKey);
                }
                else
                {
                    subnetScanDetails.put("traceOrgDhcpCredentialDetails",subnetDetails.get("traceOrgDhcpCredentialDetails"));

                    subnetScanDetails.put("traceOrgCiscoDHCPServerUtil",subnetDetails.get("traceOrgCiscoDHCPServerUtil"));

                    subnetScanDetails.put("traceOrgWindowsDhcpServerUtil",subnetDetails.get("traceOrgWindowsDhcpServerUtil"));

                    JobKey jobKey = JobKey.jobKey(TraceOrgCommonConstants.SCAN_SUBNET);

                    JobDetail job = JobBuilder.newJob(TraceOrgScanDhcpSchedulerJob.class).withIdentity(jobKey).usingJobData(new JobDataMap(subnetScanDetails)).storeDurably().build();

                    TraceOrgCommonUtil.quartzThread.addJob(job, true);

                    TraceOrgCommonUtil.quartzThread.triggerJob(jobKey);
                }

                _logger.info("Remove from quque "+subnetId);

                TraceOrgCommonUtil.m_scheduleScanSubnet.remove(subnetId);

                _logger.info(TraceOrgCommonUtil.m_scheduleScanSubnet.size());

            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }
}
