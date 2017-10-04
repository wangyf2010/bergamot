package com.intrbiz.bergamot.ui.api;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.intrbiz.Util;
import com.intrbiz.balsa.engine.route.Router;
import com.intrbiz.balsa.metadata.WithDataAdapter;
import com.intrbiz.bergamot.metadata.IgnoreBinding;
import com.intrbiz.bergamot.metadata.IsaObjectId;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.model.message.reading.CheckReadingMO;
import com.intrbiz.bergamot.ui.BergamotApp;
import com.intrbiz.lamplighter.data.LamplighterDB;
import com.intrbiz.lamplighter.model.CheckReading;
import com.intrbiz.lamplighter.model.StoredDoubleGaugeReading;
import com.intrbiz.lamplighter.model.StoredFloatGaugeReading;
import com.intrbiz.lamplighter.model.StoredIntGaugeReading;
import com.intrbiz.lamplighter.model.StoredLongGaugeReading;
import com.intrbiz.lamplighter.model.StoredMeterReading;
import com.intrbiz.lamplighter.model.StoredReading;
import com.intrbiz.lamplighter.model.StoredTimerReading;
import com.intrbiz.metadata.Any;
import com.intrbiz.metadata.CheckRegEx;
import com.intrbiz.metadata.CoalesceMode;
import com.intrbiz.metadata.IsaInt;
import com.intrbiz.metadata.IsaLong;
import com.intrbiz.metadata.JSON;
import com.intrbiz.metadata.ListOf;
import com.intrbiz.metadata.Param;
import com.intrbiz.metadata.Prefix;
import com.intrbiz.metadata.RequirePermission;
import com.intrbiz.metadata.RequireValidPrincipal;
import com.intrbiz.metadata.Var;
import com.intrbiz.metadata.doc.Desc;
import com.intrbiz.metadata.doc.Title;

@Title("Lamplighter (Readings) API Methods")
@Desc({
    "Lamplighter is Bergamot Monitorings internal readings (metrics) sub-system.  Lamplighter collects readings (performance metrics published by checks) and stores them for later trend analysis.",
    "Lamplighter stores various types of metrics:",
    " * Gauges",
    " * * Int Gauge (32bit integer)",
    " * * Long Gauge (64bit iInteger)",
    " * * Float Gauge (32bit floating point",
    " * * Double Gauge (64bit floating point",
})
@Prefix("/api/lamplighter")
@RequireValidPrincipal()
public class LamplighterAPIRouter extends Router<BergamotApp>
{        
    @Title("Get readings for check")
    @Desc({
        "Get the list of available readings for the check identified by the given UUID.",
        "This will return metadata about all readings which are stored for a check, including reading ID, reading type."
    })
    @Any("/check/id/:id/readings")
    @JSON(notFoundIfNull = true)
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @ListOf(CheckReadingMO.class)
    public List<CheckReadingMO> getReadingsByCheck(LamplighterDB db, @Var("site") Site site, @IsaObjectId() UUID id)
    {
        List<CheckReadingMO> readings = new LinkedList<CheckReadingMO>();
        for (CheckReading reading : db.getCheckReadingsForCheck(id))
        {
            readings.add(new CheckReadingMO(
                    reading.getId(),
                    reading.getSiteId(),
                    reading.getCheckId(),
                    reading.getName(),
                    reading.getSummary(),
                    reading.getDescription(),
                    reading.getUnit(),
                    reading.getReadingType(),
                    reading.getCreated() == null ? 0 : reading.getCreated().getTime(),
                    reading.getUpdated() == null ? 0 : reading.getUpdated().getTime(),
                    reading.getPollInterval()
            ));
        }
        return readings;
    }
    
    // double gauge
    
