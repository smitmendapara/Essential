package com.example.demo.util;

import com.motadata.traceorg.ipam.logger.TraceOrgLogger;
import com.motadata.traceorg.ipam.model.TraceOrgEvent;
import com.motadata.traceorg.ipam.model.TraceOrgSubnetIpDetails;
import com.motadata.traceorg.ipam.services.TraceOrgService;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

public class TraceOrgSubnetIpDetailsUpdate implements Callable<Boolean>
{
    private static final TraceOrgLogger _logger = new TraceOrgLogger(TraceOrgSubNetScanner.class, "GUI / Subnet IP Details");

    TraceOrgSubnetIpDetails traceOrgSubnetIpDetails;

    TraceOrgService traceOrgService;

    public TraceOrgSubnetIpDetailsUpdate(TraceOrgSubnetIpDetails traceOrgSubnetIpDetails, TraceOrgService traceOrgService)
    {
        this.traceOrgSubnetIpDetails = traceOrgSubnetIpDetails;

        this.traceOrgService = traceOrgService;
    }

    @Override
    public Boolean call() throws Exception
    {
        try
        {
            if(traceOrgService.isExist(TraceOrgCommonConstants.TRACE_ORG_SUBNET_IP_DETAILS,TraceOrgCommonConstants.SUBNET_ID,traceOrgSubnetIpDetails.getSubnetId().getId().toString()))
            {
                List<TraceOrgSubnetIpDetails> traceOrgSubnetIpDetailsList = (List<TraceOrgSubnetIpDetails>)traceOrgService.commonQuery("",TraceOrgCommonConstants.SUBNET_IP_DETAILS_BY_IP_ADDRESS.replace(TraceOrgCommonConstants.IP_ADDRESS_VALUE,traceOrgSubnetIpDetails.getIpAddress()) + " and subnetId = '"+traceOrgSubnetIpDetails.getSubnetId().getId()+"'");

                if(traceOrgSubnetIpDetailsList != null && !traceOrgSubnetIpDetailsList.isEmpty())
                {
                    TraceOrgSubnetIpDetails traceOrgSubnetIpDetailsExisted =  traceOrgSubnetIpDetailsList.get(0);

                    traceOrgSubnetIpDetailsExisted.setMacAddress(traceOrgSubnetIpDetails.getMacAddress());

                    if(traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED) && traceOrgSubnetIpDetailsExisted.getMacAddress()!=null && traceOrgSubnetIpDetails.getMacAddress()!=null)
                    {
                        if(traceOrgSubnetIpDetailsExisted.getPreviousMacAddress()!=null && !traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().isEmpty())
                        {
                            if(!traceOrgSubnetIpDetailsExisted.getPreviousMacAddress().equalsIgnoreCase(traceOrgSubnetIpDetailsExisted.getMacAddress()))
                            {
                                traceOrgSubnetIpDetailsExisted.setConflictMac(traceOrgSubnetIpDetailsExisted.getPreviousMacAddress());
                            }
                            else
                            {
                                traceOrgSubnetIpDetailsExisted.setConflictMac(null);
                            }
                            traceOrgSubnetIpDetailsExisted.setPreviousMacAddress(traceOrgSubnetIpDetailsExisted.getMacAddress());
                        }
                        else
                        {
                            traceOrgSubnetIpDetailsExisted.setPreviousMacAddress(traceOrgSubnetIpDetails.getMacAddress());
                        }
                    }

                    traceOrgSubnetIpDetailsExisted.setDescription(traceOrgSubnetIpDetails.getDescription());

                    traceOrgSubnetIpDetailsExisted.setDeviceType(traceOrgSubnetIpDetails.getDeviceType());

                    traceOrgSubnetIpDetailsExisted.setDnsStatus(traceOrgSubnetIpDetails.getDnsStatus());

                    traceOrgSubnetIpDetailsExisted.setHostName(traceOrgSubnetIpDetails.getHostName());

                    if(traceOrgSubnetIpDetails.getLastAliveTime()!=null)
                    {
                        traceOrgSubnetIpDetailsExisted.setLastAliveTime(new Date(traceOrgSubnetIpDetails.getLastAliveTime()));
                    }


                    traceOrgSubnetIpDetailsExisted.setIpToDns(traceOrgSubnetIpDetails.getIpToDns());

                    traceOrgSubnetIpDetailsExisted.setDnsToIp(traceOrgSubnetIpDetails.getDnsToIp());

                    //traceOrgSubnetIpDetailsExisted.setDeactiveStatus(traceOrgSubnetIpDetails.isDeactiveStatus());

                    if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.USED) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.AVAILABLE))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.TRANSIENT) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.AVAILABLE))
                    {
                        if(traceOrgSubnetIpDetailsExisted.getLastAliveTime()!=null)
                        {
                            if((new Date().getTime() - new Date(traceOrgSubnetIpDetailsExisted.getLastAliveTime()).getTime())/(1000*60*60*24)>=7)
                            {
                                traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.AVAILABLE);
                            }
                            else
                            {
                                traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);
                            }
                        }
                        else
                        {
                            traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.TRANSIENT);
                        }

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.TRANSIENT) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.RESERVED) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.USED))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.USED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else if(traceOrgSubnetIpDetailsExisted.getStatus().equals(TraceOrgCommonConstants.RESERVED) && traceOrgSubnetIpDetails.getStatus().equals(TraceOrgCommonConstants.AVAILABLE))
                    {
                        traceOrgSubnetIpDetailsExisted.setStatus(TraceOrgCommonConstants.RESERVED);

                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(TraceOrgCommonConstants.AVAILABLE);

                        traceOrgSubnetIpDetails.setModifiedDate(new Date());
                    }
                    else
                    {
                        traceOrgSubnetIpDetailsExisted.setPreviousStatus(traceOrgSubnetIpDetailsExisted.getStatus());

                        traceOrgSubnetIpDetailsExisted.setStatus(traceOrgSubnetIpDetails.getStatus());
                    }

                    boolean updateStatus = traceOrgService.insert(traceOrgSubnetIpDetailsExisted);

                    if (updateStatus)
                    {
                        if(traceOrgSubnetIpDetailsExisted.getConflictMac() != null && traceOrgSubnetIpDetailsExisted.getMacAddress()!=null)
                        {
                            TraceOrgEvent traceOrgEvent =  new TraceOrgEvent();

                            traceOrgEvent.setTimestamp(new Date());

                            traceOrgEvent.setEventType("Conflict IP");

                            traceOrgEvent.setEventContext("IP Address "+traceOrgSubnetIpDetailsExisted.getIpAddress()+" with  Mac Address "+traceOrgSubnetIpDetailsExisted.getMacAddress()+" conflicted with  Mac Address "+traceOrgSubnetIpDetailsExisted.getConflictMac()+" in IP Address Manager " );

                            traceOrgEvent.setSeverity(0);

                            traceOrgService.insert(traceOrgEvent);
                        }
                    }
                }
            }
        }
        catch (Exception exception)
        {
            _logger.error(exception);
        }

        return true;
    }
}

