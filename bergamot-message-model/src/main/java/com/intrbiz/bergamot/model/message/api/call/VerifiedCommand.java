package com.intrbiz.bergamot.model.message.api.call;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.intrbiz.bergamot.model.message.api.APIResponse;
import com.intrbiz.bergamot.model.message.config.BergamotValidationReportMO;

@JsonTypeName("bergamot.api.command_editor.verified_command")
public class VerifiedCommand extends APIResponse
{    
    @JsonProperty("report")
    private BergamotValidationReportMO report;
    
    @JsonProperty("parameters_view")
    private String parametersView;
    
    public VerifiedCommand()
    {
        super();
    }
    
    public VerifiedCommand(Stat stat, BergamotValidationReportMO report, String parametersView)
    {
        super(stat);
        this.parametersView = parametersView;
    }

    public String getParametersView()
    {
        return parametersView;
    }

    public void setParametersView(String parametersView)
    {
        this.parametersView = parametersView;
    }

    public BergamotValidationReportMO getReport()
    {
        return report;
    }

    public void setReport(BergamotValidationReportMO report)
    {
        this.report = report;
    }
    
    
}
