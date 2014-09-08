package com.intrbiz.bergamot.command.admin;

import java.util.List;

import com.intrbiz.bergamot.BergamotCLI;
import com.intrbiz.bergamot.BergamotCLICommand;
import com.intrbiz.bergamot.config.UICfg;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.data.DataManager;
import com.intrbiz.util.pool.database.DatabasePool;

public class DBVersionCommand extends BergamotCLICommand
{
    public DBVersionCommand()
    {
        super();
    }

    @Override
    public String name()
    {
        return "db-version";
    }
    
    @Override
    public boolean admin()
    {
        return true;
    }

    @Override
    public String usage()
    {
        return "";
    }

    @Override
    public String help()
    {
        return "Display the version of the Bergamot database schema\n";
    }

    @Override
    public int execute(BergamotCLI cli, List<String> args) throws Exception
    {
        // read the UI config and connect to the database
        UICfg config = UICfg.loadConfiguration();
        // setup the data manager
        DataManager.getInstance().registerDefaultServer(DatabasePool.Default.with().postgresql().url(config.getDatabase().getUrl()).username(config.getDatabase().getUsername()).password(config.getDatabase().getPassword()).build());
        // ensure the DB schema is installed
        BergamotDB.install();
        // now actually create the site
        try (BergamotDB db = BergamotDB.connect())
        {
            System.out.println(db.getName() + " " + db.getVersion());
        }
        // all ok
        return 0;
    }
}