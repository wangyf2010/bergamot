package com.intrbiz.bergamot.accounting.io;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.intrbiz.bergamot.accounting.model.ExecuteCheckAccountingEvent;
import com.intrbiz.bergamot.accounting.model.ProcessResultAccountingEvent;
import com.intrbiz.bergamot.accounting.model.ProcessResultAccountingEvent.ResultType;
import com.intrbiz.bergamot.accounting.model.SendNotificationAccountingEvent;
import com.intrbiz.bergamot.accounting.model.SendNotificationAccountingEvent.NotificationType;

public class TestBergamotAccountingTranscoder
{
    private static UUID siteId = UUID.fromString("01cf7f8e-2da3-4b5b-8764-a8cb0e1e8e6b");
    private static UUID execId = UUID.fromString("7c5efc47-8cd4-475c-817c-fba6acf291c6");
    private static UUID checkId = UUID.fromString("3640d25d-547d-40ab-8eb6-fa97155e9dbb");
    private static UUID alertId = UUID.fromString("e6fa47ea-f435-4607-b0d5-d128fe259742");
    
    private BergamotAccountingTranscoder codec;
    
    @Before()
    public void setup()
    {
        this.codec  = BergamotAccountingTranscoder.getDefault();
    }
    
    @Test
    public void testEncodeExecuteCheckAccountingEventToString()
    {
        ExecuteCheckAccountingEvent original = new ExecuteCheckAccountingEvent(siteId, execId, checkId, "nrpe", "nrpe", "check_load");
        String encoded = this.codec.encodeToString(original);
        assertThat(encoded, is(notNullValue()));
        ExecuteCheckAccountingEvent decoded = this.codec.decodeFromString(encoded);
        assertThat(decoded, is(notNullValue()));
        assertThat(decoded, is(equalTo(original)));
    }
    
    @Test
    public void testEncodeProcessResultAccountingEventToString()
    {
        ProcessResultAccountingEvent original = new ProcessResultAccountingEvent(siteId, execId, checkId, ResultType.ACTIVE);
        String encoded = this.codec.encodeToString(original);
        assertThat(encoded, is(notNullValue()));
        ProcessResultAccountingEvent decoded = this.codec.decodeFromString(encoded);
        assertThat(decoded, is(notNullValue()));
        assertThat(decoded, is(equalTo(original)));
    }
    
    @Test
    public void testEncodeSendAlertAccountingEventToString()
    {
        SendNotificationAccountingEvent original = new SendNotificationAccountingEvent(siteId, alertId, checkId, NotificationType.ALERT, 1);
        String encoded = this.codec.encodeToString(original);
        assertThat(encoded, is(notNullValue()));
        SendNotificationAccountingEvent decoded = this.codec.decodeFromString(encoded);
        assertThat(decoded, is(notNullValue()));
        assertThat(decoded, is(equalTo(original)));
    }
}
