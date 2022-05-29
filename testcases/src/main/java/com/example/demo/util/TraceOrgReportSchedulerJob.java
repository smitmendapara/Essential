package com.example.demo.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.TraceOrgMailServer;
import com.motadata.traceorg.ipam.model.TraceOrgReportScheduler;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import org.apache.commons.mail.HtmlEmail;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("ALL")
public class TraceOrgReportSchedulerJob implements Job
{
    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgReportSchedulerJob.class, "ReportScheduler ExecuteJob");

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        try
        {
            JobDataMap dataMap = context.getMergedJobDataMap();

            TraceOrgReportScheduler traceOrgReportScheduler = (TraceOrgReportScheduler) dataMap.get("traceOrgReportScheduler");

            TraceOrgService traceOrgService = (TraceOrgService) dataMap.get("traceOrgService");

            TraceOrgCommonUtil traceOrgCommonUtil = (TraceOrgCommonUtil) dataMap.get("traceOrgCommonUtil");

            _logger.info("Scheduling :: " + traceOrgReportScheduler.getSchedulerName());

            String fileName = null;

            String mailSubject = traceOrgReportScheduler.getSchedulerName();

            if(traceOrgReportScheduler.getExportType().equalsIgnoreCase("PDF"))
            {
                if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.ALL_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllIpReportPdf(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.USED_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllUsedIpReportPdf(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.RESERVED_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllReservedIpReportPdf(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.EVENT_LOG_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllEventReportPdf(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.AVAILABLE_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllAvailableIpReportPdf(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.TRANSIENT_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllTransientIpReportPdf(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.ROGUE_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllRogueIpReportPdf(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.CONFLICT_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllConflictIpReportPdf(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.SUBNET_UTILIZATION_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportSubnetUtilizationReportPdf(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.DHCP_UTILIZATION_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportDHCPUtilizationReportPdf(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.VENDOR_SUMMARY_REPORT))
                {
                    List<Map<String,Object>> vendorSummaryList = new ArrayList<>() ;

                    List<Object> vendorList = null;

                    switch (traceOrgReportScheduler.getReportExportTimeline())
                    {
                        case 1 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and Date(modifiedDate) = CURDATE() and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 2 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and DATE(modifiedDate) = DATE(CURDATE() -1) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 3 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 4 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 5 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 6 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 7 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6  group by deviceType order by devicenumber desc");
                            break;
                        case 8 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 group by deviceType order by devicenumber desc");
                            break;
                        case 9 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and YEAR(modifiedDate) = YEAR(curdate()) - 1 group by deviceType order by devicenumber desc");
                            break;
                        case 10 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null group by deviceType order by devicenumber desc");
                            break;
                        case 11 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 12 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+") and deviceType is not null and MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        default:
                            vendorList = null;
                            break;
                    }

                    if(vendorList != null && !vendorList.isEmpty())
                    {
                        Integer totalCount = 0;

                        for(Object vendorOutputs : vendorList)
                        {
                            Map<String,Object> vendorDetails = new HashMap<>();

                            Gson gson= new Gson();

                            Type listType = new TypeToken<List<String>>() {}.getType();

                            List<String> vendorOutputsList = gson.fromJson(gson.toJson(vendorOutputs), listType);

                            totalCount = totalCount + Integer.parseInt(vendorOutputsList.get(0));
                        }

                        for(Object vendorOutputs : vendorList)
                        {
                            Map<String,Object> vendorDetails = new HashMap<>();

                            Gson gson= new Gson();

                            Type listType = new TypeToken<List<String>>() {}.getType();

                            List<String> vendorOutputsList = gson.fromJson(gson.toJson(vendorOutputs), listType);

                            DecimalFormat decimalFormat = new DecimalFormat();

                            decimalFormat.setMaximumFractionDigits(2);

                            if(vendorOutputsList !=null && !vendorOutputsList.isEmpty() && vendorOutputsList.get(1) != null)
                            {
                                vendorDetails.put(TraceOrgCommonConstants.VENDOR_NAME,vendorOutputsList.get(1));

                                vendorDetails.put(TraceOrgCommonConstants.VENDOR_COUNT,Long.parseLong(vendorOutputsList.get(0)));

                                vendorDetails.put(TraceOrgCommonConstants.VENDOR_PERCENTAGE,decimalFormat.format((float)(Long.parseLong(vendorOutputsList.get(0))*100)/totalCount));
                            }
                            if(vendorDetails!=null && !vendorDetails.isEmpty())
                            {
                                vendorSummaryList.add(vendorDetails);
                            }
                        }
                    }
                    fileName = traceOrgCommonUtil.exportVendorSummaryReportPdf(vendorSummaryList,"Vendor Summary");

                }
            }
            else if(traceOrgReportScheduler.getExportType().equalsIgnoreCase("CSV"))
            {
                if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.ALL_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllIpReportCsv(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.USED_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllUsedIpReportCsv(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.RESERVED_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllReservedIpReportCsv(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.EVENT_LOG_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllEventReportCsv(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.AVAILABLE_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllAvailableIpReportCsv(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.TRANSIENT_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllTransientIpReportCsv(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.ROGUE_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllRogueIpReportCsv(traceOrgReportScheduler.getReportExportTimeline(),traceOrgReportScheduler.getSubnetId());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.CONFLICT_IP_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportAllConflictIpReportCsv(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.SUBNET_UTILIZATION_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportSubnetUtilizationReportCsv(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.DHCP_UTILIZATION_REPORT))
                {
                    fileName = traceOrgCommonUtil.exportDHCPUtilizationReportCsv(traceOrgReportScheduler.getReportExportTimeline());

                }
                else if(traceOrgReportScheduler.getIpFilter().equalsIgnoreCase(TraceOrgCommonConstants.VENDOR_SUMMARY_REPORT))
                {
                    List<Map<String,Object>> vendorSummaryList = new ArrayList<>() ;

                    List<Object> vendorList = null;

                    switch (traceOrgReportScheduler.getReportExportTimeline())
                    {
                        case 1 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and Date(modifiedDate) = CURDATE() and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 2 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and DATE(modifiedDate) = DATE(CURDATE() -1) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 3 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 4 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 5 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 6 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 7 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6  group by deviceType order by devicenumber desc");
                            break;
                        case 8 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 group by deviceType order by devicenumber desc");
                            break;
                        case 9 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and YEAR(modifiedDate) = YEAR(curdate()) - 1 group by deviceType order by devicenumber desc");
                            break;
                        case 10 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  group by deviceType order by devicenumber desc");
                            break;
                        case 11 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        case 12 :
                            vendorList = (List<Object>) traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",traceOrgReportScheduler.getSubnetId()),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+traceOrgReportScheduler.getSubnetId()+")  and MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                            break;
                        default:
                            vendorList = null;
                            break;
                    }

                    if(vendorList != null && !vendorList.isEmpty())
                    {
                        Integer totalCount = 0;

                        for(Object vendorOutputs : vendorList)
                        {
                            Map<String,Object> vendorDetails = new HashMap<>();

                            Gson gson= new Gson();

                            Type listType = new TypeToken<List<String>>() {}.getType();

                            List<String> vendorOutputsList = gson.fromJson(gson.toJson(vendorOutputs), listType);

                            totalCount = totalCount + Integer.parseInt(vendorOutputsList.get(0));
                        }

                        for(Object vendorOutputs : vendorList)
                        {
                            Map<String,Object> vendorDetails = new HashMap<>();

                            Gson gson= new Gson();

                            Type listType = new TypeToken<List<String>>() {}.getType();

                            List<String> vendorOutputsList = gson.fromJson(gson.toJson(vendorOutputs), listType);

                            DecimalFormat decimalFormat = new DecimalFormat();

                            decimalFormat.setMaximumFractionDigits(2);

                            if(vendorOutputsList !=null && !vendorOutputsList.isEmpty())
                            {
                                if(vendorOutputsList.get(1) == null)
                                {
                                    vendorDetails.put(TraceOrgCommonConstants.VENDOR_NAME,"Others");
                                }
                                else
                                {
                                    vendorDetails.put(TraceOrgCommonConstants.VENDOR_NAME,vendorOutputsList.get(1));
                                }
                                vendorDetails.put(TraceOrgCommonConstants.VENDOR_COUNT,Long.parseLong(vendorOutputsList.get(0)));

                                vendorDetails.put(TraceOrgCommonConstants.VENDOR_PERCENTAGE,decimalFormat.format((float)(Long.parseLong(vendorOutputsList.get(0))*100)/totalCount));
                            }
                            if(vendorDetails!=null && !vendorDetails.isEmpty())
                            {
                                vendorSummaryList.add(vendorDetails);
                            }
                        }
                    }
                    fileName = traceOrgCommonUtil.exportVendorSummaryReportCsv(vendorSummaryList,"Vendor Summary");
                }
            }

            if(fileName!=null && !fileName.isEmpty() && mailSubject!=null && !mailSubject.isEmpty())
            {
                File file = new File(TraceOrgCommonConstants.CURRENT_DIR +TraceOrgCommonConstants.PATH_SEPARATOR+"Report"+TraceOrgCommonConstants.PATH_SEPARATOR+fileName);

                try
                {
                    TraceOrgMailServer traceOrgMailServer =(TraceOrgMailServer) traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_MAIL_SERVER, 1L);

                    if(traceOrgMailServer != null)
                    {
                        if(traceOrgReportScheduler.getEmailTo().contains(","))
                        {
                            for(String emailTo : traceOrgReportScheduler.getEmailTo().split(","))
                            {
                                sendMailWithAttachment(traceOrgMailServer.getMailHost(),traceOrgMailServer.getMailPort(),mailSubject,"Hello "+traceOrgMailServer.getMailUserName()+",<br><br> <t>Please Find the Attachment for Report of IP Address Manager <br><br> Thank You ",traceOrgMailServer.getMailFromEmail(),emailTo,traceOrgMailServer.getMailProtocol(),traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailPassword(),traceOrgMailServer.getMailTimeout(),file);
                            }
                        }
                        else
                        {
                            sendMailWithAttachment(traceOrgMailServer.getMailHost(),traceOrgMailServer.getMailPort(),mailSubject,"Hello "+traceOrgMailServer.getMailUserName()+",<br><br> <t>Please Find the Attachment for Report of IP Address Manager <br><br> Thank You ",traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailToEmail(),traceOrgMailServer.getMailProtocol(),traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailPassword(),traceOrgMailServer.getMailTimeout(),file);
                        }

                    }
                }
                catch (Exception exception)
                {
                    try
                    {
                        TraceOrgMailServer traceOrgMailServer = (TraceOrgMailServer) traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_MAIL_SERVER, 2L);

                        if(traceOrgMailServer != null)
                        {
                            if(traceOrgReportScheduler.getEmailTo().contains(","))
                            {
                                for(String emailTo : traceOrgReportScheduler.getEmailTo().split(","))
                                {
                                    sendMailWithAttachment(traceOrgMailServer.getMailHost(),traceOrgMailServer.getMailPort(),mailSubject,"Hello "+traceOrgMailServer.getMailUserName()+",<br><br> <t>Please Find the Attachment for Report of IP Address Manager <br><br> Thank You ",traceOrgMailServer.getMailFromEmail(),emailTo,traceOrgMailServer.getMailProtocol(),traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailPassword(),traceOrgMailServer.getMailTimeout(),file);
                                }
                            }
                            else
                            {
                                sendMailWithAttachment(traceOrgMailServer.getMailHost(),traceOrgMailServer.getMailPort(),mailSubject,"Hello "+traceOrgMailServer.getMailUserName()+",<br><br> <t>Please Find the Attachment for Report of IP Address Manager <br><br> Thank You ",traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailToEmail(),traceOrgMailServer.getMailProtocol(),traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailPassword(),traceOrgMailServer.getMailTimeout(),file);
                            }

                        }
                    }
                    catch (Exception exception2)
                    {
                        _logger.error(exception2);
                    }
                    _logger.error(exception);
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }

    public  void sendMailWithAttachment(String mailServerHost, int mailServerPort, String subject, String message, String sender, String recipients, String securityType, String userName, String password, int timeout,File file) throws Exception
    {
        try
        {
            HtmlEmail email = new HtmlEmail();

            email.setHostName(mailServerHost);

            email.setSmtpPort(mailServerPort);

            email.setFrom(sender);

            email.setDebug(false);

            email.setSubject(subject);

            email.attach(file);

            if (message != null && message.length() > 0)
            {
                email.setHtmlMsg(message);
            }
            else
            {
                email.setHtmlMsg("Empty Email Body !!!");
            }

            if (recipients != null && recipients.length() > 0)
            {
                String[] var11 = recipients.split(",");

                int var12 = var11.length;

                for (int var13 = 0; var13 < var12; ++var13)
                {
                    String recipient = var11[var13];

                    email.addTo(recipient);
                }
            }

            email.setSocketTimeout(timeout * 1000);

            email.setSocketConnectionTimeout(timeout * 1000);

            if (userName != null && password != null && userName.length() > 0 && password.length() > 0)
            {
                email.setAuthentication(userName, password);
            }

            if (securityType.equalsIgnoreCase("ssl"))
            {
                email.setSslSmtpPort(String.valueOf(mailServerPort));

                email.setSSLOnConnect(true);
            }
            else if (securityType.equalsIgnoreCase("tls"))
            {
                email.setStartTLSEnabled(true);
            }
            email.send();
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
    }
}
