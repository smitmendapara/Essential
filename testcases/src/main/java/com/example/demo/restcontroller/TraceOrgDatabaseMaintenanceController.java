package com.example.demo.restcontroller;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.Response;
import com.motadata.traceorg.ipam.model.TraceOrgDatabaseMaintenance;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.TraceOrgCommonConstants;
import com.motadata.traceorg.ipam.util.TraceOrgCommonUtil;
import com.motadata.traceorg.ipam.util.TraceOrgMessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("ALL")
@RestController
public class TraceOrgDatabaseMaintenanceController
{

    @Autowired
    private TraceOrgService traceOrgService;

    @Autowired
    private TraceOrgCommonUtil traceOrgCommonUtil;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgDatabaseMaintenanceController.class, "Database Maintenance Controller");

    @RequestMapping(value = TraceOrgCommonConstants.DATABASE_MAINTENANCE+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getDatabaseMaintenanceDetail(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken) && traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    TraceOrgDatabaseMaintenance traceOrgDatabaseMaintenance = (TraceOrgDatabaseMaintenance) this.traceOrgService.getById("TraceOrgDatabaseMaintenance", id);

                    if (traceOrgDatabaseMaintenance != null)
                    {
                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setData(traceOrgDatabaseMaintenance);
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
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

    @RequestMapping(value = TraceOrgCommonConstants.DATABASE_MAINTENANCE+"{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateDatabaseMaintenanceDetail(@PathVariable Long id, HttpServletRequest request, @RequestBody TraceOrgDatabaseMaintenance traceOrgDatabaseMaintenance)
    {
        Response response = new Response();

        if(id !=null && traceOrgDatabaseMaintenance.getMaintainedDays() !=null && traceOrgDatabaseMaintenance.getMaintainedDays() > 0 )
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken) && traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    traceOrgDatabaseMaintenance.setId(id);

                    boolean insertStatus = this.traceOrgService.insert(traceOrgDatabaseMaintenance);

                    if(insertStatus)
                    {
                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.DATABASE_MAINTENANCE_UPDATE_SUCCESS);
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


    @RequestMapping(value = TraceOrgCommonConstants.DATABASE_MAINTENANCE+"{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> runDatabaseMaintenanceDetail(@PathVariable Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null && id == 1)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken) && traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    TraceOrgDatabaseMaintenance traceOrgDatabaseMaintenance =  (TraceOrgDatabaseMaintenance)this.traceOrgService.getById("TraceOrgDatabaseMaintenance",id);

                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                    Date dataFrom = new Date();

                    String currentDate = dateFormat.format(dataFrom);

                    this.traceOrgService.sqlQueryAction("SET SQL_SAFE_UPDATES = 0");

                    this.traceOrgService.sqlQueryAction("Delete from event where DATEDIFF('"+currentDate+"',timestamp) > "+ traceOrgDatabaseMaintenance.getMaintainedDays()+"");

                    this.traceOrgService.sqlQueryAction("SET SQL_SAFE_UPDATES = 1");

                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                    response.setMessage("Data Retention Applied Successfully");

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