    @Title("Latest double gauge readings")
    @Desc({
        "Get the latest readings for a double gauge."
    })
    @Any("/graph/reading/gauge/double/:id/latest/:limit")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getLatestDoubleReadings(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaInt(min = 1, max = 1000, defaultValue = 100, coalesce = CoalesceMode.ALWAYS) Integer limit,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredDoubleGaugeReading> readings = db.getLatestDoubleGaugeReadings(site.getId(), id, limit);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredDoubleGaugeReading::getValue, StoredDoubleGaugeReading::getWarning, StoredDoubleGaugeReading::getCritical, StoredDoubleGaugeReading::getMin, StoredDoubleGaugeReading::getMax);
    }
    
    @Title("Get double gauge readings")
    @Desc({
        "Get double gauge readings for the given period (from start to end) applying the given aggregation method over the given rollup period.",
        "For example we can get the 5 minute average using the `avg` aggregation method with rollup period of `300000`."
    })
    @Any("/graph/reading/gauge/double/:id/date/:rollup/:agg/:start/:end")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getDoubleReadingsByDate(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaLong(mandatory = true, defaultValue = 300_000L, coalesce = CoalesceMode.ALWAYS) Long rollup, 
            @CheckRegEx(value="(avg|sum|min|max)", mandatory = true, defaultValue = "avg", coalesce = CoalesceMode.ALWAYS) String agg,
            @IsaLong() Long start,
            @IsaLong() Long end,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredDoubleGaugeReading> readings = db.getDoubleGaugeReadingsByDate(checkReading.getSiteId(), checkReading.getId(), new Timestamp(start), new Timestamp(end), rollup, agg);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredDoubleGaugeReading::getValue, StoredDoubleGaugeReading::getWarning, StoredDoubleGaugeReading::getCritical, StoredDoubleGaugeReading::getMin, StoredDoubleGaugeReading::getMax);
    }
    
    // float gauge
    
    @Title("Latest float gauge readings")
    @Desc({
        "Get the latest readings for a float gauge."
    })
    @Any("/graph/reading/gauge/float/:id/latest/:limit")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getLatestFloatReadings(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaInt(min = 1, max = 1000, defaultValue = 100, coalesce = CoalesceMode.ALWAYS) Integer limit,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredFloatGaugeReading> readings = db.getLatestFloatGaugeReadings(site.getId(), id, limit);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredFloatGaugeReading::getValue, StoredFloatGaugeReading::getWarning, StoredFloatGaugeReading::getCritical, StoredFloatGaugeReading::getMin, StoredFloatGaugeReading::getMax);
    }
    
    @Title("Get float gauge readings")
    @Desc({
        "Get float gauge readings for the given period (from start to end) applying the given aggregation method over the given rollup period.",
        "For example we can get the 5 minute average using the `avg` aggregation method with rollup period of `300000`."
    })
    @Any("/graph/reading/gauge/float/:id/date/:rollup/:agg/:start/:end")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getFloatReadingsByDate(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaLong(mandatory = true, defaultValue = 300_000L, coalesce = CoalesceMode.ALWAYS) Long rollup, 
            @CheckRegEx(value="(avg|sum|min|max)", mandatory = true, defaultValue = "avg", coalesce = CoalesceMode.ALWAYS) String agg,
            @IsaLong() Long start,
            @IsaLong() Long end,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredFloatGaugeReading> readings = db.getFloatGaugeReadingsByDate(checkReading.getSiteId(), checkReading.getId(), new Timestamp(start), new Timestamp(end), rollup, agg);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredFloatGaugeReading::getValue, StoredFloatGaugeReading::getWarning, StoredFloatGaugeReading::getCritical, StoredFloatGaugeReading::getMin, StoredFloatGaugeReading::getMax);
    }
    
    // long gauge
    
    @Title("Latest long gauge readings")
    @Desc({
        "Get the latest readings for a long gauge."
    })
    @Any("/graph/reading/gauge/long/:id/latest/:limit")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getLatestLongReadings(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaInt(min = 1, max = 1000, defaultValue = 100, coalesce = CoalesceMode.ALWAYS) Integer limit,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredLongGaugeReading> readings = db.getLatestLongGaugeReadings(site.getId(), id, limit);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredLongGaugeReading::getValue, StoredLongGaugeReading::getWarning, StoredLongGaugeReading::getCritical, StoredLongGaugeReading::getMin, StoredLongGaugeReading::getMax);
    }
    
    @Title("Get long gauge readings")
    @Desc({
        "Get long gauge readings for the given period (from start to end) applying the given aggregation method over the given rollup period.",
        "For example we can get the 5 minute average using the `avg` aggregation method with rollup period of `300000`."
    })
    @Any("/graph/reading/gauge/long/:id/date/:rollup/:agg/:start/:end")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getLongReadingsByDate(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaLong(mandatory = true, defaultValue = 300_000L, coalesce = CoalesceMode.ALWAYS) Long rollup, 
            @CheckRegEx(value="(avg|sum|min|max)", mandatory = true, defaultValue = "avg", coalesce = CoalesceMode.ALWAYS) String agg,
            @IsaLong() Long start,
            @IsaLong() Long end,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredLongGaugeReading> readings = db.getLongGaugeReadingsByDate(checkReading.getSiteId(), checkReading.getId(), new Timestamp(start), new Timestamp(end), rollup, agg);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredLongGaugeReading::getValue, StoredLongGaugeReading::getWarning, StoredLongGaugeReading::getCritical, StoredLongGaugeReading::getMin, StoredLongGaugeReading::getMax);
    }
    
    // int gauge
    
    @Title("Latest int gauge readings")
    @Desc({
        "Get the latest readings for a int gauge."
    })
    @Any("/graph/reading/gauge/int/:id/latest/:limit")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getLatestIntReadings(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaInt(min = 1, max = 1000, defaultValue = 100, coalesce = CoalesceMode.ALWAYS) Integer limit,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredIntGaugeReading> readings = db.getLatestIntGaugeReadings(site.getId(), id, limit);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredIntGaugeReading::getValue, StoredIntGaugeReading::getWarning, StoredIntGaugeReading::getCritical, StoredIntGaugeReading::getMin, StoredIntGaugeReading::getMax);
    }
    
    @Title("Get int gauge readings")
    @Desc({
        "Get int gauge readings for the given period (from start to end) applying the given aggregation method over the given rollup period.",
        "For example we can get the 5 minute average using the `avg` aggregation method with rollup period of `300000`."
    })
    @Any("/graph/reading/gauge/int/:id/date/:rollup/:agg/:start/:end")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getIntReadingsByDate(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaLong(mandatory = true, defaultValue = 300_000L, coalesce = CoalesceMode.ALWAYS) Long rollup, 
            @CheckRegEx(value="(avg|sum|min|max)", mandatory = true, defaultValue = "avg", coalesce = CoalesceMode.ALWAYS) String agg,
            @IsaLong() Long start,
            @IsaLong() Long end,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredIntGaugeReading> readings = db.getIntGaugeReadingsByDate(checkReading.getSiteId(), checkReading.getId(), new Timestamp(start), new Timestamp(end), rollup, agg);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeLineChartData(jenny, checkReading, readings, series, StoredIntGaugeReading::getValue, StoredIntGaugeReading::getWarning, StoredIntGaugeReading::getCritical, StoredIntGaugeReading::getMin, StoredIntGaugeReading::getMax);
    }
    
    // meter
    
    @Title("Latest meter readings")
    @Desc({
        "Get the latest readings for a meter."
    })
    @Any("/graph/reading/meter/:id/latest/:limit")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getMeterReadings(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaInt(min = 1, max = 1000, defaultValue = 100, coalesce = CoalesceMode.ALWAYS) Integer limit,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredMeterReading> readings = db.getLatestMeterReadings(site.getId(), id, limit);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeMeterLineChartData(jenny, checkReading, readings, series, (r) -> r.getCount());
    }
    
    @Title("Get meter readings")
    @Desc({
        "Get meter readings for the given period (from start to end) applying the given aggregation method over the given rollup period.",
        "For example we can get the 5 minute average using the `avg` aggregation method with rollup period of `300000`."
    })
    @Any("/graph/reading/meter/:id/date/:rollup/:agg/:start/:end")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getMeterReadingsByDate(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaLong(mandatory = true, defaultValue = 300_000L, coalesce = CoalesceMode.ALWAYS) Long rollup, 
            @CheckRegEx(value="(avg|sum|min|max)", mandatory = true, defaultValue = "avg", coalesce = CoalesceMode.ALWAYS) String agg,
            @IsaLong() Long start,
            @IsaLong() Long end,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredMeterReading> readings = db.getMeterReadingsByDate(checkReading.getSiteId(), checkReading.getId(), new Timestamp(start), new Timestamp(end), rollup, agg);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeMeterLineChartData(jenny, checkReading, readings, series, (r) -> r.getCount());
    }
    
    // timer
    
    @Title("Latest timer readings")
    @Desc({
        "Get the latest readings for a timer."
    })
    @Any("/graph/reading/timer/:id/latest/:limit")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getTimerReadings(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaInt(min = 1, max = 1000, defaultValue = 100, coalesce = CoalesceMode.ALWAYS) Integer limit,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredTimerReading> readings = db.getLatestTimerReadings(site.getId(), id, limit);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeMeterLineChartData(jenny, checkReading, readings, series, (r) -> r.getCount());
    }
    
    @Title("Get timer readings")
    @Desc({
        "Get timer readings for the given period (from start to end) applying the given aggregation method over the given rollup period.",
        "For example we can get the 5 minute average using the `avg` aggregation method with rollup period of `300000`."
    })
    @Any("/graph/reading/timer/:id/date/:rollup/:agg/:start/:end")
    @RequirePermission("api.read.lamplighter.readings")
    @WithDataAdapter(LamplighterDB.class)
    @IgnoreBinding(ignoreDocs = false)
    public void getTimerReadingsByDate(
            LamplighterDB db, 
            @Var("site") Site site, 
            @IsaObjectId() UUID id, 
            @IsaLong(mandatory = true, defaultValue = 300_000L, coalesce = CoalesceMode.ALWAYS) Long rollup, 
            @CheckRegEx(value="(avg|sum|min|max)", mandatory = true, defaultValue = "avg", coalesce = CoalesceMode.ALWAYS) String agg,
            @IsaLong() Long start,
            @IsaLong() Long end,
            @Param("series") String series
    ) throws IOException
    {
        // get the data
        CheckReading checkReading = db.getCheckReading(id);
        List<StoredTimerReading> readings = db.getTimerReadingsByDate(checkReading.getSiteId(), checkReading.getId(), new Timestamp(start), new Timestamp(end), rollup, agg);
        // write
        JsonGenerator jenny = response().ok().json().getJsonWriter();
        this.writeMeterLineChartData(jenny, checkReading, readings, series, (r) -> r.getCount());
    }
    
    // generic
    
    /**
     * Generically output a line chart JSON data structure
     * @param jenny the json output stream
     * @param checkReading the check reading
     * @param readings the data points
     * @param series which series to output
     * @param getValue the value accessor
     * @param getWarning the warning accessor
     * @param getCritical the critical accessor
     * @param getMin the min accessor
     * @param getMax the max accessor
     * @throws IOException
     */
    private <T extends StoredReading> void writeLineChartData(JsonGenerator jenny, CheckReading checkReading, List<T> readings, String series, Function<T,Object> getValue, Function<T,Object> getWarning, Function<T,Object> getCritical, Function<T,Object> getMin, Function<T,Object> getMax) throws IOException
    {
        jenny.writeStartObject();
        // title
        jenny.writeFieldName("title");
        jenny.writeString(checkReading.getSummary() + (Util.isEmpty(checkReading.getUnit()) ? "" : " (" + checkReading.getUnit() + ")"));
        // x-title
        jenny.writeFieldName("x-title");
        jenny.writeString("");
        // y-title
        jenny.writeFieldName("y-title");
        jenny.writeString(Util.isEmpty(checkReading.getUnit()) ? "" : checkReading.getUnit());
        // x
        jenny.writeFieldName("x");
        jenny.writeStartArray();
        for (T reading : readings)
        {
            jenny.writeNumber(reading.getCollectedAt().getTime());
        }
        jenny.writeEndArray();
        // y sets
        jenny.writeFieldName("y");
        jenny.writeStartArray();
        // value
        jenny.writeStartObject();
        jenny.writeFieldName("title");
        jenny.writeString(checkReading.getSummary() + (Util.isEmpty(checkReading.getUnit()) ? "" : " (" + checkReading.getUnit() + ")"));
        jenny.writeFieldName("colour");
        jenny.writeString("#00BF00");
        jenny.writeFieldName("y");
        jenny.writeStartArray();
        for (T reading : readings)
        {
            jenny.writeObject(getValue.apply(reading));
        }
        jenny.writeEndArray();
        jenny.writeEndObject();
        // optional series
        if (! (Util.isEmpty(series) || "none".equals(series)))
        {
            // warning
            if (series.contains("warning"))
            {
                jenny.writeStartObject();
                jenny.writeFieldName("title");
                jenny.writeString("Warning");
                jenny.writeFieldName("colour");
                jenny.writeString("#FFBF00");
                jenny.writeFieldName("y");
                jenny.writeStartArray();
                for (T reading : readings)
                {
                    jenny.writeObject(getWarning.apply(reading));
                }
                jenny.writeEndArray();
                jenny.writeEndObject();
            }
            // critical
            if (series.contains("critical"))
            {
                jenny.writeStartObject();
                jenny.writeFieldName("title");
                jenny.writeString("Critical");
                jenny.writeFieldName("colour");
                jenny.writeString("#E20800");
                jenny.writeFieldName("y");
                jenny.writeStartArray();
                for (T reading : readings)
                {
                    jenny.writeObject(getCritical.apply(reading));
                }
                jenny.writeEndArray();
                jenny.writeEndObject();
            }
            // min
            if (series.contains("min"))
            {
                jenny.writeStartObject();
                jenny.writeFieldName("title");
                jenny.writeString("Min");
                jenny.writeFieldName("colour");
                jenny.writeString("#A4C0E4");
                jenny.writeFieldName("y");
                jenny.writeStartArray();
                for (T reading : readings)
                {
                    jenny.writeObject(getMin.apply(reading));
                }
                jenny.writeEndArray();
                jenny.writeEndObject();
            }
            // max
            if (series.contains("max"))
            {
                jenny.writeStartObject();
                jenny.writeFieldName("title");
                jenny.writeString("Max");
                jenny.writeFieldName("colour");
                jenny.writeString("#A4C0E4");
                jenny.writeFieldName("y");
                jenny.writeStartArray();
                for (T reading : readings)
                {
                    jenny.writeObject(getMax.apply(reading));
                }
                jenny.writeEndArray();
                jenny.writeEndObject();
            }
        }
        // end y sets
        jenny.writeEndArray();
        jenny.writeEndObject();
    }
    
    private <T extends StoredReading> void writeMeterLineChartData(JsonGenerator jenny, CheckReading checkReading, List<T> readings, String series, Function<T,Long> getCount) throws IOException
    {
        jenny.writeStartObject();
        // title
        jenny.writeFieldName("title");
        jenny.writeString(checkReading.getSummary() + (Util.isEmpty(checkReading.getUnit()) ? "" : " (" + checkReading.getUnit() + ")"));
        // x-title
        jenny.writeFieldName("x-title");
        jenny.writeString("");
        // y-title
        jenny.writeFieldName("y-title");
        jenny.writeString(Util.isEmpty(checkReading.getUnit()) ? "" : checkReading.getUnit());
        // x
        jenny.writeFieldName("x");
        jenny.writeStartArray();
        for (T reading : readings)
        {
            jenny.writeNumber(reading.getCollectedAt().getTime());
        }
        jenny.writeEndArray();
        // y sets
        jenny.writeFieldName("y");
        jenny.writeStartArray();
        // value
        jenny.writeStartObject();
        jenny.writeFieldName("title");
        jenny.writeString(checkReading.getSummary() + (Util.isEmpty(checkReading.getUnit()) ? "" : " (" + checkReading.getUnit() + ")"));
        jenny.writeFieldName("colour");
        jenny.writeString("#00BF00");
        jenny.writeFieldName("y");
        jenny.writeStartArray();
        for (T reading : readings)
        {
            jenny.writeObject(getCount.apply(reading));
        }
        jenny.writeEndArray();
        jenny.writeEndObject();
        // end y sets
        jenny.writeEndArray();
        jenny.writeEndObject();
    }
}
