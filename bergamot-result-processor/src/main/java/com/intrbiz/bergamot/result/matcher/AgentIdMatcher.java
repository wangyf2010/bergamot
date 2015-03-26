package com.intrbiz.bergamot.result.matcher;

import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.Check;
import com.intrbiz.bergamot.model.message.result.MatchOnAgentId;
import com.intrbiz.bergamot.model.message.result.PassiveResultMO;

public class AgentIdMatcher implements Matcher<MatchOnAgentId>
{
    @Override
    public boolean build(Matchers matchers, MatchOnAgentId matchOn)
    {
        return true;
    }

    @Override
    public Check<?, ?> match(BergamotDB db, MatchOnAgentId matchOn, PassiveResultMO passiveResult)
    {
        return db.getHostByAgentId(passiveResult.getSiteId(), matchOn.getAgentId());
    }   
}