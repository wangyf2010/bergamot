package com.intrbiz.bergamot.ui.router.admin;

import java.util.stream.Collectors;

import com.intrbiz.balsa.engine.route.Router;
import com.intrbiz.balsa.metadata.WithDataAdapter;
import com.intrbiz.bergamot.config.model.ClusterCfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.Site;
import com.intrbiz.bergamot.ui.BergamotApp;
import com.intrbiz.configuration.Configuration;
import com.intrbiz.metadata.Any;
import com.intrbiz.metadata.Prefix;
import com.intrbiz.metadata.RequirePermission;
import com.intrbiz.metadata.RequireValidPrincipal;
import com.intrbiz.metadata.SessionVar;
import com.intrbiz.metadata.Template;

@Prefix("/admin/cluster")
@Template("layout/main")
@RequireValidPrincipal()
@RequirePermission("ui.admin")
public class ClusterAdminRouter extends Router<BergamotApp>
{    
    @Any("/")
    @WithDataAdapter(BergamotDB.class)
    public void index(BergamotDB db, @SessionVar("site") Site site)
    {
        model("clusters", db.listClusters(site.getId()).stream().filter((c) -> permission("read.config", c.getId())).collect(Collectors.toList()));
        model("cluster_templates", db.listConfigTemplates(site.getId(), Configuration.getRootElement(ClusterCfg.class)).stream().filter((c) -> permission("read.config", c.getId())).collect(Collectors.toList()));
        encode("admin/cluster/index");
    }
}
