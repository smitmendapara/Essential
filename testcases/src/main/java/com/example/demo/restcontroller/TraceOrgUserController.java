package com.example.demo.restcontroller;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.Response;
import com.motadata.traceorg.ipam.model.TraceOrgEvent;
import com.motadata.traceorg.ipam.model.User;
import com.motadata.traceorg.ipam.model.UserRole;
import com.motadata.traceorg.ipam.services.TraceOrgService;
import com.motadata.traceorg.ipam.util.TraceOrgCommonConstants;
import com.motadata.traceorg.ipam.util.TraceOrgCommonUtil;
import com.motadata.traceorg.ipam.util.TraceOrgMessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"ALL"})
@RestController
public class TraceOrgUserController {
	
	@Autowired
	private TraceOrgService traceOrgService;
	
	@Autowired
	private TraceOrgCommonUtil traceOrgCommonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgUserController.class, "User Controller");

	@RequestMapping(value = TraceOrgCommonConstants.USER_REST_URL, method = RequestMethod.GET)
    public ResponseEntity<?> listAllUsers(HttpServletRequest request)
    {
		Response response = new Response();

		try
		{
			String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

			if(traceOrgCommonUtil.checkToken(accessToken))
			{
				List<User> userList = (List<User>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.USER);

				for(User user : userList)
                {
                    if(user.isStatus())
                    {
                        user.setActiveStatus("Enable");
                    }
                    else
                    {
                        user.setActiveStatus("Disable");
                    }
                }

				response.setData(userList);

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
	
	
	@RequestMapping(value = TraceOrgCommonConstants.USER_REST_URL, method = RequestMethod.POST)
    public ResponseEntity<?> insertUser(HttpServletRequest request, @RequestBody User user)
    {
		Response response = new Response();

		try
        {
			String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

            if (traceOrgCommonUtil.checkToken(accessToken))
            {
				if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
				{
					if(user.getUserName() != null && !user.getUserName().trim().isEmpty() && user.getPassword()!=null && !user.getPassword().trim().isEmpty() && user.getEmail() !=null && !user.getEmail().trim().isEmpty())
                    {
                        if (this.traceOrgService.isExist(TraceOrgCommonConstants.USER, TraceOrgCommonConstants.USER_NAME,user.getUserName()))
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.USER_NAME_ALREADY_EXIST);

                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                            _logger.debug("user "+user.getUserName() + " is already exist");
                        }
                        else
                        {
                            if(user.getRoleId()!=null)
                            {
                                UserRole userRole = (UserRole) traceOrgService.getById(TraceOrgCommonConstants.USERROLE, user.getRoleId());

                                user.setUserRoleId(userRole);

                            }
                            user.setPassword(passwordEncoder.encode(URLEncoder.encode(user.getPassword())));

                            if(user.getActiveStatus() != null && !user.getActiveStatus().isEmpty())
                            {
                                user.setStatus(user.getActiveStatus().equalsIgnoreCase("Enable"));
                            }

                            boolean insertStatus = this.traceOrgService.insert(user);

                            _logger.debug("user "+user.getUserName()+" insert status is "+insertStatus);

                            if(insertStatus)
                            {
                                //EVENT LOG

                                TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                                traceOrgEvent.setTimestamp(new Date());

                                traceOrgEvent.setDoneBy(traceOrgCommonUtil.currentUser(accessToken));

                                traceOrgEvent.setEventType("Add USER");

                                traceOrgEvent.setEventContext("User "+user.getUserName()+" added in IP Address Manager by "+traceOrgCommonUtil.currentUserName(accessToken)  );

                                traceOrgEvent.setSeverity(2);

                                this.traceOrgService.insert(traceOrgEvent);

                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                response.setMessage(TraceOrgMessageConstants.USER_ADD_SUCCESS);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                                _logger.debug("User "+user.getUserName()+" added successfully");
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
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
	
	
	@RequestMapping(value = TraceOrgCommonConstants.USER_REST_URL+"{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getUser(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
        Response response = new Response();

        if(id != null )
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if(traceOrgCommonUtil.checkToken(accessToken))
                {
                    User user = (User) this.traceOrgService.getById(TraceOrgCommonConstants.USER, id);

                    if (user != null)
                    {

                        user.setRoleId(user.getUserRoleId().getId());

                        response.setSuccess(TraceOrgCommonConstants.TRUE);

                        response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));

                        if(user.isStatus())
                        {
                            user.setActiveStatus("Enable");
                        }
                        else
                        {
                            user.setActiveStatus("Disable");
                        }
                        response.setData(user);
                    }
                    else
                    {
                        response.setSuccess(TraceOrgCommonConstants.FALSE);

                        response.setMessage(TraceOrgMessageConstants.USER_ID_WRONG);

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

	@RequestMapping(value = TraceOrgCommonConstants.USER_REST_URL+"{id}", method = RequestMethod.PUT)
	public ResponseEntity<?> updateUser(@PathVariable Long id, HttpServletRequest request, @RequestBody User user)
	{
        Response response = new Response();

		try
		{
			String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

			if(traceOrgCommonUtil.checkToken(accessToken))
			{
				User currentUser = (User)this.traceOrgService.getById(TraceOrgCommonConstants.USER,id);

				if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
				{
                    if(id !=null && user.getUserName() != null && !user.getUserName().trim().isEmpty() && user.getEmail() !=null && !user.getEmail().trim().isEmpty())
                    {
                        if (currentUser != null)
                        {
                            List<User> userList =  (List<User>)this.traceOrgService.commonQuery("", TraceOrgCommonConstants.USER_BY_USERNAME.replace(TraceOrgCommonConstants.USER_NAME_VALUE,user.getUserName()));

                            if (userList != null && !userList.isEmpty() && userList.size() < 2)
                            {
                                if(userList.get(0).getId().equals(id))
                                {
                                    User loginUser = this.traceOrgService.findByUserName(traceOrgCommonUtil.currentUserName(accessToken));

                                    if(!loginUser.getId().equals(currentUser.getId()))
                                    {
                                        if(user.getActiveStatus()!=null && !user.getActiveStatus().isEmpty())
                                        {
                                            currentUser.setStatus(user.getActiveStatus().equals("Enable"));
                                        }
                                        else
                                        {
                                            currentUser.setStatus(user.isStatus());
                                        }

                                        if(user.getRoleId()!=null)
                                        {
                                            UserRole userRole = (UserRole) traceOrgService.getById(TraceOrgCommonConstants.USERROLE, user.getRoleId());

                                            currentUser.setUserRoleId(userRole);
                                        }

                                        currentUser.setEmail(user.getEmail());

                                        currentUser.setUserName(user.getUserName());

                                        currentUser.setDescription(user.getDescription());

                                        if(user.getActiveStatus()!=null && !user.getActiveStatus().isEmpty())
                                        {
                                            currentUser.setStatus(user.getActiveStatus().equals("Enable"));
                                        }
                                        else
                                        {
                                            currentUser.setStatus(user.isStatus());
                                        }

                                        boolean insertStatus = this.traceOrgService.insert(currentUser);

                                        _logger.debug("user "+user.getUserName() + " update status is "+insertStatus);

                                        if(insertStatus)
                                        {
                                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                                            response.setMessage(TraceOrgMessageConstants.USER_UPDATE_SUCCESS);

                                            _logger.debug("User "+user.getUserName()+" updated successfully");

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
                                        if(user.getActiveStatus()!=null && !user.getActiveStatus().isEmpty())
                                        {
                                            currentUser.setStatus(user.getActiveStatus().equals("Enable"));
                                        }
                                        else
                                        {
                                            currentUser.setStatus(user.isStatus());
                                        }


                                        if(currentUser.isStatus())
                                        {
                                            UserRole userRole = (UserRole) traceOrgService.getById(TraceOrgCommonConstants.USERROLE, user.getRoleId());

                                            currentUser.setUserRoleId(userRole);

                                            currentUser.setEmail(user.getEmail());

                                            currentUser.setUserName(user.getUserName());

                                            currentUser.setDescription(user.getDescription());

                                            if(user.getActiveStatus()!=null && !user.getActiveStatus().isEmpty())
                                            {
                                                currentUser.setStatus(user.getActiveStatus().equals("Enable"));
                                            }
                                            else
                                            {
                                                currentUser.setStatus(user.isStatus());
                                            }

                                            boolean insertStatus = this.traceOrgService.insert(currentUser);

                                            _logger.debug("user "+user.getUserName() + " update status is "+insertStatus);

                                            if(insertStatus)
                                            {
                                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                                response.setMessage(TraceOrgMessageConstants.USER_UPDATE_SUCCESS);

                                                _logger.debug("User "+user.getUserName()+" updated successfully");

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

                                            response.setMessage(TraceOrgMessageConstants.USER_CAN_NOT_DISABLE_OWN);

                                            response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                                        }
                                    }
                                }
                                else
                                {
                                    response.setSuccess(TraceOrgCommonConstants.FALSE);

                                    response.setMessage(TraceOrgMessageConstants.USER_NAME_ALREADY_EXIST);

                                    response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                                }
                            }
                            else
                            {
                                UserRole userRole = (UserRole) traceOrgService.getById(TraceOrgCommonConstants.USERROLE, user.getRoleId());

                                currentUser.setUserRoleId(userRole);

                                currentUser.setEmail(user.getEmail());

                                currentUser.setUserName(user.getUserName());

                                currentUser.setDescription(user.getDescription());

                                if(user.getActiveStatus()!=null && !user.getActiveStatus().isEmpty())
                                {
                                    currentUser.setStatus(user.getActiveStatus().equals("Enable"));
                                }
                                else
                                {
                                    currentUser.setStatus(user.isStatus());
                                }

                                this.traceOrgService.insert(currentUser);

                                response.setSuccess(TraceOrgCommonConstants.TRUE);

                                response.setMessage(TraceOrgMessageConstants.USER_UPDATE_SUCCESS);

                                _logger.debug("User "+user.getUserName()+" updated successfully");

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                            }
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.USER_ID_WRONG);
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
	
	@RequestMapping(value = TraceOrgCommonConstants.USER_REST_URL+"{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeUser(@PathVariable(TraceOrgCommonConstants.ID) Long id, HttpServletRequest request)
    {
		Response response = new Response();

		if(id !=null)
        {
            try
            {
                String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

                if (traceOrgCommonUtil.checkToken(accessToken))
                {
                    User user = (User) this.traceOrgService.getById(TraceOrgCommonConstants.USER, id);

                    User currentUser = this.traceOrgService.findByUserName(traceOrgCommonUtil.currentUserName(accessToken));

                    if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
                    {
                        String userName = user.getUserName();

                        if(!currentUser.getId().equals(id))
                        {
                            if (user != null)
                            {

                                this.traceOrgService.delete(TraceOrgCommonConstants.TRACE_ORG_FORGOT_PASSWORD, "user", TraceOrgCommonUtil.getStringValue(id));

                                this.traceOrgService.delete(TraceOrgCommonConstants.TRACE_ORG_EVENT, "doneBy", TraceOrgCommonUtil.getStringValue(id));

                                boolean deleteStatus = this.traceOrgService.delete(TraceOrgCommonConstants.USER, TraceOrgCommonConstants.ID, TraceOrgCommonUtil.getStringValue(id));

                                if(deleteStatus)
                                {
                                    //EVENT LOG
                                    TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                                    traceOrgEvent.setTimestamp(new Date());

                                    traceOrgEvent.setDoneBy(traceOrgCommonUtil.currentUser(accessToken));

                                    traceOrgEvent.setEventType("Delete USER");

                                    traceOrgEvent.setEventContext("User "+userName+" deleted in IP Address Manager by "+traceOrgCommonUtil.currentUserName(accessToken));

                                    _logger.debug("User "+user.getUserName()+" deleted successfully");

                                    traceOrgEvent.setSeverity(2);

                                    this.traceOrgService.insert(traceOrgEvent);

                                    response.setSuccess(TraceOrgCommonConstants.TRUE);

                                    response.setMessage(TraceOrgMessageConstants.USER_DELETE_SUCCESS);

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
                                _logger.debug("User "+user.getUserName()+" is not deleted: user is null");

                                response.setSuccess(TraceOrgCommonConstants.FALSE);

                                response.setMessage(TraceOrgMessageConstants.USER_ID_WRONG);

                                response.setCurrentUserRole(traceOrgCommonUtil.currentUserRole(accessToken));
                            }
                        }
                        else
                        {
                            response.setSuccess(TraceOrgCommonConstants.FALSE);

                            response.setMessage(TraceOrgMessageConstants.USER_CAN_NOT_DELETE_OWN);

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

    //GET ALL UserRole
	@SuppressWarnings("unchecked")
	@RequestMapping(value = TraceOrgCommonConstants.USER_ROLE_REST_URL, method = RequestMethod.GET)
	public ResponseEntity<?> listAllUserRoles(HttpServletRequest request)
	{
		Response response = new Response();

		try
		{
			String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

			if(traceOrgCommonUtil.checkToken(accessToken))
			{
				List<UserRole> userRoleList = (List<UserRole>) this.traceOrgService.commonQuery("",TraceOrgCommonConstants.USERROLE);

				if(userRoleList != null)
				{
					response.setData(userRoleList);

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

	@RequestMapping(value = "/changePassword/{id}", method = RequestMethod.PUT)
	public ResponseEntity<?> changePassword(@PathVariable Long id, HttpServletRequest request, @RequestBody User user)
	{
		Response response = new Response();

		try
        {
			String accessToken = request.getHeader(TraceOrgCommonConstants.ACCESSTOKEN);

			if (traceOrgCommonUtil.checkToken(accessToken))
			{
				if(traceOrgCommonUtil.currentUserRole(accessToken).equals(TraceOrgCommonConstants.ROLE_ADMIN))
				{
				    if(id!=null && user.getPassword()!=null && !user.getPassword().trim().isEmpty() )
                    {
                        User currentUser = (User)this.traceOrgService.getById(TraceOrgCommonConstants.USER,id);

                        currentUser.setPassword(passwordEncoder.encode(URLEncoder.encode(user.getPassword())));

                        boolean updateStatus = this.traceOrgService.insert(currentUser);

                        _logger.debug("user "+user.getUserName() + " change password status is "+updateStatus);

                        if(updateStatus)
                        {
                            response.setSuccess(TraceOrgCommonConstants.TRUE);

                            response.setMessage(TraceOrgMessageConstants.USER_PASSWORD_UPDATE_SUCCESS);
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
		catch (Exception exception)
		{
			_logger.error(exception);

			response.setSuccess(TraceOrgCommonConstants.FALSE);

			response.setMessage(TraceOrgMessageConstants.ENTER_VALID_DETAILS);
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}