package com.example.demo.restcontroller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.Response;
import com.motadata.traceorg.ipam.model.TraceOrgReportScheduler;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetIpDetails;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.TraceOrgCommonConstants;
import com.motadata.traceorg.ipam.util.TraceOrgCommonUtil;
import com.motadata.traceorg.ipam.util.TraceOrgMessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("ALL")
@RestController
public class TraceOrgReportSchedulerController
{
    @Autowired
    private TraceOrgService traceOrgService;

    @Autowired
    private TraceOrgCommonUtil traceOrgCommonUtil;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgReportSchedulerController.class, "Report Scheduler Controller");

    @RequestMapping(value = TraceOrgCommonConstants.REPORT_SCHEDULER_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllReportScheduler(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgReportScheduler> traceOrgReportSchedulers = (List< TraceOrgReportScheduler>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_REPORT_SCHEDULER);

                if(traceOrgReportSchedulers !=null && !traceOrgReportSchedulers.isEmpty())
                {
                    response.setData(traceOrgReportSchedulers);

                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                }
                else
                {
                    response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                }
            }
            else
            {
                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.REPORT_SCHEDULER_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> listReportScheduler(HttpServletRequest request, @PathVariable(TraceOrgCommonConstants.ID) Long id)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    TraceOrgReportScheduler traceOrgReportScheduler = (TraceOrgReportScheduler) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_REPORT_SCHEDULER,id);

                    if(traceOrgReportScheduler !=null)
                    {
                        response.setData(traceOrgReportScheduler);

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                    }
                    else
                    {
                        response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.REPORT_SCHEDULER_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> insertReportScheduler(HttpServletRequest request, @RequestBody TraceOrgReportScheduler traceOrgReportScheduler)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    if (traceOrgReportScheduler.getIpFilter()!=null && !traceOrgReportScheduler.getIpFilter().trim().isEmpty() && traceOrgReportScheduler.getSchedulerName()!=null && !traceOrgReportScheduler.getSchedulerName().trim().isEmpty() && traceOrgReportScheduler.getSchedulerTime()!=null && !traceOrgReportScheduler.getSchedulerTime().isEmpty() && ((traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("All IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Used IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Reserved IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Available IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Transient IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Rogue IP")) ? (traceOrgReportScheduler.getSubnetId()!=null && !traceOrgReportScheduler.getSubnetId().trim().isEmpty()) : true))
                    {
                        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                        Date schedulerTime = simpleDateFormat.parse(traceOrgReportScheduler.getSchedulerDate()+" "+traceOrgReportScheduler.getSchedulerTime());

                        if(schedulerTime.after(new Date()))
                        {
                            switch(traceOrgReportScheduler.getIpFilter().toUpperCase())
                            {
                                case "ALL IP" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.ALL_IP_REPORT);
                                    break;
                                case "USED IP" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.USED_IP_REPORT);
                                    break;
                                case "RESERVED IP" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.RESERVED_IP_REPORT);
                                    break;
                                case "EVENT LOG" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.EVENT_LOG_REPORT);
                                    break;
                                case "AVAILABLE IP" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.AVAILABLE_IP_REPORT);
                                    break;
                                case "TRANSIENT IP" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.TRANSIENT_IP_REPORT);
                                    break;
                                case "ROGUE IP" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.ROGUE_IP_REPORT);
                                    break;
                                case "CONFLICT IP" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.CONFLICT_IP_REPORT);
                                    break;
                                case "SUBNET UTILIZATION" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.SUBNET_UTILIZATION_REPORT);
                                    break;
                                case "DHCP UTILIZATION" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.DHCP_UTILIZATION_REPORT);
                                    break;
                                case "VENDOR SUMMARY" :
                                    traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.VENDOR_SUMMARY_REPORT);
                                    break;
                                default:
                                    traceOrgReportScheduler.setIpFilter(null);
                                    break;
                            }

                            switch(traceOrgReportScheduler.getExportType().toUpperCase())
                            {
                                case "PDF" :
                                    traceOrgReportScheduler.setExportType("PDF");
                                    break;
                                case "CSV" :
                                    traceOrgReportScheduler.setExportType("CSV");
                                    break;
                                default:
                                    traceOrgReportScheduler.setExportType(null);
                                    break;
                            }

                            if(traceOrgReportScheduler.getIpFilter() !=null && traceOrgReportScheduler.getExportType()!=null)
                            {
                                boolean insertStatus = this.traceOrgService.insert(traceOrgReportScheduler);

                                if(insertStatus)
                                {
                                    traceOrgCommonUtil.scheduleCustomJob(traceOrgReportScheduler,this.traceOrgService,traceOrgCommonUtil);

                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SCHEDULER_ADD_SUCCESS);
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
                                }
                            }
                            else
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
                            }
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage("Past Time Scheduler can not be scheduled");
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.DO_NOT_HAVE_ACCESS);
                }
            }
            else
            {
                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.REPORT_SCHEDULER_REST_URL + "{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateReportScheduler(HttpServletRequest request, @PathVariable Long id, @RequestBody TraceOrgReportScheduler traceOrgReportScheduler)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        if (id!=null && traceOrgReportScheduler.getIpFilter()!=null && !traceOrgReportScheduler.getIpFilter().trim().isEmpty() && traceOrgReportScheduler.getSchedulerName()!=null && !traceOrgReportScheduler.getSchedulerName().trim().isEmpty() && traceOrgReportScheduler.getSchedulerTime()!=null && !traceOrgReportScheduler.getSchedulerTime().isEmpty() && ((traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("All IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Used IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Reserved IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Available IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Transient IP") || traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Rogue IP")|| traceOrgReportScheduler.getIpFilter().equalsIgnoreCase("Vendor Summary")) ? (traceOrgReportScheduler.getSubnetId()!=null && !traceOrgReportScheduler.getSubnetId().trim().isEmpty()) : true))
                        {
                            DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                            Date schedulerTime = simpleDateFormat.parse(traceOrgReportScheduler.getSchedulerDate()+" "+traceOrgReportScheduler.getSchedulerTime());

                            if(schedulerTime.after(new Date()))
                            {
                                TraceOrgReportScheduler exitstedTraceOrgReportScheduler = (TraceOrgReportScheduler)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_REPORT_SCHEDULER,id);

                                if(exitstedTraceOrgReportScheduler !=null)
                                {
                                    exitstedTraceOrgReportScheduler = traceOrgReportScheduler ;

                                    switch(traceOrgReportScheduler.getIpFilter().toUpperCase())
                                    {
                                        case "ALL IP" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.ALL_IP_REPORT);
                                            break;
                                        case "USED IP" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.USED_IP_REPORT);
                                            break;
                                        case "RESERVED IP" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.RESERVED_IP_REPORT);
                                            break;
                                        case "EVENT LOG" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.EVENT_LOG_REPORT);
                                            break;
                                        case "AVAILABLE IP" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.AVAILABLE_IP_REPORT);
                                            break;
                                        case "ROGUE IP" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.ROGUE_IP_REPORT);
                                            break;
                                        case "TRANSIENT IP" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.TRANSIENT_IP_REPORT);
                                            break;
                                        case "CONFLICT IP" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.CONFLICT_IP_REPORT);
                                            break;
                                        case "SUBNET UTILIZATION" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.SUBNET_UTILIZATION_REPORT);
                                            break;
                                        case "DHCP UTILIZATION" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.DHCP_UTILIZATION_REPORT);
                                            break;
                                        case "VENDOR SUMMARY" :
                                            traceOrgReportScheduler.setIpFilter(TraceOrgCommonConstants.VENDOR_SUMMARY_REPORT);
                                            break;
                                        default:
                                            traceOrgReportScheduler.setIpFilter(null);
                                            break;
                                    }

                                    switch(traceOrgReportScheduler.getExportType().toUpperCase())
                                    {
                                        case "PDF" :
                                            traceOrgReportScheduler.setExportType("PDF");
                                            break;
                                        case "CSV" :
                                            traceOrgReportScheduler.setExportType("CSV");
                                            break;
                                        default:
                                            traceOrgReportScheduler.setExportType(null);
                                            break;
                                    }

                                    if(traceOrgReportScheduler.getIpFilter() !=null && traceOrgReportScheduler.getExportType()!=null)
                                    {
                                        exitstedTraceOrgReportScheduler.setSubnetId(traceOrgReportScheduler.getSubnetId());

                                        exitstedTraceOrgReportScheduler.setId(id);

                                        boolean insertStatus =  this.traceOrgService.insert(exitstedTraceOrgReportScheduler);

                                        if(insertStatus)
                                        {
                                            traceOrgCommonUtil.removeReportCustomJob(traceOrgReportScheduler);

                                            traceOrgCommonUtil.scheduleCustomJob(traceOrgReportScheduler,this.traceOrgService,traceOrgCommonUtil);

                                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                            response.setMessage(TraceOrgMessageConstants.SCHEDULER_UPDATE_SUCCESS);
                                        }
                                    }
                                    else
                                    {
                                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                                        response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
                                    }
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
                                }
                            }
                            else
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setMessage("Past Time Scheduler can not be scheduled");
                            }
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setMessage(TraceOrgMessageConstants.DO_NOT_HAVE_ACCESS);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.REPORT_SCHEDULER_REST_URL+"{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeReportScheduler(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id!=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {

                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        TraceOrgReportScheduler traceOrgReportScheduler = (TraceOrgReportScheduler) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_REPORT_SCHEDULER, id);

                        if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                        {
                            if (traceOrgReportScheduler != null)
                            {

                                traceOrgCommonUtil.removeReportCustomJob(traceOrgReportScheduler);

                                boolean deleteStatus = this.traceOrgService.delete(TraceOrgCommonConstants.TRACE_ORG_REPORT_SCHEDULER, TraceOrgCommonConstants.ID, TraceOrgCommonUtil.getStringValue(id));

                                if(deleteStatus)
                                {
                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setMessage(TraceOrgMessageConstants.REPORT_SCHEDULER_DELETE_SUCCESS);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                                }
                            }
                            else
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                            }
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.DO_NOT_HAVE_ACCESS);
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setMessage(TraceOrgMessageConstants.DO_NOT_HAVE_ACCESS);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_ALL_IP_PDF_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportAllIpPdfReport(HttpServletRequest request, @RequestParam Integer exportTimeline, @RequestParam String subnetId)
    {
        Response response = new Response();

        if(exportTimeline!=null && subnetId!=null && !subnetId.trim().isEmpty())
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    try
                    {
                        String fileName = traceOrgCommonUtil.exportAllIpReportPdf(exportTimeline,subnetId);

                        if(fileName!=null && !fileName.isEmpty())
                        {
                            response.setData(fileName);

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                    }
                    catch (Exception exception)
                    {
                        _logger.error(exception);

                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_ALL_USED_IP_PDF_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportUsedIpPdfReport(HttpServletRequest request, @RequestParam Integer exportTimeline)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                try
                {
                    String fileName = traceOrgCommonUtil.exportAllEventReportPdf(exportTimeline);

                    if(fileName!=null && !fileName.isEmpty())
                    {
                        response.setData(fileName);

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                    }
                    else
                    {
                        response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                    }
                }
                catch (Exception exception)
                {
                    _logger.error(exception);

                    response.setSuccess(TraceOrgCommonConstants.FALSE);
                }
            }
            else
            {
                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_ALL_AVAILABLE_IP_PDF_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportAllAvailableIpPdfReport(HttpServletRequest request, @RequestParam Integer exportTimeline, @RequestParam String subnetId)
    {
        Response response = new Response();

        if(subnetId!=null && !subnetId.trim().isEmpty() && exportTimeline!=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    try
                    {
                        String fileName = traceOrgCommonUtil.exportAllAvailableIpReportPdf(exportTimeline,subnetId);

                        if(fileName!=null && !fileName.isEmpty())
                        {
                            response.setData(fileName);

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                    }
                    catch (Exception exception)
                    {
                        _logger.error(exception);

                        response.setSuccess(TraceOrgCommonConstants.FALSE);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_ALL_TRANSIENT_IP_PDF_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportAllTransientIpPdfReport(HttpServletRequest request, @RequestParam Integer exportTimeline, @RequestParam String subnetId)
    {
        Response response = new Response();

        if(subnetId!=null && !subnetId.trim().isEmpty() && exportTimeline!=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    try
                    {
                        String fileName = traceOrgCommonUtil.exportAllAvailableIpReportPdf(exportTimeline,subnetId);

                        if(fileName!=null && !fileName.isEmpty())
                        {
                            response.setData(fileName);

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                    }
                    catch (Exception exception)
                    {
                        _logger.error(exception);

                        response.setSuccess(TraceOrgCommonConstants.FALSE);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_ALL_RESERVED_IP_PDF_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportAllReservedIpPdfReport(HttpServletRequest request, @RequestParam Integer exportTimeline)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                try
                {
                    String fileName = traceOrgCommonUtil.exportAllEventReportPdf(exportTimeline);

                    if(fileName!=null && !fileName.isEmpty())
                    {
                        response.setData(fileName);

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                    }
                    else
                    {
                        response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                    }
                }
                catch (Exception exception)
                {
                    _logger.error(exception);

                    response.setSuccess(TraceOrgCommonConstants.FALSE);
                }
            }
            else
            {
                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);

            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_EVENT_PDF_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportEventPdfReport(HttpServletRequest request, @RequestParam Integer exportTimeline)
    {
        Response response = new Response();

        if(exportTimeline != null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    try
                    {
                        String fileName = traceOrgCommonUtil.exportAllEventReportPdf(exportTimeline);

                        if(fileName!=null && !fileName.isEmpty())
                        {
                            response.setData(fileName);

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }

                    }
                    catch (Exception exception)
                    {
                        _logger.error(exception);

                        response.setSuccess(TraceOrgCommonConstants.FALSE);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_EVENT_CSV_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportEventCsvReport(HttpServletRequest request, @RequestParam Integer exportTimeline)
    {
        Response response = new Response();

        if(exportTimeline != null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    try
                    {
                        String fileName = traceOrgCommonUtil.exportAllEventReportCsv(exportTimeline);

                        if(fileName!=null && !fileName.isEmpty())
                        {
                            response.setData(fileName);

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }

                    }
                    catch (Exception exception)
                    {
                        _logger.error(exception);

                        response.setSuccess(TraceOrgCommonConstants.FALSE);
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_SUBNET_IP_BY_REPORT_TIMELINE, method = RequestMethod.GET)
    public ResponseEntity<?> exportSubnetIpReportByTimeline(HttpServletRequest request, @RequestParam String subnetId, @RequestParam String ipStatus, @RequestParam Integer exportTimeline)
    {
        Response response = new Response();

        if(subnetId !=null && !subnetId.trim().isEmpty() && ipStatus!=null && !ipStatus.trim().isEmpty() && exportTimeline!=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

                    List<Map<String,Object>> vendorSummaryList = new ArrayList<>() ;

                    List<TraceOrgSubnetIpDetails> outputIpDetailsList = new ArrayList<>();

                    switch (ipStatus.toUpperCase())
                    {
                        case "USED IP" :
                            ipStatus = TraceOrgCommonConstants.USED;
                            break;
                        case "AVAILABLE IP" :
                            ipStatus = TraceOrgCommonConstants.AVAILABLE;
                            break;
                        case "RESERVED IP" :
                            ipStatus = TraceOrgCommonConstants.RESERVED;
                            break;
                        case "TRANSIENT IP" :
                            ipStatus = TraceOrgCommonConstants.TRANSIENT;
                            break;
                        case "ALL IP" :
                            ipStatus = "All";
                            break;
                    }

                    if(ipStatus.equals(TraceOrgCommonConstants.USED) || ipStatus.equals(TraceOrgCommonConstants.AVAILABLE) || ipStatus.equals(TraceOrgCommonConstants.TRANSIENT) || ipStatus.equals(TraceOrgCommonConstants.RESERVED))
                    {
                        switch (exportTimeline)
                        {
                            case 1 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 2 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 3 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 4 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 5 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 6 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 7 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"'  order by INET_ATON(ipAddress)");
                                break;
                            case 8 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 9 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 10 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where subnetId in ("+subnetId+") and  deactiveStatus = false and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 11 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 12 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            default:
                                subnetIpDetailsList = null;
                                break;
                        }
                    }
                    else if(ipStatus.equalsIgnoreCase("ALL"))
                    {
                        switch (exportTimeline)
                        {
                            case 1 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 2 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 3 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 4 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 5 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                                break;
                            case 6 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                                break;
                            case 7 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                                break;
                            case 8 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                                break;
                            case 9 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                                break;
                            case 10 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
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
                    }
                    else if (ipStatus.equals("ROGUE IP"))
                    {
                        switch (exportTimeline)
                        {
                            case 1 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress) ");
                                break;
                            case 2 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress) ");
                                break;
                            case 3 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress) ");
                                break;
                            case 4 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress) ");
                                break;
                            case 5 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 6 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 7 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress)");
                                break;
                            case 8 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress)");
                                break;
                            case 9 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 10 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where subnetId in ("+subnetId+") and  deactiveStatus = false and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 11 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 12 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            default:
                                subnetIpDetailsList = null;
                                break;
                        }
                    }
                    else if(ipStatus.equalsIgnoreCase("VENDOR SUMMARY"))
                    {
                        List<Object> vendorList = null;

                        switch (exportTimeline)
                        {
                            case 1 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and Date(modifiedDate) = CURDATE() and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 2 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and DATE(modifiedDate) = DATE(CURDATE() -1) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 3 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 4 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 5 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 6 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 7 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6  group by deviceType order by devicenumber desc");
                                break;
                            case 8 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 group by deviceType order by devicenumber desc");
                                break;
                            case 9 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and YEAR(modifiedDate) = YEAR(curdate()) - 1 group by deviceType order by devicenumber desc");
                                break;
                            case 10 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  group by deviceType order by devicenumber desc");
                                break;
                            case 11 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 12 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            default:
                                subnetIpDetailsList = null;
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
                    }

                    if(!ipStatus.equalsIgnoreCase("VENDOR SUMMARY"))
                    {
                        if(subnetIpDetailsList!=null && !subnetIpDetailsList.isEmpty())
                        {
                            response.setData(traceOrgCommonUtil.exportIpReportPdf(subnetIpDetailsList,ipStatus));

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);
                        }
                    }
                    else
                    {
                        if(vendorSummaryList!=null && !vendorSummaryList.isEmpty())
                        {
                            response.setData(traceOrgCommonUtil.exportVendorSummaryReportPdf(vendorSummaryList,ipStatus));

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);
                        }
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_SUBNET_IP_CSV_BY_REPORT_TIMELINE, method = RequestMethod.GET)
    public ResponseEntity<?> exportSubnetIpCsvReportByTimeline(HttpServletRequest request, @RequestParam String subnetId, @RequestParam String ipStatus, @RequestParam Integer exportTimeline)
    {
        Response response = new Response();

        if(subnetId !=null && !subnetId.trim().isEmpty() && ipStatus!=null && !ipStatus.trim().isEmpty() && exportTimeline!=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    List<TraceOrgSubnetIpDetails> subnetIpDetailsList = null ;

                    List<Map<String,Object>> vendorSummaryList = new ArrayList<>() ;

                    List<TraceOrgSubnetIpDetails> outputIpDetailsList = new ArrayList<>();

                    switch (ipStatus.toUpperCase())
                    {
                        case "USED IP" :
                            ipStatus = TraceOrgCommonConstants.USED;
                            break;
                        case "AVAILABLE IP" :
                            ipStatus = TraceOrgCommonConstants.AVAILABLE;
                            break;
                        case "RESERVED IP" :
                            ipStatus = TraceOrgCommonConstants.RESERVED;
                            break;
                        case "TRANSIENT IP" :
                            ipStatus = TraceOrgCommonConstants.TRANSIENT;
                            break;
                    }

                    if(ipStatus.equals(TraceOrgCommonConstants.USED) || ipStatus.equals(TraceOrgCommonConstants.AVAILABLE) || ipStatus.equals(TraceOrgCommonConstants.TRANSIENT) || ipStatus.equals(TraceOrgCommonConstants.RESERVED))
                    {
                        switch (exportTimeline)
                        {
                            case 1 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 2 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 3 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 4 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress) ");
                                break;
                            case 5 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 6 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 7 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"'  order by INET_ATON(ipAddress)");
                                break;
                            case 8 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 9 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 10 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where subnetId in ("+subnetId+") and  deactiveStatus = false and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 11 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            case 12 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and status= '"+ipStatus+"' order by INET_ATON(ipAddress)");
                                break;
                            default:
                                subnetIpDetailsList = null;
                                break;
                        }
                    }
                    else if(ipStatus.equalsIgnoreCase("ALL IP"))
                    {
                        switch (exportTimeline)
                        {
                            case 1 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 2 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 3 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 4 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress) ");
                                break;
                            case 5 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                                break;
                            case 6 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                                break;
                            case 7 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                                break;
                            case 8 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 and  deactiveStatus = false and subnetId in ("+subnetId+")  order by INET_ATON(ipAddress)");
                                break;
                            case 9 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") order by INET_ATON(ipAddress)");
                                break;
                            case 10 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where subnetId in ("+subnetId+") and  deactiveStatus = false order by INET_ATON(ipAddress)");
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
                    }
                    else if (ipStatus.equals("ROGUE IP"))
                    {
                        switch (exportTimeline)
                        {
                            case 1 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where Date(modifiedDate) = CURDATE() and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress) ");
                                break;
                            case 2 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where DATE(modifiedDate) = DATE(CURDATE() -1) and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress) ");
                                break;
                            case 3 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress) ");
                                break;
                            case 4 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress) ");
                                break;
                            case 5 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 6 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 7 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6 and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress)");
                                break;
                            case 8 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true  order by INET_ATON(ipAddress)");
                                break;
                            case 9 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where YEAR(modifiedDate) = YEAR(curdate()) - 1 and  deactiveStatus = false and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 10 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where subnetId in ("+subnetId+") and  deactiveStatus = false and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 11 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where WEEK(modifiedDate) =  WEEK(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            case 12 :
                                subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where MONTH(modifiedDate)= MONTH(curdate()) - 1 and  deactiveStatus = false and YEAR(modifiedDate) = YEAR(CURDATE()) and subnetId in ("+subnetId+") and rogueStatus=true order by INET_ATON(ipAddress)");
                                break;
                            default:
                                subnetIpDetailsList = null;
                                break;
                        }
                    }
                    else if(ipStatus.equalsIgnoreCase("VENDOR SUMMARY"))
                    {
                        List<Object> vendorList = null;

                        switch (exportTimeline)
                        {
                            case 1 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and Date(modifiedDate) = CURDATE() and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 2 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and DATE(modifiedDate) = DATE(CURDATE() -1) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 3 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and WEEK(modifiedDate) =  WEEK(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 4 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and MONTH(modifiedDate)= MONTH(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 5 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and QUARTER(modifiedDate) = QUARTER(curdate()) and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 6 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and QUARTER(modifiedDate) = QUARTER(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 7 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and TIMESTAMPDIFF(MONTH, modifiedDate, NOW()) < 6  group by deviceType order by devicenumber desc");
                                break;
                            case 8 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and TIMESTAMPDIFF(YEAR, modifiedDate, NOW()) < 1 group by deviceType order by devicenumber desc");
                                break;
                            case 9 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and YEAR(modifiedDate) = YEAR(curdate()) - 1 group by deviceType order by devicenumber desc");
                                break;
                            case 10 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  group by deviceType order by devicenumber desc");
                                break;
                            case 11 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and WEEK(modifiedDate) =  WEEK(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
                                break;
                            case 12 :
                                vendorList = (List<Object>) this.traceOrgService.commonQuery(TraceOrgCommonConstants.SELECT_VENDOR_WITH_COUNT_FOR_REPORT.replace("subnetIdValue",subnetId),"TraceOrgSubnetIpDetails where deactiveStatus = false and subnetId in("+subnetId+")  and MONTH(modifiedDate)= MONTH(curdate()) - 1 and YEAR(modifiedDate) = YEAR(CURDATE()) group by deviceType order by devicenumber desc");
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
                    }

                    if(!ipStatus.equalsIgnoreCase("VENDOR SUMMARY"))
                    {
                        if(subnetIpDetailsList!=null && !subnetIpDetailsList.isEmpty())
                        {
                            response.setData(traceOrgCommonUtil.exportIpReportCsv(subnetIpDetailsList,ipStatus));

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);
                        }
                    }
                    else
                    {
                        if(vendorSummaryList!=null && !vendorSummaryList.isEmpty())
                        {
                            response.setData(traceOrgCommonUtil.exportVendorSummaryReportCsv(vendorSummaryList,ipStatus));

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);
                        }
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            catch (Exception exception)
            {
                _logger.error(exception);

                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
            }
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
