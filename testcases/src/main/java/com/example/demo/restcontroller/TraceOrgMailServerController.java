package com.example.demo.restcontroller;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.Response;
import com.motadata.traceorg.ipam.model.TraceOrgMailServer;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.TraceOrgCommonConstants;
import com.motadata.traceorg.ipam.util.TraceOrgCommonUtil;
import com.motadata.traceorg.ipam.util.TraceOrgMessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@SuppressWarnings("ALL")
@RestController
public class TraceOrgMailServerController {


    @Autowired
    private TraceOrgService traceOrgService;

    @Autowired
    private TraceOrgCommonUtil traceOrgCommonUtil;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgMailServer.class, "Mail Server Controller");


    @RequestMapping(value = TraceOrgCommonConstants.MAIL_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllMailServer(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgMailServer> traceOrgMailServerList = (List<TraceOrgMailServer>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_MAIL_SERVER);

                response.setData(traceOrgMailServerList);

                response.setSuccess(TraceOrgCommonConstants.TRUE);

                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
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


    @RequestMapping(value = TraceOrgCommonConstants.MAIL_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getMailServer(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken) && traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    TraceOrgMailServer traceOrgMailServer = (TraceOrgMailServer) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_MAIL_SERVER, id);

                    if (traceOrgMailServer != null)
                    {
                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setData(traceOrgMailServer);
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.MAIL_SERVER_ID_NOT_VALID);
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

    @RequestMapping(value = TraceOrgCommonConstants.MAIL_REST_URL+"{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateMailServer(@PathVariable Long id, HttpServletRequest request, @RequestBody TraceOrgMailServer traceOrgMailServer)
    {
        Response response = new Response();

        if(id !=null && traceOrgMailServer.getMailHost() !=null && !traceOrgMailServer.getMailHost().trim().isEmpty()
                && traceOrgMailServer.getMailPort() !=null && traceOrgMailServer.getMailUserName()!=null && !traceOrgMailServer.getMailUserName().trim().isEmpty()
                && traceOrgMailServer.getMailPassword() !=null && !traceOrgMailServer.getMailPassword().trim().isEmpty())
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken) && traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    Response response2= traceOrgCommonUtil.testMailServer(traceOrgMailServer);

                    if(response2.isSuccess())
                    {
                        traceOrgMailServer.setId(id);

                        if(id ==1)
                            traceOrgMailServer.setMailType("Primary");
                        else if(id ==2)
                            traceOrgMailServer.setMailType("Secondary");

                        boolean insertStatus = this.traceOrgService.insert(traceOrgMailServer);

                        if(insertStatus)
                        {
                            _logger.debug("Mail server "+traceOrgMailServer.getMailHost() +" updated successfully");

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                            response.setMessage(TraceOrgMessageConstants.MAIL_SERVER_UPDATE_SUCCESS);
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                            response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.MAIL_SERVER_NOT_VALID);
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


    @RequestMapping(value = TraceOrgCommonConstants.INSERT_MAIL_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> insertMailServer(HttpServletRequest request, @RequestParam String mailToEmail)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if (traceOrgCommonUtil.checkToken(accessToken))
            {

                if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    if( mailToEmail!=null && !mailToEmail.trim().isEmpty())
                    {
                        List<Object> traceOrgMails = (List<Object>) this.traceOrgService.sqlQuery("Select Max(id) from mail_server");

                        TraceOrgMailServer traceOrgMailServer = new TraceOrgMailServer();

                        traceOrgMailServer.setMailToEmail(mailToEmail);

                        if(traceOrgMails !=null && !traceOrgMails.isEmpty() && traceOrgMails.get(0)!= null )
                        {
                            if(Long.parseLong(traceOrgMails.get(0).toString()) <=2)
                            {
                                traceOrgMailServer.setId(3L);
                            }
                            else
                            {
                                traceOrgMailServer.setId(Long.parseLong(traceOrgMails.get(0).toString()) + 1 );
                            }
                        }
                        else
                        {
                            traceOrgMailServer.setId(3L);
                        }

                        boolean insertStatus = this.traceOrgService.insert(traceOrgMailServer);

                        if(insertStatus)
                        {
                            _logger.debug("Mail server "+traceOrgMailServer.getMailHost() +" inserted successfully");

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setMessage(TraceOrgMessageConstants.MAIL_ADD_SUCCESS);
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

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

                    response.setMessage(TraceOrgMessageConstants.DO_NOT_HAVE_ACCESS);
                }
            }
            else
            {
                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
            }
        }
        catch(Exception exception )
        {
            response.setSuccess(TraceOrgCommonConstants.FALSE);

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    //Test Mail
    @RequestMapping(value = TraceOrgCommonConstants.MAIL_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> testMailServer(HttpServletRequest request, @RequestBody TraceOrgMailServer traceOrgMailServer)
    {
        Response response = new Response();


        if( traceOrgMailServer.getMailHost() !=null && !traceOrgMailServer.getMailHost().trim().isEmpty()
                && traceOrgMailServer.getMailPort() !=null && traceOrgMailServer.getMailUserName()!=null && !traceOrgMailServer.getMailUserName().trim().isEmpty()
                && traceOrgMailServer.getMailPassword() !=null && !traceOrgMailServer.getMailPassword().trim().isEmpty())
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        try
                        {
                            TraceOrgCommonUtil.sendMail(traceOrgMailServer.getMailHost(),traceOrgMailServer.getMailPort(),"IPAM Test Mail","Hello Admin, Thank You ",traceOrgMailServer.getMailFromEmail(),"dontreplymebuddy@gmail.com",traceOrgMailServer.getMailProtocol(),traceOrgMailServer.getMailFromEmail(),traceOrgMailServer.getMailPassword(),traceOrgMailServer.getMailTimeout());

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setMessage(TraceOrgMessageConstants.MAIL_SERVER_VALID);
                        }
                        catch (Exception exception)
                        {
                            _logger.error(exception);

                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.MAIL_SERVER_NOT_VALID);
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
}
