package com.example.demo.restcontroller;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.Response;
import com.motadata.traceorg.ipam.model.TraceOrgEvent;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetDetails;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetIpDetails;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.TraceOrgCommonConstants;
import com.motadata.traceorg.ipam.util.TraceOrgCommonUtil;
import com.motadata.traceorg.ipam.util.TraceOrgMessageConstants;
import com.motadata.traceorg.ipam.util.TraceOrgPDFBuilder;
import de.siegmar.fastcsv.reader.CsvContainer;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author Krunal Thakkar
 *
 */

@SuppressWarnings("ALL")
@RestController
public class TraceOrgSubnetIpController
{
    @Autowired
    private TraceOrgService traceOrgService;

    @Autowired
    private TraceOrgCommonUtil traceOrgCommonUtil;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgSubnetIpController.class, "Subnet IP Controller");

    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllSubnetIp(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetIpDetails> subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS);

                if(subnetIpDetailsList!=null && !subnetIpDetailsList.isEmpty())
                {
                    response.setData(subnetIpDetailsList);

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

    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_IP_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getSubnetIp(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    TraceOrgSubnetIpDetails subnetIpDetails = (TraceOrgSubnetIpDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS, id);

                    if (subnetIpDetails != null)
                    {

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setData(subnetIpDetails);
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ID_NOT_VALID);
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

    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_IP_REST_URL+"{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeSubnetIp(@PathVariable(TraceOrgCommonConstants.ID) String id, HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken) )
            {
                if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    if(id != null && !id.isEmpty())
                    {
                        if(id.contains(","))
                        {
                            String[] subnetIpIdString = id.split(",");

                            for (String aSubnetIpIdString : subnetIpIdString)
                            {
                                long subnetIPId = Long.parseLong(aSubnetIpIdString);

                                boolean deleteStatus = deactiveSubnetIp(subnetIPId);

                                if (deleteStatus)
                                {
                                    TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = (TraceOrgSubnetIpDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,subnetIPId);

                                    TraceOrgSubnetDetails traceOrgSubnetDetail = (TraceOrgSubnetDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,traceOrgSubnetIpDetails.getSubnetId().getId());

                                    List<TraceOrgSubnetIpDetails> totalSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+traceOrgSubnetDetail.getId()+"' and  deactiveStatus = false");

                                    List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE, TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId()))+" and  deactiveStatus = false");

                                    List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                                    List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and deactiveStatus = false");

                                    traceOrgSubnetDetail.setAvailableIp((long) availableSubnetIpDetailsList.size());

                                    traceOrgSubnetDetail.setUsedIp((long) usedSubnetIpDetailsList.size());

                                    traceOrgSubnetDetail.setTransientIp((long) transientSubnetIpDetailsList.size());

                                    traceOrgSubnetDetail.setTotalIp((long)totalSubnetIpDetailsList.size());

                                    traceOrgService.insert(traceOrgSubnetDetail);

                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SUBNET_IP_DELETE_SUCCESS);

                                    _logger.debug("subnet "+traceOrgSubnetDetail.getSubnetAddress()+" ip address deleted successfully..");
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
                                }
                            }
                        }
                        else
                        {
                            long subnetIPId = Long.parseLong(id);

                            boolean deleteStatus = deactiveSubnetIp(subnetIPId);

                            if(deleteStatus)
                            {
                                TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = (TraceOrgSubnetIpDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,subnetIPId);

                                TraceOrgSubnetDetails traceOrgSubnetDetail = (TraceOrgSubnetDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,traceOrgSubnetIpDetails.getSubnetId().getId());

                                List<TraceOrgSubnetIpDetails> totalSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+traceOrgSubnetDetail.getId()+"' and  deactiveStatus = false");

                                List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE, TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId()))+" and  deactiveStatus = false");

                                List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                                List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                                traceOrgSubnetDetail.setAvailableIp((long) availableSubnetIpDetailsList.size());

                                traceOrgSubnetDetail.setUsedIp((long) usedSubnetIpDetailsList.size());

                                traceOrgSubnetDetail.setTransientIp((long) transientSubnetIpDetailsList.size());

                                traceOrgSubnetDetail.setTotalIp((long)totalSubnetIpDetailsList.size());

                                traceOrgService.insert(traceOrgSubnetDetail);

                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                response.setMessage(TraceOrgMessageConstants.SUBNET_IP_DELETE_SUCCESS);

                                _logger.debug("subnet "+traceOrgSubnetDetail.getSubnetAddress()+" ip address deleted successfully..");
                            }
                            else
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
                            }
                        }
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

    private boolean deactiveSubnetIp(long subnetIPId)
    {
        boolean result = false;

        try
        {
            TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = (TraceOrgSubnetIpDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,subnetIPId);

            if (traceOrgSubnetIpDetails != null)
            {
                traceOrgSubnetIpDetails.setDeactiveStatus(true);

                traceOrgSubnetIpDetails.setModifiedDate(new Date());

                result = this.traceOrgService.insert(traceOrgSubnetIpDetails);
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return result;
    }

    //Available IP Address
    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_AVAILABLE_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllSubnetAvailableIp(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE));

                if(availableSubnetIpDetailsList!=null && !availableSubnetIpDetailsList.isEmpty())
                {
                    response.setData(availableSubnetIpDetailsList);

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

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    //Reserved IP Address
    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_RESERVED_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllSubnetReservedIp(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.RESERVED));

                if(availableSubnetIpDetailsList!=null && !availableSubnetIpDetailsList.isEmpty())
                {
                    response.setData(availableSubnetIpDetailsList);

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

            response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //Transient IP Address
    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_TRANSIENT_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllSubnetTransientIp(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT));

                if(transientSubnetIpDetailsList!=null && !transientSubnetIpDetailsList.isEmpty())
                {
                    response.setData(transientSubnetIpDetailsList);

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


    //USED IP Address
    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_USED_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllSubnetUsedIp(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED));

                if(usedSubnetIpDetailsList!=null && !usedSubnetIpDetailsList.isEmpty())
                {
                    response.setData(usedSubnetIpDetailsList);

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

    //IP ADDRESSES BY SUBNET
    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_IP_BY_SUBNET_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getSubnetIPBySubnetId(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    TraceOrgSubnetDetails traceOrgSubnetDetails = (TraceOrgSubnetDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS, id);

                    if(traceOrgSubnetDetails != null)
                    {
                        List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_BY_SUBNET_ID.replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetails.getId())));

                        if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                        {
                            traceOrgSubnetIpDetailsList.forEach(subnetIpDetails->{
                                subnetIpDetails.setSubnetName(subnetIpDetails.getSubnetId().getSubnetName());
                            });
                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setData(traceOrgSubnetIpDetailsList);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ID_NOT_VALID);
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


    @RequestMapping(value = TraceOrgCommonConstants.USED_SUBNET_IP_BY_SUBNET_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getUsedSubnetIPBySubnetId(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    TraceOrgSubnetDetails traceOrgSubnetDetails = (TraceOrgSubnetDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS, id);

                    if(traceOrgSubnetDetails != null)
                    {
                        List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+id+"' and status = 'USED'");

                        if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                        {
                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setData(traceOrgSubnetIpDetailsList);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ID_NOT_VALID);
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


    @RequestMapping(value = TraceOrgCommonConstants.AVAILABLE_SUBNET_IP_BY_SUBNET_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getAvailableSubnetIPBySubnetId(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    TraceOrgSubnetDetails traceOrgSubnetDetails = (TraceOrgSubnetDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS, id);

                    if(traceOrgSubnetDetails != null)
                    {
                        List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+id+"' and status = 'AVAILABLE'");

                        if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                        {
                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setData(traceOrgSubnetIpDetailsList);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ID_NOT_VALID);
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

    @RequestMapping(value = TraceOrgCommonConstants.RESERVED_SUBNET_IP_BY_SUBNET_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getReservedSubnetIPBySubnetId(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    TraceOrgSubnetDetails traceOrgSubnetDetails = (TraceOrgSubnetDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS, id);

                    if(traceOrgSubnetDetails != null)
                    {
                        List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+id+"' and status = 'RESERVED'");

                        if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                        {
                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setData(traceOrgSubnetIpDetailsList);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ID_NOT_VALID);
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

    @RequestMapping(value = TraceOrgCommonConstants.SUBNET_IP_CSV_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> importSubnetIpFromCSV(HttpServletRequest request, @RequestParam MultipartFile subnetIpCsv, @RequestParam Long subnetId)
    {
        Response response = new Response();

        if(subnetIpCsv !=null && !subnetIpCsv.isEmpty() && subnetId!=null && traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,"id",subnetId.toString()))
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        if(subnetIpCsv.getOriginalFilename().toLowerCase().endsWith("csv"))
                        {
                            boolean importStatus = traceOrgCommonUtil.importCSVFile(subnetIpCsv, request, TraceOrgCommonConstants.SUBNET_IP_DETAIL_CSV_NAME);

                            if (importStatus)
                            {
                                TraceOrgSubnetDetails traceOrgSubnetDetail = (TraceOrgSubnetDetails)traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,subnetId);

                                File importFile = new File(request.getRealPath(TraceOrgCommonConstants.SUBNET_IP_DETAIL_CSV_PATH));

                                CsvReader csvReader = new CsvReader();

                                CsvContainer csv = csvReader.read(importFile, StandardCharsets.UTF_8);

                                boolean validFileStatus = false;

                                for (CsvRow csvRow : csv.getRows())
                                {
                                    if (csvRow.getOriginalLineNumber() == 1)
                                    {
                                        validFileStatus = traceOrgCommonUtil.checkSubnetIPFileData(csvRow);
                                    }

                                    if(validFileStatus && csvRow.getOriginalLineNumber() > 1)
                                    {
                                        if(csvRow.getField(0) == null || csvRow.getField(0).isEmpty()  || csvRow.getField(2) == null || csvRow.getField(2).isEmpty())
                                        {
                                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                                            response.setMessage(TraceOrgMessageConstants.CSV_IMPORT_SUCCESS);
                                        }
                                        else
                                        {
                                            if(csvRow.getField(1) != null && !csvRow.getField(1).isEmpty() &&  !csvRow.getField(1).contains(":"))
                                            {
                                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                                response.setMessage(TraceOrgMessageConstants.CSV_IMPORT_SUCCESS);
                                            }
                                            else
                                            {
                                                TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = new TraceOrgSubnetIpDetails();

                                                if(csvRow.getField(0) != null && !csvRow.getField(0).isEmpty())
                                                {
                                                    traceOrgSubnetIpDetails.setIpAddress(csvRow.getField(0));
                                                }

                                                if(csvRow.getField(1) != null && !csvRow.getField(1).isEmpty())
                                                {
                                                    traceOrgSubnetIpDetails.setMacAddress(csvRow.getField(1));
                                                }

                                                if(csvRow.getField(2) != null && !csvRow.getField(2).isEmpty())
                                                {
                                                    String status = csvRow.getField(2);

                                                    switch(status.toUpperCase())
                                                    {
                                                        case "USED" :
                                                            traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.USED);
                                                            break;
                                                        case "TRANSIENT" :
                                                            traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.TRANSIENT);
                                                            break;
                                                        case "AVAILABLE" :
                                                            traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.AVAILABLE);
                                                            break;
                                                        case "RESERVED" :
                                                            traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.RESERVED);
                                                            break;
                                                        default:
                                                            traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.AVAILABLE);
                                                            break;
                                                    }
                                                }

                                                if(csvRow.getField(3) != null && !csvRow.getField(3).isEmpty())
                                                {
                                                    traceOrgSubnetIpDetails.setIpToDns(csvRow.getField(3));
                                                }

                                                if(csvRow.getField(4) != null && !csvRow.getField(4).isEmpty())
                                                {
                                                    traceOrgSubnetIpDetails.setDnsToIp(csvRow.getField(4));
                                                }

                                                if(csvRow.getField(5) != null && !csvRow.getField(5).isEmpty())
                                                {
                                                    traceOrgSubnetIpDetails.setDeviceType(csvRow.getField(5));
                                                }

                                                if(csvRow.getField(6) != null && !csvRow.getField(6).isEmpty())
                                                {
                                                    if(csvRow.getField(6).equalsIgnoreCase("Yes"))
                                                    {
                                                        traceOrgSubnetIpDetails.setRogueStatus(TraceOrgCommonConstants.TRUE);
                                                    }
                                                    else
                                                    {
                                                        traceOrgSubnetIpDetails.setRogueStatus(TraceOrgCommonConstants.FALSE);
                                                    }
                                                }

                                                if(!traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED) && traceOrgSubnetIpDetails.isRogueStatus())
                                                {
                                                    traceOrgSubnetIpDetails.setRogueStatus(TraceOrgCommonConstants.FALSE);
                                                }

                                                traceOrgSubnetIpDetails.setSubnetId(traceOrgSubnetDetail);

                                                boolean insertStatus = insertSubnetIpByIpAddress(traceOrgSubnetIpDetails);

                                                if(insertStatus)
                                                {
                                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                                    response.setMessage(TraceOrgMessageConstants.CSV_IMPORT_SUCCESS);
                                                }
                                                else
                                                {
                                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                                    response.setMessage(TraceOrgMessageConstants.SOMETHING_WENT_WRONG);
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                                        response.setMessage(TraceOrgMessageConstants.FILE_NOT_VALID);
                                    }
                                }

                                List<TraceOrgSubnetIpDetails> totalSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+traceOrgSubnetDetail.getId()+"' and  deactiveStatus = false");

                                List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE, TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId()))+" and  deactiveStatus = false");

                                List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                                List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and deactiveStatus = false");

                                traceOrgSubnetDetail.setAvailableIp((long) availableSubnetIpDetailsList.size());

                                traceOrgSubnetDetail.setUsedIp((long) usedSubnetIpDetailsList.size());

                                traceOrgSubnetDetail.setTransientIp((long) transientSubnetIpDetailsList.size());

                                traceOrgSubnetDetail.setTotalIp((long)totalSubnetIpDetailsList.size());

                                traceOrgSubnetDetail.setLastScanTime(new Date()); // IPAM-125 bug 1

                                traceOrgService.insert(traceOrgSubnetDetail);

                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                response.setMessage(TraceOrgMessageConstants.CSV_IMPORT_SUCCESS);
                            }
                            else
                            {
                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setMessage(TraceOrgMessageConstants.FILE_NOT_VALID);
                            }
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.FILE_NOT_VALID);
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

    private boolean insertSubnetIpByIpAddress(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails)
    {
        boolean result = false;

        try
        {
            if(this.traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,TraceOrgCommonConstants.IP_ADDRESS,traceOrgSubnetIpDetails.getIpAddress()))
            {
                List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,traceOrgSubnetIpDetails.getIpAddress())+ " and subnetId = '"+traceOrgSubnetIpDetails.getSubnetId().getId()+"'");

                if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                {
                    TraceOrgSubnetIpDetails traceOrgSubnetIpDetailsExisted =  traceOrgSubnetIpDetailsList.get(0);

                    traceOrgSubnetIpDetailsExisted.setMacAddress(traceOrgSubnetIpDetails.getMacAddress());

                    traceOrgSubnetIpDetailsExisted.setDescription(traceOrgSubnetIpDetails.getDescription());

                    traceOrgSubnetIpDetailsExisted.setDeviceType(traceOrgSubnetIpDetails.getDeviceType());

                    traceOrgSubnetIpDetailsExisted.setDnsStatus(traceOrgSubnetIpDetails.getDnsStatus());

                    traceOrgSubnetIpDetailsExisted.setHostName(traceOrgSubnetIpDetails.getHostName());

                    traceOrgSubnetIpDetailsExisted.setRogueStatus(traceOrgSubnetIpDetails.isRogueStatus());

                    traceOrgSubnetIpDetailsExisted.setIpToDns(traceOrgSubnetIpDetails.getIpToDns());

                    traceOrgSubnetIpDetailsExisted.setDnsToIp(traceOrgSubnetIpDetails.getDnsToIp());

                    traceOrgSubnetIpDetailsExisted.setDeactiveStatus(TraceOrgCommonConstants.FALSE);

                    traceOrgSubnetIpDetailsExisted.setPreviousStatus(traceOrgSubnetIpDetailsExisted.getStatus());

                    switch(traceOrgSubnetIpDetails.getStatus().toUpperCase())
                    {
                        case "AVAILABLE" :
                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.AVAILABLE);
                            break;
                        case "TRANSIENT" :
                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);
                            break;
                        case "USED" :
                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);
                            break;
                        case "RESERVED" :
                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.RESERVED);
                            break;
                        default:
                            traceOrgSubnetIpDetailsExisted.setStatus(null);
                            break;
                    }

                    traceOrgSubnetIpDetails.setModifiedDate(new Date());

                    traceOrgSubnetIpDetailsExisted.setDeactiveStatus(TraceOrgCommonConstants.FALSE);

                    if(traceOrgSubnetIpDetailsExisted.getStatus() !=null)
                    {
                        result = this.traceOrgService.insert(traceOrgSubnetIpDetailsExisted);
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }


    @RequestMapping(value = TraceOrgCommonConstants.ACTIVE_SUBNET_IP_RANGE_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> activeSubnetIpRange(HttpServletRequest request, @RequestParam String startIp , @RequestParam String endIp , @RequestParam Long subnetId)
    {
        Response response = new Response();

        if( startIp != null && !startIp.trim().isEmpty() && endIp != null && !endIp.trim().isEmpty() && subnetId != null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        TraceOrgSubnetDetails traceOrgSubnetDetail = (TraceOrgSubnetDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,subnetId);

                        if(traceOrgSubnetDetail != null)
                        {
                            if( this.traceOrgCommonUtil.isValidIp(traceOrgSubnetDetail,startIp) &&  this.traceOrgCommonUtil.isValidIp(traceOrgSubnetDetail,endIp))
                            {

                                if(Long.parseLong(startIp.substring(startIp.lastIndexOf(".")+ 1,startIp.length())) < Long.parseLong(endIp.substring(endIp.lastIndexOf(".")+1,endIp.length())))
                                {
                                    Long startIpId = 0L;

                                    Long endIpId = 0L;

                                    List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsStartList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,startIp) + " and subnetId = '"+subnetId+"'");

                                    if(traceOrgSubnetIpDetailsStartList != null && !traceOrgSubnetIpDetailsStartList.isEmpty())
                                    {
                                        startIpId = traceOrgSubnetIpDetailsStartList.get(0).getId();

                                    }
                                    List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsEndList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,endIp)+ " and subnetId = '"+subnetId+"'");

                                    if(traceOrgSubnetIpDetailsEndList !=null && !traceOrgSubnetIpDetailsEndList.isEmpty())
                                    {
                                        endIpId = traceOrgSubnetIpDetailsEndList.get(0).getId();
                                    }

                                    if(startIpId > 0  && endIpId > 0)
                                    {
                                        int count = 0 ;

                                        for(; startIpId <= endIpId ; startIpId++)
                                        {
                                            TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = (TraceOrgSubnetIpDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,startIpId);

                                            if(traceOrgSubnetIpDetails.isDeactiveStatus())
                                            {
                                                traceOrgSubnetIpDetails.setDeactiveStatus(TraceOrgCommonConstants.FALSE);

                                                traceOrgSubnetIpDetails.setModifiedDate(new Date());

                                                this.traceOrgService.insert(traceOrgSubnetIpDetails);
                                                count++;
                                            }
                                        }

                                        List<TraceOrgSubnetIpDetails> totalSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+traceOrgSubnetDetail.getId()+"' and  deactiveStatus = false");

                                        List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE, TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId()))+" and  deactiveStatus = false");

                                        List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                                        List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                                        traceOrgSubnetDetail.setAvailableIp((long) availableSubnetIpDetailsList.size());

                                        traceOrgSubnetDetail.setUsedIp((long) usedSubnetIpDetailsList.size());

                                        traceOrgSubnetDetail.setTransientIp((long) transientSubnetIpDetailsList.size());

                                        traceOrgSubnetDetail.setTotalIp((long)totalSubnetIpDetailsList.size());

                                        traceOrgSubnetDetail.setLastScanTime(new Date());

                                        traceOrgService.insert(traceOrgSubnetDetail);

                                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ADD_SUCCESS);
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


    @RequestMapping(value = TraceOrgCommonConstants.UPDATE_SUBNET_IP_RANGE_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> updateSubnetIpRange(HttpServletRequest request, @RequestParam String startIp , @RequestParam String endIp, @RequestParam String status, @RequestParam Long subnetId)
    {
        Response response = new Response();

        if(status != null && !status.isEmpty() && startIp != null && !startIp.isEmpty() && endIp != null && !endIp.isEmpty() && subnetId !=null && (status.equalsIgnoreCase("USED") || status.equalsIgnoreCase("Available") || status.equalsIgnoreCase("Reserved") || status.equalsIgnoreCase("Transient")))
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {

                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        TraceOrgSubnetDetails traceOrgSubnetDetail = (TraceOrgSubnetDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,subnetId);

                        if(traceOrgSubnetDetail != null)
                        {
                            if( this.traceOrgCommonUtil.isValidIp(traceOrgSubnetDetail,startIp) &&  this.traceOrgCommonUtil.isValidIp(traceOrgSubnetDetail,endIp))
                            {

                                if(Long.parseLong(startIp.substring(startIp.lastIndexOf(".")+ 1,startIp.length())) < Long.parseLong(endIp.substring(endIp.lastIndexOf(".")+1,endIp.length())))
                                {
                                    Long startIpId = 0L;

                                    Long endIpId = 0L;

                                    List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsStartList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,startIp)+ " and subnetId = '"+subnetId+"'");

                                    if(traceOrgSubnetIpDetailsStartList != null && !traceOrgSubnetIpDetailsStartList.isEmpty())
                                    {
                                        startIpId = traceOrgSubnetIpDetailsStartList.get(0).getId();

                                    }
                                    List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsEndList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,endIp) + " and subnetId = '"+subnetId+"'");

                                    if(traceOrgSubnetIpDetailsEndList !=null && !traceOrgSubnetIpDetailsEndList.isEmpty())
                                    {
                                        endIpId = traceOrgSubnetIpDetailsEndList.get(0).getId();
                                    }

                                    if(startIpId > 0  && endIpId > 0)
                                    {
                                        for(; startIpId <= endIpId ; startIpId++)
                                        {
                                            TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = (TraceOrgSubnetIpDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,startIpId);

                                            switch(status.toUpperCase())
                                            {
                                                case "USED" :
                                                    traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.USED);
                                                    break;
                                                case "TRANSIENT" :
                                                    traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.TRANSIENT);
                                                    break;
                                                case "AVAILABLE" :
                                                    traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.AVAILABLE);
                                                    break;
                                                case "RESERVED" :
                                                    traceOrgSubnetIpDetails.setStatus(TraceOrgCommonConstants.RESERVED);
                                                    break;
                                                default:
                                                    traceOrgSubnetIpDetails.setStatus(null);
                                                    break;
                                            }
                                            if(traceOrgSubnetIpDetails.getStatus() !=null)
                                            {
                                                traceOrgSubnetIpDetails.setModifiedDate(new Date());

                                                this.traceOrgService.insert(traceOrgSubnetIpDetails);
                                            }
                                        }

                                        List<TraceOrgSubnetIpDetails> totalSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+traceOrgSubnetDetail.getId()+"' and  deactiveStatus = false");

                                        List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE, TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId()))+" and deactiveStatus = false");

                                        List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and deactiveStatus = false");

                                        List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and deactiveStatus = false");

                                        traceOrgSubnetDetail.setAvailableIp((long) availableSubnetIpDetailsList.size());

                                        traceOrgSubnetDetail.setUsedIp((long) usedSubnetIpDetailsList.size());

                                        traceOrgSubnetDetail.setTransientIp((long) transientSubnetIpDetailsList.size());

                                        traceOrgSubnetDetail.setTotalIp((long)totalSubnetIpDetailsList.size());

                                        traceOrgService.insert(traceOrgSubnetDetail);

                                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_UPDATE_SUCCESS);
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


    @RequestMapping(value = TraceOrgCommonConstants.DELETE_SUBNET_IP_RANGE_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> deleteSubnetIpRange(HttpServletRequest request, @RequestParam String startIp , @RequestParam String endIp, @RequestParam Long subnetId)
    {
        Response response = new Response();

        if( startIp != null && !startIp.isEmpty() && endIp != null && !endIp.isEmpty() && subnetId !=null )
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        TraceOrgSubnetDetails traceOrgSubnetDetail = (TraceOrgSubnetDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,subnetId);

                        if(traceOrgSubnetDetail != null)
                        {
                            if( this.traceOrgCommonUtil.isValidIp(traceOrgSubnetDetail,startIp) &&  this.traceOrgCommonUtil.isValidIp(traceOrgSubnetDetail,endIp))
                            {

                                if(Long.parseLong(startIp.substring(startIp.lastIndexOf(".")+ 1,startIp.length())) < Long.parseLong(endIp.substring(endIp.lastIndexOf(".")+1,endIp.length())))
                                {
                                    Long startIpId = 0L;

                                    Long endIpId = 0L;

                                    List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsStartList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,startIp)+ " and subnetId = '"+subnetId+"'");

                                    if(traceOrgSubnetIpDetailsStartList != null && !traceOrgSubnetIpDetailsStartList.isEmpty())
                                    {
                                        startIpId = traceOrgSubnetIpDetailsStartList.get(0).getId();

                                    }
                                    List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsEndList = (List<TraceOrgSubnetIpDetails>)this.traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,endIp)+ " and subnetId = '"+subnetId+"'");

                                    if(traceOrgSubnetIpDetailsEndList !=null && !traceOrgSubnetIpDetailsEndList.isEmpty())
                                    {
                                        endIpId = traceOrgSubnetIpDetailsEndList.get(0).getId();
                                    }

                                    if(startIpId > 0  && endIpId > 0)
                                    {
                                        for(; startIpId <= endIpId ; startIpId++)
                                        {
                                            TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = (TraceOrgSubnetIpDetails)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,startIpId);

                                            traceOrgSubnetIpDetails.setDeactiveStatus(TraceOrgCommonConstants.TRUE);

                                            traceOrgSubnetIpDetails.setModifiedDate(new Date());

                                            this.traceOrgService.insert(traceOrgSubnetIpDetails);
                                        }

                                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                                        response.setMessage(TraceOrgMessageConstants.SUBNET_IP_DELETE_SUCCESS);
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

                                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
                            }

                            List<TraceOrgSubnetIpDetails> totalSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS +" where subnetId = '"+traceOrgSubnetDetail.getId()+"' and  deactiveStatus = false");

                            List<TraceOrgSubnetIpDetails> availableSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.AVAILABLE).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE, TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId()))+" and  deactiveStatus = false");

                            List<TraceOrgSubnetIpDetails> usedSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.USED).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                            List<TraceOrgSubnetIpDetails> transientSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) traceOrgService.commonQuery(TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_STATUS_AND_SUBNET_ID.replace(TraceOrgCommonConstants.STATUS_VALUE,TraceOrgCommonConstants.TRANSIENT).replace(TraceOrgCommonConstants.SUBNET_ID_VALUE,TraceOrgCommonUtil.getStringValue(traceOrgSubnetDetail.getId())) +" and  deactiveStatus = false");

                            traceOrgSubnetDetail.setAvailableIp((long) availableSubnetIpDetailsList.size());

                            traceOrgSubnetDetail.setUsedIp((long) usedSubnetIpDetailsList.size());

                            traceOrgSubnetDetail.setTransientIp((long) transientSubnetIpDetailsList.size());

                            traceOrgSubnetDetail.setTotalIp((long)totalSubnetIpDetailsList.size());

                            traceOrgService.insert(traceOrgSubnetDetail);
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

    @RequestMapping(value = TraceOrgCommonConstants.ROGUE_SUBNET_IP_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> rogueSubnetIp(@RequestParam String id, @RequestParam boolean status, HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken) )
            {
                if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                {
                    if(id != null && !id.isEmpty())
                    {
                        if(id.contains(","))
                        {
                            String[] subnetIpIdStrings = id.split(",");

                            boolean responseStatus = false;

                            for (String subnetIpIdString: subnetIpIdStrings)
                            {
                                long subnetIpId = Long.parseLong(subnetIpIdString);

                                boolean deleteStatus = rogueSubnetIp(subnetIpId,status,accessToken);

                                if (deleteStatus)
                                {
                                    responseStatus = TraceOrgCommonConstants.TRUE;
                                }
                            }

                            if(responseStatus)
                            {
                                if(status)
                                {
                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ROGUE_SUCCESS);
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SUBNET_IP_TRUST_SUCCESS);
                                }
                            }
                            else
                            {
                                if(status)
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.AVAILABLE_IP_NOT_ROGUE);
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.AVAILABLE_IP_NOT_TRUST);
                                }
                            }
                        }
                        else
                        {
                            long subnetIPId = Long.parseLong(id);

                            boolean deleteStatus = rogueSubnetIp(subnetIPId,status,accessToken);

                            if(deleteStatus)
                            {
                                if(status)
                                {
                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SUBNET_IP_ROGUE_SUCCESS);
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.SUBNET_IP_TRUST_SUCCESS);
                                }
                            }
                            else
                            {
                                if(status)
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.AVAILABLE_IP_NOT_ROGUE);
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                    response.setMessage(TraceOrgMessageConstants.AVAILABLE_IP_NOT_TRUST);
                                }
                            }
                        }
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

    @RequestMapping(value = TraceOrgCommonConstants.ROGUE_SUBNET_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> rogueIpSummary(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetDetails> traceOrgSubnetDetailsList = (List<TraceOrgSubnetDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS);

                List<TraceOrgSubnetIpDetails> subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where rogueStatus = true and deactiveStatus = false");

                List<TraceOrgSubnetIpDetails> subnetUsedIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where status = 'Used' and deactiveStatus = false");

                Long rogueSubnetIpCount = 0L;

                if(subnetIpDetailsList!=null && !traceOrgSubnetDetailsList.isEmpty() && traceOrgSubnetDetailsList != null)
                {
                    HashMap<String,String> rogueIpSummary = new HashMap<>();

                    rogueIpSummary.put("totalIp",""+subnetUsedIpDetailsList.size());

                    rogueIpSummary.put("rogueIp",""+subnetIpDetailsList.size());

                    response.setData(rogueIpSummary);

                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                }
                else
                {
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


    private boolean rogueSubnetIp(long subnetIPId,boolean rogueStatus,String accessToken)
    {
        boolean result = false;

        try
        {
            TraceOrgSubnetIpDetails traceOrgSubnetIpDetails = (TraceOrgSubnetIpDetails) this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,subnetIPId);

            if (traceOrgSubnetIpDetails != null && traceOrgSubnetIpDetails.getStatus().equalsIgnoreCase("USED"))
            {
                traceOrgSubnetIpDetails.setRogueStatus(rogueStatus);

                traceOrgSubnetIpDetails.setModifiedDate(new Date());

                result = this.traceOrgService.insert(traceOrgSubnetIpDetails);

                //EVENT LOG
                TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                traceOrgEvent.setTimestamp(new Date());

                traceOrgEvent.setDoneBy(traceOrgCommonUtil.currentUser(accessToken));

                if(rogueStatus)
                {
                    traceOrgEvent.setEventType("IP Mark As Rogue");

                    traceOrgEvent.setEventContext("IP "+traceOrgSubnetIpDetails.getIpAddress()+" Mark as Rogue in IP Address Manager by "+traceOrgCommonUtil.currentUserName(accessToken));
                }
                else
                {
                    traceOrgEvent.setEventType("IP Mark As Trusted");

                    traceOrgEvent.setEventContext("IP "+traceOrgSubnetIpDetails.getIpAddress()+" Mark as Trusted in IP Address Manager by "+traceOrgCommonUtil.currentUserName(accessToken));
                }

                traceOrgEvent.setSeverity(1);

                traceOrgService.insert(traceOrgEvent);

            }

        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }
        return result;
    }

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_CSV_SUBNET_IP_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> exportCsvSubnetIp(@PathVariable(TraceOrgCommonConstants.ID) String id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id != null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    String[] subnetIpIdString = id.split(",");

                    if(this.traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,"id",subnetIpIdString[0]))
                    {
                        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = this.traceOrgCommonUtil.getSubnetIpDetailsList(subnetIpIdString);

                        if (subnetIpDetailsList != null && !subnetIpDetailsList.isEmpty())
                        {
                            String url = traceOrgCommonUtil.exportSubnetIpCsv(request,subnetIpDetailsList);

                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                            response.setData(url);
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);
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

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_PDF_SUBNET_IP_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> exportPdfSubnetIp(@PathVariable(TraceOrgCommonConstants.ID) String id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id != null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    String[] subnetIpIdString = id.split(",");

                    if(this.traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_SUBNET_DETAILS,"id",subnetIpIdString[0]))
                    {
                        List<TraceOrgSubnetIpDetails> subnetIpDetailsList = this.traceOrgCommonUtil.getSubnetIpDetailsList(subnetIpIdString);

                        if (subnetIpDetailsList != null && !subnetIpDetailsList.isEmpty())
                        {
                            try
                            {
                                LinkedHashSet<String> columns = new LinkedHashSet<String>()
                                {{
                                    add("IP Address");

                                    add("Status");

                                    add("Scope");

                                    add("Mac Address");

                                    add("Vendor");

                                    add("IP To DNS");

                                    add("DNS To IP");

                                    add("Rogue");

                                    add("Last Alive Time");

                                }};

                                List<HashMap<String, Object>> ipSummaryObject = new ArrayList<>();

                                Integer availableIp = 0;

                                Integer usedIp = 0;

                                Integer transientIp = 0;

                                List<Object> pdfResults = new ArrayList<>();

                                List<Object> pdfResult;

                                String subnetAddress = null;

                                for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList)
                                {
                                    if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.AVAILABLE))
                                    {
                                        availableIp++;
                                    }
                                    else if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.USED))
                                    {
                                        usedIp++;
                                    }
                                    else if(traceOrgSubnetIpDetail.getStatus().equalsIgnoreCase(TraceOrgCommonConstants.TRANSIENT))
                                    {
                                        transientIp++;
                                    }

                                    pdfResult = new ArrayList<>();

                                    subnetAddress = traceOrgSubnetIpDetail.getSubnetId().getSubnetAddress();

                                    pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                                    pdfResult.add(traceOrgSubnetIpDetail.getStatus());

                                    pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                                    pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                                    pdfResult.add(traceOrgSubnetIpDetail.getDeviceType());

                                    pdfResult.add(traceOrgSubnetIpDetail.getIpToDns());

                                    pdfResult.add(traceOrgSubnetIpDetail.getDnsToIp());

                                    if(traceOrgSubnetIpDetail.isRogueStatus())
                                        pdfResult.add("Yes");
                                    else
                                        pdfResult.add("No");

                                    pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                                    pdfResults.add(pdfResult);
                                }

                                HashMap<String, Object> results = new HashMap<>();

                                HashMap<String, Object> availableIpSummary = new HashMap<>();

                                availableIpSummary.put("status","Available (%)");

                                availableIpSummary.put("value",new DecimalFormat("#.00").format((double)(availableIp*100)/subnetIpDetailsList.size()));

                                HashMap<String, Object> usedIpSummary = new HashMap<>();

                                usedIpSummary.put("status","Used (%)");

                                usedIpSummary.put("value",new DecimalFormat("#.00").format((double)(usedIp*100)/subnetIpDetailsList.size()));

                                HashMap<String, Object> transientIpSummary = new HashMap<>();

                                transientIpSummary.put("status","Transient (%)");

                                transientIpSummary.put("value",new DecimalFormat("#.00").format((double)(transientIp*100)/subnetIpDetailsList.size()));

                                ipSummaryObject.add(availableIpSummary);

                                ipSummaryObject.add(usedIpSummary);

                                ipSummaryObject.add(transientIpSummary);

                                results.put("ipSummary", ipSummaryObject);

                                results.put("grid-result", pdfResults);

                                results.put("columns", columns);

                                results.put("logFor", "IP_REPORT");

                                List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                                visualizationResults.add(results);

                                HashMap<String, Object> gridReport = new HashMap<>();

                                gridReport.put("Title", "Subnet IP Details "+TraceOrgCommonConstants.LEFT_SQUARE_BRACKET + subnetAddress + TraceOrgCommonConstants.RIGHT_SQUARE_BRACKET);

                                String fileName = "Subnet IP Details "+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".pdf";

                                fileName = fileName.replace(" ","_").replace(":","_").replace(",","");

                                TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);

                                response.setData(fileName);

                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
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

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                            response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);
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

    @RequestMapping(value = TraceOrgCommonConstants.EXPORT_PDF_SUBNET_CONFLICT_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> exportPdfSubnetConflictIp(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetIpDetails> subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where conflictMac is not null and macAddress is not null");

                if (subnetIpDetailsList != null && !subnetIpDetailsList.isEmpty()) {

                    try
                    {
                        LinkedHashSet<String> columns = new LinkedHashSet<String>()
                        {{
                            add("IP Address");

                            add("Subnet");

                            add("Subnet Category");

                            add("Assigned MAC");

                            add("Conflicting MAC");

                            add("Conflict Time");

                        }};

                        List<Object> pdfResults = new ArrayList<>();

                        List<Object> pdfResult;

                        String subnetAddress = null;

                        for (TraceOrgSubnetIpDetails traceOrgSubnetIpDetail : subnetIpDetailsList) {
                            pdfResult = new ArrayList<>();

                            subnetAddress = traceOrgSubnetIpDetail.getSubnetId().getSubnetAddress();

                            pdfResult.add(traceOrgSubnetIpDetail.getIpAddress());

                            pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getSubnetName());

                            pdfResult.add(traceOrgSubnetIpDetail.getSubnetId().getTraceOrgCategory().getCategoryName());

                            pdfResult.add(traceOrgSubnetIpDetail.getMacAddress());

                            pdfResult.add(traceOrgSubnetIpDetail.getConflictMac());

                            pdfResult.add(traceOrgSubnetIpDetail.getLastAliveTime());

                            pdfResults.add(pdfResult);
                        }

                        HashMap<String, Object> results = new HashMap<>();

                        results.put("grid-result", pdfResults);

                        results.put("columns", columns);

                        List<HashMap<String, Object>> visualizationResults = new ArrayList<>();

                        visualizationResults.add(results);

                        HashMap<String, Object> gridReport = new HashMap<>();

                        gridReport.put("Title", "Subnet Conflict IP Details");

                        String fileName = "Subnet Conflict IP Details "+TraceOrgCommonConstants.VISUAL_DATE_FORMAT.format(new Date())+".pdf";

                        fileName = fileName.replace(" ","_").replace(":","_").replace(",","");

                        TraceOrgPDFBuilder.addGridReport(1, visualizationResults, new HashMap<String, Object>(), fileName, gridReport);

                        response.setData(fileName);

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
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

                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                    response.setMessage(TraceOrgMessageConstants.NO_DATA_AVAILABLE);
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

    @RequestMapping(value = TraceOrgCommonConstants.CONFLICT_SUBNET_IP_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllConflictSubnetIp(HttpServletRequest request)
    {
        Response response = new Response();

        try
        {
            String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if(traceOrgCommonUtil.checkToken(accessToken))
            {
                List<TraceOrgSubnetIpDetails> subnetIpDetailsList = (List<TraceOrgSubnetIpDetails>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS + " where conflictMac is not null and macAddress is not null");

                if(subnetIpDetailsList!=null && !subnetIpDetailsList.isEmpty())
                {
                    response.setData(subnetIpDetailsList);

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
}
