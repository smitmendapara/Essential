package com.example.demo.util;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.TraceOrgDhcpCredentialDetails;
import com.motadata.traceorg.ipam.model.TraceOrgGlobalSetting;
import com.motadata.traceorg.ipam.model.TraceOrgReportScheduler;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetDetails;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.List;

@SuppressWarnings("ALL")
@Configuration
public class TraceOrgInitServlet implements ServletContextInitializer {

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgInitServlet.class, "Init Servlet");

    @Autowired
    TraceOrgService traceOrgService;

    @Autowired
    TraceOrgCommonUtil traceOrgCommonUtil;

    @Autowired
    TraceOrgCiscoDHCPServerUtil traceOrgCiscoDHCPServerUtil;

    @Autowired
    TraceOrgWindowsDhcpServerUtil traceOrgWindowsDhcpServerUtil;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException
    {
        try
        {
            System.setProperty("java.awt.headless", "true");

            TraceOrgCommonUtil.initializeQuartzThread();

            TraceOrgCommonUtil.scheduleSubnetScanCheckJob();

            TraceOrgCommonUtil.createTmpDirForReport();

            List<TraceOrgGlobalSetting> traceOrgGlobalSettings = (List<TraceOrgGlobalSetting>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_GLOBAL_SETTING);

            if(traceOrgGlobalSettings!=null && !traceOrgGlobalSettings.isEmpty())
            {
                TraceOrgGlobalSetting traceOrgGlobalSetting = traceOrgGlobalSettings.get(0);

                TraceOrgCommonUtil.setLogLevel(traceOrgGlobalSetting.getLoggingLevel());
            }

            List<TraceOrgReportScheduler> traceOrgReportSchedulers = (List<TraceOrgReportScheduler>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_REPORT_SCHEDULER);

            if(traceOrgReportSchedulers !=null && !traceOrgReportSchedulers.isEmpty())
            {
                for(TraceOrgReportScheduler traceOrgReportScheduler :traceOrgReportSchedulers)
                {
                    traceOrgCommonUtil.scheduleCustomJob(traceOrgReportScheduler,this.traceOrgService,traceOrgCommonUtil);
                }
            }
            List<TraceOrgSubnetDetails> traceOrgSubnetDetails = (List<TraceOrgSubnetDetails>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS);

            if(traceOrgSubnetDetails !=null && !traceOrgSubnetDetails.isEmpty())
            {
                for(TraceOrgSubnetDetails traceOrgSubnetDetail :traceOrgSubnetDetails)
                {
                    if(traceOrgSubnetDetail.isScheduleStatus() && traceOrgSubnetDetail.getDuration()!=null && traceOrgSubnetDetail.getScheduleHour()!=null && traceOrgSubnetDetail.getScheduleHour() > 0)
                    {
                        String cronExpression = null;

                        switch (traceOrgSubnetDetail.getDuration())
                        {
                            case "Days" :
                                cronExpression = "0 0 0 1-31/"+traceOrgSubnetDetail.getScheduleHour()+" * ?";
                                break;
                            case "Hours" :
                                cronExpression = "0 0 1-23/"+traceOrgSubnetDetail.getScheduleHour()+" ? * *";
                                break;
                            case "Month" :
                                cronExpression = "0 0 0 1 1-12/"+traceOrgSubnetDetail.getScheduleHour()+" ?";
                                break;
                        }

                        if(cronExpression!=null)
                        {
                            traceOrgCommonUtil.scanSubnetCronJob(cronExpression,traceOrgSubnetDetail,traceOrgService,traceOrgCommonUtil);
                        }
                    }
                }
            }

            List<TraceOrgDhcpCredentialDetails> traceOrgDhcpCredentialDetails = (List<TraceOrgDhcpCredentialDetails>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_DHCP_CREDENTIAL);

            if(traceOrgSubnetDetails !=null && !traceOrgSubnetDetails.isEmpty())
            {
                for(TraceOrgDhcpCredentialDetails traceOrgDhcpCredentialDetail :traceOrgDhcpCredentialDetails)
                {
                    if(traceOrgDhcpCredentialDetail.getDuration() !=null && traceOrgDhcpCredentialDetail.getScheduleHour()!=null && traceOrgDhcpCredentialDetail.getScheduleHour()>0)
                    {
                        String cronExpression = null;

                        switch (traceOrgDhcpCredentialDetail.getDuration())
                        {
                            case "Days" :
                                cronExpression = "0 0 0 1-31/"+traceOrgDhcpCredentialDetail.getScheduleHour()+" * ?";
                                break;
                            case "Hours" :
                                cronExpression = "0 0 1-23/"+traceOrgDhcpCredentialDetail.getScheduleHour()+" ? * *";
                                break;
                            case "Month" :
                                cronExpression = "0 0 0 1 1-12/"+traceOrgDhcpCredentialDetail.getScheduleHour()+" ?";
                                break;
                        }

                        traceOrgCommonUtil.scanDhcpCronJob(cronExpression,traceOrgDhcpCredentialDetail,this.traceOrgService,traceOrgCommonUtil,traceOrgCiscoDHCPServerUtil,traceOrgWindowsDhcpServerUtil);
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }

}
