package com.example.demo.restcontroller;


import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.Response;
import com.motadata.traceorg.ipam.model.TraceOrgGlobalSetting;
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

@RestController
public class TraceOrgGlobalSettingController
{
    @Autowired
    private TraceOrgService traceOrgService;

    @Autowired
    private TraceOrgCommonUtil traceOrgCommonUtil;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgGlobalSettingController.class, "Global Setting Controller");

    @RequestMapping(value = TraceOrgCommonConstants.GLOBAL_SETTING_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllGlobalSetting(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgGlobalSetting> traceOrgGlobalSettings = (List<TraceOrgGlobalSetting>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_GLOBAL_SETTING);

                response.setData(traceOrgGlobalSettings);

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

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @RequestMapping(value = TraceOrgCommonConstants.GLOBAL_SETTING_REST_URL+"{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateGlobalSetting(@PathVariable Long id, HttpServletRequest request, @RequestBody TraceOrgGlobalSetting traceOrgGlobalSetting)
    {
        Response response = new Response();

        if(id !=null && traceOrgGlobalSetting.getLoggingLevel() !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken) && traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    traceOrgGlobalSetting.setId(1L);

                    boolean insertStatus = this.traceOrgService.insert(traceOrgGlobalSetting);

                    if(insertStatus)
                    {

                        TraceOrgCommonUtil.setLogLevel(traceOrgGlobalSetting.getLoggingLevel());

                        _logger.debug("Logging level "+traceOrgGlobalSetting.getLoggingLevel() +" is applied successfully..");

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.GLOBAL_SETTING_UPDATE_SUCCESS);
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
