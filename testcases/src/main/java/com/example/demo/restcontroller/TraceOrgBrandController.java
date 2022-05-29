package com.example.demo.restcontroller;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.Response;
import com.motadata.traceorg.ipam.model.TraceOrgBrand;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.TraceOrgCommonConstants;
import com.motadata.traceorg.ipam.util.TraceOrgCommonUtil;
import com.motadata.traceorg.ipam.util.TraceOrgMessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@SuppressWarnings("ALL")
@RestController
public class TraceOrgBrandController {

	@Autowired
	private TraceOrgService traceOrgService;
	
	@Autowired
	private TraceOrgCommonUtil traceOrgCommonUtil;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgBrandController.class, "Brand Controller");

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @RequestMapping(value = TraceOrgCommonConstants.BRAND_REST_URL+"{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateBrand(@PathVariable Long id, HttpServletRequest request, @RequestParam MultipartFile brandLogo, @RequestParam String productName)
    {
        Response response = new Response();

        try
        {
            if(id !=null && productName != null && !productName.trim().isEmpty() && brandLogo != null)
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        if(brandLogo.getOriginalFilename().toLowerCase().endsWith("jpg") || brandLogo.getOriginalFilename().toLowerCase().endsWith("jpeg") || brandLogo.getOriginalFilename().toLowerCase().endsWith("png"))
                        {
                            traceOrgCommonUtil.fileUpload(brandLogo,request);

                            TraceOrgBrand traceOrgBrand = (TraceOrgBrand)this.traceOrgService.getById(TraceOrgCommonConstants.TRACE_ORG_BRAND,1L);

                            traceOrgBrand.setProductName(productName);

                            traceOrgBrand.setProductImg(TraceOrgCommonConstants.LOGO_PNG);

                            boolean insertStatus = this.traceOrgService.insert(traceOrgBrand);

                            if(insertStatus)
                            {
                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                response.setMessage(TraceOrgMessageConstants.Brand_UPDATE_SUCCESS);
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

                            response.setMessage(TraceOrgMessageConstants.FILE_NOT_VALID);
                        }
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setMessage(TraceOrgMessageConstants.DO_NOT_HAVE_ACCESS);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                    }
                }
                else
                {
                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                    response.setMessage(TraceOrgMessageConstants.TOKEN_NOT_RECOGNISED);
                }
            }
            else
            {
                response.setSuccess(TraceOrgCommonConstants.FALSE);

                response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
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
