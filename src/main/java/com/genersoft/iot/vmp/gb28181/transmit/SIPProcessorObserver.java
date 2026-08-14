package com.genersoft.iot.vmp.gb28181.transmit;

import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.ISIPResponseProcessor;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: SIPSignaling processing observer
 * @author: panlinlin
 * @date:   2021November 5, afternoon15：32
 */
@Slf4j
@Component
public class SIPProcessorObserver implements ISIPProcessorObserver {

    private static final Map<String,  ISIPRequestProcessor> requestProcessorMap = new ConcurrentHashMap<>();
    private static final Map<String, ISIPResponseProcessor> responseProcessorMap = new ConcurrentHashMap<>();

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Add request subscription
     * @param method method name
     * @param processor handler
     */
    public void addRequestProcessor(String method, ISIPRequestProcessor processor) {
        requestProcessorMap.put(method, processor);
    }

    /**
     * Add response subscription
     * @param method method name
     * @param processor handler
     */
    public void addResponseProcessor(String method, ISIPResponseProcessor processor) {
        responseProcessorMap.put(method, processor);
    }

    /**
     * Distribute the RequestEvent event
     * @param requestEvent RequestEventevent
     */
    @Override
    @Async
    public void processRequest(RequestEvent requestEvent) {
        String method = requestEvent.getRequest().getMethod();
        ISIPRequestProcessor sipRequestProcessor = requestProcessorMap.get(method);
        if (sipRequestProcessor == null) {
            log.warn("Method not supported{}ofrequest", method);
            // TODO Reply to error
            return;
        }
        requestProcessorMap.get(method).process(requestEvent);

    }

    /**
     * Distribute ResponseEvent events
     * @param responseEvent responseEventevent
     */
    @Override
    @Async
    public void processResponse(ResponseEvent responseEvent) {
        SIPResponse response = (SIPResponse)responseEvent.getResponse();
        int status = response.getStatusCode();

        // Success
        if (((status >= Response.OK) && (status < Response.MULTIPLE_CHOICES)) || status == Response.UNAUTHORIZED) {
            ISIPResponseProcessor sipRequestProcessor = responseProcessorMap.get(response.getCSeqHeader().getMethod());
            if (sipRequestProcessor != null) {
                sipRequestProcessor.process(responseEvent);
            }

            CallIdHeader callIdHeader = response.getCallIdHeader();
            CSeqHeader cSeqHeader = response.getCSeqHeader();
            if (callIdHeader != null) {
                SipEvent sipEvent = sipSubscribe.getSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                if (sipEvent != null) {
                    if (sipEvent.getOkEvent() != null) {
                        SipSubscribe.EventResult<ResponseEvent> eventResult = new SipSubscribe.EventResult<>(responseEvent);
                        sipEvent.getOkEvent().response(eventResult);
                    }
                    sipSubscribe.removeSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                }
            }
        } else if ((status >= Response.TRYING) && (status < Response.OK)) {
            // Add other responses that do not require a reply, such as 101, 180, etc.
            // Time to update sip subscription
//            sipSubscribe.updateTimeout(response.getCallIdHeader().getCallId());
        } else {
            log.warn("Failed response received！status：" + status + ",message:" + response.getReasonPhrase());
            if (responseEvent.getResponse() != null && !sipSubscribe.isEmpty() ) {
                CallIdHeader callIdHeader = response.getCallIdHeader();
                CSeqHeader cSeqHeader = response.getCSeqHeader();
                if (callIdHeader != null) {
                    SipEvent sipEvent = sipSubscribe.getSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                    if (sipEvent != null ) {
                        if (sipEvent.getErrorEvent() != null) {
                            SipSubscribe.EventResult<ResponseEvent> eventResult = new SipSubscribe.EventResult<>(responseEvent);
                            sipEvent.getErrorEvent().response(eventResult);
                        }
                        sipSubscribe.removeSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                    }
                }
            }
            if (responseEvent.getDialog() != null) {
                responseEvent.getDialog().delete();
            }
        }


    }

    /**
     * Send a message to a timeout subscription
     * @param timeoutEvent timeoutEventevent
     */
    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.info("[Message sending timeout]");
//        ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
//
//        if (clientTransaction != null) {
//            log.info("[Sending wrong subscription] clientTransaction != null");
//            Request request = clientTransaction.getRequest();
//            if (request != null) {
//                log.info("[Sending wrong subscription] request != null");
//                CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
//                if (callIdHeader != null) {
//                    log.info("[Sending wrong subscription]");
//                    SipSubscribe.Event subscribe = sipSubscribe.getErrorSubscribe(callIdHeader.getCallId());
//                    SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(timeoutEvent);
//                    if (subscribe != null){
//                        subscribe.response(eventResult);
//                    }
//                    sipSubscribe.removeOkSubscribe(callIdHeader.getCallId());
//                    sipSubscribe.removeErrorSubscribe(callIdHeader.getCallId());
//                }
//            }
//        }
//        eventPublisher.requestTimeOut(timeoutEvent);
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.out.println("processIOException");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
//        if (transactionTerminatedEvent.isServerTransaction()) {
//            ServerTransaction serverTransaction = transactionTerminatedEvent.getServerTransaction();
//            serverTransaction.get
//        }


//        Transaction transaction = null;
//        System.out.println("processTransactionTerminated");
//        if (transactionTerminatedEvent.isServerTransaction()) {
//            transaction = transactionTerminatedEvent.getServerTransaction();
//        }else {
//            transaction = transactionTerminatedEvent.getClientTransaction();
//        }
//
//        System.out.println(transaction.getBranchId());
//        System.out.println(transaction.getState());
//        System.out.println(transaction.getRequest().getMethod());
//        CallIdHeader header = (CallIdHeader)transaction.getRequest().getHeader(CallIdHeader.NAME);
//        SipSubscribe.EventResult<TransactionTerminatedEvent> terminatedEventEventResult = new SipSubscribe.EventResult<>(transactionTerminatedEvent);

//        sipSubscribe.getErrorSubscribe(header.getCallId()).response(terminatedEventEventResult);
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        CallIdHeader callId = dialogTerminatedEvent.getDialog().getCallId();
    }




}
