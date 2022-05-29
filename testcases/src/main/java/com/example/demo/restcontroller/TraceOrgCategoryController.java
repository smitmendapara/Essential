package com.example.demo.restcontroller;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.*;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.TraceOrgCommonConstants;
import com.motadata.traceorg.ipam.util.TraceOrgCommonUtil;
import com.motadata.traceorg.ipam.util.TraceOrgMessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"unchecked","SpringAutowiredFieldsWarningInspection"})
@RestController
public class TraceOrgCategoryController {

    @Autowired
    private TraceOrgService traceOrgService;

    @Autowired
    private TraceOrgCommonUtil traceOrgCommonUtil;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgCategoryController.class, "Category Controller");


    @RequestMapping(value = TraceOrgCommonConstants.CATEGORY_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listCategory(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgCategory> traceOrgCategories = (List<TraceOrgCategory>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_CATEGORY);

                if(traceOrgCategories !=null && !traceOrgCategories.isEmpty())
                {
                    response.setData(traceOrgCategories);

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
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @RequestMapping(value = TraceOrgCommonConstants.CATEGORY_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> insertCategory(HttpServletRequest request, @RequestBody TraceOrgCategory traceOrgCategory)
    {
        Response response = new Response();

        if(traceOrgCategory.getCategoryName() !=null && !traceOrgCategory.getCategoryName().trim().isEmpty())
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        if (this.traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_CATEGORY, TraceOrgCommonConstants.CATEGORY_NAME,traceOrgCategory.getCategoryName()))
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.CATEGORY_NAME_ALREADY_EXIST);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            boolean insertStatus = this.traceOrgService.insert(traceOrgCategory);

                            if(insertStatus)
                            {
                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                response.setMessage(TraceOrgMessageConstants.CATEGORY_ADD_SUCCESS);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                            }
                            else
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                            }
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


    @RequestMapping(value = TraceOrgCommonConstants.CATEGORY_REST_URL+"{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateCategory(HttpServletRequest request, @PathVariable Long id , @RequestBody TraceOrgCategory traceOrgCategory)
    {
        Response response = new Response();

        if(id!=null && traceOrgCategory.getCategoryName() !=null && !traceOrgCategory.getCategoryName().trim().isEmpty())
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        TraceOrgCategory existedCategory = (TraceOrgCategory)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_CATEGORY,id);

                        if(existedCategory!=null)
                        {
                            List<TraceOrgCategory> categoryList = (List<TraceOrgCategory>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_CATEGORY + " where categoryName = '"+traceOrgCategory.getCategoryName()+"'");

                            if(categoryList!=null && !categoryList.isEmpty() && categoryList.size()<2)
                            {
                                if(categoryList.get(0).getId().equals(existedCategory.getId()))
                                {
                                    existedCategory.setCategoryName(traceOrgCategory.getCategoryName());

                                    boolean insertStatus = this.traceOrgService.insert(existedCategory);

                                    if(insertStatus)
                                    {
                                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                                        response.setMessage(TraceOrgMessageConstants.CATEGORY_UPDATE_SUCCESS);

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

                                    response.setMessage(TraceOrgMessageConstants.CATEGORY_NAME_ALREADY_EXIST);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                                }
                            }
                            else
                            {
                                existedCategory.setCategoryName(traceOrgCategory.getCategoryName());

                                boolean insertStatus = this.traceOrgService.insert(existedCategory);

                                if(insertStatus)
                                {
                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setMessage(TraceOrgMessageConstants.CATEGORY_UPDATE_SUCCESS);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                                }
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


    @RequestMapping(value = TraceOrgCommonConstants.CATEGORY_REST_URL+"{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeCategory(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            if(id==1)
            {
                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage("Default Category can not be removed");
            }
            else
            {
                try
                {
                    String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                    if(traceOrgCommonUtil.checkToken(accessToken) )
                    {
                        if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                        {
                            if(TraceOrgCommonUtil.getCSVImportCount() > 0)
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setMessage(TraceOrgMessageConstants.IMPORT_RUNNING);
                            }
                            else if(TraceOrgCommonUtil.getSubnetScanStatus() != 0)
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                response.setMessage(TraceOrgMessageConstants.CANT_DELETE_CATEGORY_UNDER_SCAN);
                            }
                            else
                            {
                                TraceOrgCategory traceOrgCategory = (TraceOrgCategory) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_CATEGORY,id);

                                if (traceOrgCategory != null)
                                {
                                    List<TraceOrgSubnetDetails> traceOrgSubnetDetailsList = (List<TraceOrgSubnetDetails>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS+" where traceOrgCategory = '"+id+"'");

                                    if(traceOrgSubnetDetailsList !=null && !traceOrgSubnetDetailsList.isEmpty())
                                    {
                                        String subnetAddress = null;

                                        for(TraceOrgSubnetDetails traceOrgSubnetDetails : traceOrgSubnetDetailsList)
                                        {

                                            if(subnetAddress !=null)
                                            {
                                                subnetAddress = subnetAddress + ", " +traceOrgSubnetDetails.getSubnetAddress();
                                            }
                                            else
                                            {
                                                subnetAddress = traceOrgSubnetDetails.getSubnetAddress();
                                            }


                                            this.traceOrgService.delete(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,TraceOrgCommonConstants.SUBNET_ID,traceOrgSubnetDetails.getId().toString());

                                            //Remove Cron Subnet Scan
                                            traceOrgCommonUtil.removeScanSubnetCron(traceOrgSubnetDetails);

                                            //EVENT LOG
                                            TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                                            traceOrgEvent.setTimestamp(new Date());

                                            traceOrgEvent.setDoneBy(traceOrgCommonUtil.currentUser(accessToken));

                                            traceOrgEvent.setEventType("Delete Subnet");

                                            traceOrgEvent.setEventContext("Subnet "+traceOrgSubnetDetails.getSubnetAddress()+" is deleted from IP Address Manager by "+traceOrgCommonUtil.currentUserName(accessToken)  );

                                            traceOrgEvent.setSeverity(1);

                                            this.traceOrgService.insert(traceOrgEvent);
                                        }

                                        this.traceOrgService.delete(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,"traceOrgCategory",TraceOrgCommonUtil.getStringValue(id));

                                        this.traceOrgService.delete(TraceOrgCommonConstants.TRACE_ORG_CATEGORY,TraceOrgCommonConstants.ID,TraceOrgCommonUtil.getStringValue(id));

                                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                        response.setMessage(TraceOrgMessageConstants.CATEGORY_DELETE_SUCCESS);


                                        if(subnetAddress!=null)
                                        {
                                            String mailBody = null;

                                            try
                                            {
                                                TraceOrgMailServer traceOrgMailServer =(TraceOrgMailServer) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_MAIL_SERVER, 1L);

                                                if(traceOrgMailServer != null)
                                                {
                                                    if(subnetAddress.contains(",") && subnetAddress.split(",").length>0)
                                                    {
                                                        String[] subnets = subnetAddress.split(",");

                                                        mailBody  = "Hello "+traceOrgMailServer.getMailUserName()+",<br><br> <t>Subnet Deleted in IP Address Manager By "+traceOrgCommonUtil.currentUserName(accessToken)+".<br><br> <table style =\"border: 1px solid\" > <tr> <th style =\"border: 1px solid\">Subnet Address</th> </tr>";

                                                        for(String subnet :subnets)
                                                        {
                                                            mailBody = mailBody +"<tr> <td style =\"border: 1px solid\">"+subnet+"</td> </tr>";
                                                        }
                                                        mailBody =mailBody + "</table><br><br> Thank You\n";

                                                    }
                                                    else
                                                    {
                                                        mailBody = "Hello "+traceOrgMailServer.getMailUserName()+",<br><br> <t>Subnet " + subnetAddress +" Deleted  in IP Address Manager By "+traceOrgCommonUtil.currentUserName(accessToken)+". <br><br> Thank You ";
                                                    }

                                                    TraceOrgCommonUtil.sendMail(traceOrgMailServer.getMailHost(),traceOrgMailServer.getMailPort(),"Subnet Deleted In IP Address Manager",mailBody,traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailToEmail(),traceOrgMailServer.getMailProtocol(),traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailPassword(),traceOrgMailServer.getMailTimeout());
                                                }
                                            }
                                            catch (Exception exception)
                                            {
                                                try
                                                {
                                                    TraceOrgMailServer traceOrgMailServer = (TraceOrgMailServer) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_MAIL_SERVER, 2L);

                                                    if (traceOrgMailServer != null)
                                                    {

                                                        if(subnetAddress.contains(",") && subnetAddress.split(",").length >0)
                                                        {
                                                            String[] subnets = subnetAddress.split(",");

                                                            mailBody  = "Hello "+traceOrgMailServer.getMailUserName()+",<br><br> <t>Subnet Deleted in IP Address Manager By "+traceOrgCommonUtil.currentUserName(accessToken)+".<br><br> <table style =\"border: 1px solid\" > <tr> <th style =\"border: 1px solid\">Subnet Address</th> </tr>";

                                                            for(String subnet :subnets)
                                                            {
                                                                mailBody = mailBody +"<tr> <td style =\"border: 1px solid\">"+subnet+"</td> </tr>";
                                                            }
                                                            mailBody =mailBody + "</table><br><br> Thank You\n";

                                                        }
                                                        else
                                                        {
                                                            mailBody = "Hello "+traceOrgCommonUtil.currentUserName(accessToken)+",<br><br> <t>Subnet " + subnetAddress +" Deleted  in IP Address Manager By "+traceOrgCommonUtil.currentUserName(accessToken)+". <br><br> Thank You ";
                                                        }

                                                        TraceOrgCommonUtil.sendMail(traceOrgMailServer.getMailHost(), traceOrgMailServer.getMailPort(), "Subnet Deleted In IP Address Manager", mailBody, traceOrgMailServer.getMailFromEmail(), traceOrgMailServer.getMailToEmail(), traceOrgMailServer.getMailProtocol(), traceOrgMailServer.getMailFromEmail(), traceOrgMailServer.getMailPassword(), traceOrgMailServer.getMailTimeout());
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
                                    else
                                    {
                                        this.traceOrgService.delete(TraceOrgCommonConstants.TRACE_ORG_CATEGORY,TraceOrgCommonConstants.ID,TraceOrgCommonUtil.getStringValue(id));

                                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                        response.setMessage(TraceOrgMessageConstants.CATEGORY_DELETE_SUCCESS);
                                    }
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.CATEGORY_ID_NOT_VALID);
                                }
                            }
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

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
        }
        else
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
